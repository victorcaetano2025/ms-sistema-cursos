package com.ms.progress.listener;

import com.ms.progress.dto.LessonCompletedEvent;
import com.ms.progress.service.ProgressService;
import com.ms.progress.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LessonCompletedListener {

    private final ProgressService progressService;

    public LessonCompletedListener(ProgressService progressService) {
        this.progressService = progressService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_LESSON)
    public void onLessonCompleted(LessonCompletedEvent event) {
        System.out.println("Recebido evento de aula concluída! Aluno: " + event.studentId());
        
        // Chama aquele método gigante que fizemos antes, que recalcula a %
        // e dispara o evento de curso concluído se bater 100%
        progressService.updateLessonProgress(
            event.studentId(),
            event.courseId(),
            event.lessonId(),
            event.totalLessons());
    }
}