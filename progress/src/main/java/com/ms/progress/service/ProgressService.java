package com.ms.progress.service;

import com.ms.progress.dto.CourseCompletedEvent;
import com.ms.progress.model.StudentProgress;
import com.ms.progress.model.StudentLessonStatus;
import com.ms.progress.repository.StudentProgressRepository;
import com.ms.progress.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

import static com.ms.progress.config.RabbitMQConfig.ROUTING_COURSE_COMPLETED;

@Service
public class ProgressService {

    private final StudentProgressRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public ProgressService(
            StudentProgressRepository repository,
            RabbitTemplate rabbitTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void updateLessonProgress(Long studentId, Long courseId, Long lessonId, Long totalLessons) {
        validateIds(studentId, courseId, lessonId, totalLessons);

        // O progresso é local ao serviço; nenhuma tabela do Enrollment ou Course é compartilhada.
        StudentProgress progress = repository.findByStudentIdAndCourseId(studentId, courseId)
            .orElseThrow(() -> new IllegalStateException("Progresso não encontrado para a matrícula"));

        StudentLessonStatus lessonStatus = progress.getLessonStatuses().stream()
                .filter(lesson -> lesson.getLessonId().equals(lessonId))
                .findFirst()
                .orElseGet(() -> {
                    StudentLessonStatus newStatus = new StudentLessonStatus();
                    newStatus.setLessonId(lessonId);
                    newStatus.setStudentProgress(progress);
                    progress.getLessonStatuses().add(newStatus);
                    return newStatus;
                });

        // A mesma mensagem pode ser entregue novamente; marcar uma lição concluída é idempotente.
        if (!Boolean.TRUE.equals(lessonStatus.getIsLessonCompleted())) {
            lessonStatus.setIsLessonCompleted(true);
            lessonStatus.setCompletedAt(LocalDateTime.now());
        }

        long completedLessons = progress.getLessonStatuses().stream()
            .filter(l -> Boolean.TRUE.equals(l.getIsLessonCompleted()))
                .count();

        long lessonCount = totalLessons;

        double percentage = lessonCount == 0
            ? 0.0
            : ((double) completedLessons / lessonCount) * 100;
        progress.setCompletionPercentage(percentage);

        if (completedLessons >= lessonCount && lessonCount > 0 && !progress.getIsCourseCompleted()) {
            progress.setIsCourseCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            
            // O evento interno só será encaminhado ao RabbitMQ após o commit da transação.
            eventPublisher.publishEvent(new CourseCompletedEvent(studentId, courseId));
        }

        // Salva tudo atualizado no banco
        repository.save(progress);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishCourseCompleted(CourseCompletedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            ROUTING_COURSE_COMPLETED,
            event
        );
    }

    private void validateIds(Long studentId, Long courseId, Long lessonId, Long totalLessons) {
        if (studentId == null || studentId <= 0
                || courseId == null || courseId <= 0
                || lessonId == null || lessonId <= 0
                || totalLessons == null || totalLessons <= 0) {
            throw new IllegalArgumentException("Os dados da lição devem ser válidos e positivos");
        }
    }
}