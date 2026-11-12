package com.ms.course.service;

import com.ms.course.dto.LessonCompletedEvent;
import com.ms.course.model.Lesson;
import com.ms.course.repository.LessonRepository;
import com.ms.course.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.ms.course.config.RabbitMQConfig.ROUTING_LESSON_COMPLETED;

@Service
public class CourseContentService {

    private final LessonRepository lessonRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public CourseContentService(
            LessonRepository lessonRepository,
            RabbitTemplate rabbitTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.lessonRepository = lessonRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public void completeLesson(Long studentId, Long courseId, Long lessonId) {
        validateIds(studentId, courseId, lessonId);

        // O catálogo é a fonte da verdade sobre a existência e o pertencimento da lição.
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new IllegalArgumentException("Lição não encontrada no catálogo"));

        if (!lesson.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("A lição não pertence ao curso informado!");
        }

        long totalLessons = lessonRepository.countByCourseId(courseId);

        LessonCompletedEvent event = new LessonCompletedEvent(
            studentId,
            courseId,
            lessonId,
            totalLessons
        );

        // A validação do catálogo termina antes de o evento ser encaminhado ao RabbitMQ.
        eventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishLessonCompleted(LessonCompletedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            ROUTING_LESSON_COMPLETED,
            event
        );
    }

    private void validateIds(Long studentId, Long courseId, Long lessonId) {
        if (studentId == null || studentId <= 0
                || courseId == null || courseId <= 0
                || lessonId == null || lessonId <= 0) {
            throw new IllegalArgumentException("studentId, courseId e lessonId devem ser positivos");
        }
    }
}