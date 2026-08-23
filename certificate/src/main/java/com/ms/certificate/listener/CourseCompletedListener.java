package com.ms.certificate.listener;

import com.ms.certificate.dto.CourseCompletedEvent;
import com.ms.certificate.service.CertificateService;
import com.ms.certificate.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CourseCompletedListener {

    private final CertificateService certificateService;

    public CourseCompletedListener(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_COURSE_COMPLETED)
    public void onCourseCompleted(CourseCompletedEvent event) {
        System.out.println("Opa! Aluno " + event.studentId() + " finalizou o curso " + event.courseId());
        
        // Chama o serviço que gera o PDF e avisa o RabbitMQ que o certificado está pronto
        certificateService.generateCertificate(event.studentId(), event.courseId());
    }
}