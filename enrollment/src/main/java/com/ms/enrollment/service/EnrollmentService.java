package com.ms.enrollment.service;

import com.ms.enrollment.dto.EnrollmentCreatedEvent;
import com.ms.enrollment.model.Enrollment;
import com.ms.enrollment.model.EnrollmentStatus;
import com.ms.enrollment.repository.EnrollmentRepository;
import com.ms.enrollment.config.RabbitMQConfig; // A classe de config que você criou
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

import static com.ms.enrollment.config.RabbitMQConfig.ROUTING_ENROLLMENT_CREATED;

@Service
public class EnrollmentService {

    private final EnrollmentRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // Injeção de dependência via construtor (Boa prática do Spring)
    public EnrollmentService(
            EnrollmentRepository repository,
            RabbitTemplate rabbitTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Enrollment createEnrollment(Long studentId, Long courseId) {
        validateIds(studentId, courseId);

        // Evita duas matrículas ativas para o mesmo aluno no mesmo curso.
        if (repository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
            throw new IllegalStateException("Aluno já está matriculado neste curso");
        }

        // A matrícula é persistida antes do evento para que o consumidor receba um ID válido.
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        
        Enrollment savedEnrollment = repository.save(enrollment);

        EnrollmentCreatedEvent event = new EnrollmentCreatedEvent(
            savedEnrollment.getId(),
            studentId,
            courseId
        );

        // O evento só será encaminhado ao RabbitMQ depois do commit da matrícula.
        eventPublisher.publishEvent(event);

        return savedEnrollment;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEnrollmentCreated(EnrollmentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            ROUTING_ENROLLMENT_CREATED,
            event
        );
    }

    public Optional<Enrollment> findById(Long enrollmentId) {
        return repository.findById(enrollmentId);
    }

    private void validateIds(Long studentId, Long courseId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("studentId deve ser positivo");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("courseId deve ser positivo");
        }
    }
}