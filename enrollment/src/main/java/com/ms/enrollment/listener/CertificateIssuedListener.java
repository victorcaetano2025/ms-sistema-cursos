package com.ms.enrollment.listener;

import com.ms.enrollment.dto.CertificateIssuedEvent;
import com.ms.enrollment.repository.EnrollmentRepository;
import com.ms.enrollment.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CertificateIssuedListener {

    private final EnrollmentRepository enrollmentRepository;

    public CertificateIssuedListener(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CERTIFICATE)
    public void onCertificateIssued(CertificateIssuedEvent event) {
        System.out.println("Certificado pronto! Salvando URL para o aluno: " + event.studentId());
        
        // Busca a matrícula do aluno neste curso
        enrollmentRepository.findByStudentIdAndCourseId(event.studentId(), event.courseId())
            .ifPresent(enrollment -> {
                // Atualiza o banco com o link do certificado!
                enrollment.setCertificateUrl(event.pdfUrl());
                enrollment.setStatus(com.ms.enrollment.model.EnrollmentStatus.COMPLETED);
                enrollmentRepository.save(enrollment);
            });
    }
}