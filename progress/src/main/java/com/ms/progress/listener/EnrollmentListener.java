package com.ms.progress.listener;

import com.ms.progress.dto.EnrollmentCreatedEvent;
import com.ms.progress.service.ProgressInitializationService; // Serviço que cria o progresso inicial
import com.ms.progress.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentListener {

    private final ProgressInitializationService initializationService;

    public EnrollmentListener(ProgressInitializationService initializationService) {
        this.initializationService = initializationService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ENROLLMENT)
    public void onEnrollmentCreated(EnrollmentCreatedEvent event) {
        System.out.println("Recebido evento de matrícula do aluno: " + event.studentId());
        
        // Chama a lógica para criar o StudentProgress no banco com 0%
        initializationService.initializeProgress(
            event.enrollmentId(),
            event.studentId(),
            event.courseId());
    }
}