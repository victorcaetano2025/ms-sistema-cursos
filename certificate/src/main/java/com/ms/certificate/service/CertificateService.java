package com.ms.certificate.service;

import com.ms.certificate.dto.CertificateIssuedEvent;
import com.ms.certificate.model.Certificate;
import com.ms.certificate.repository.CertificateRepository;
import com.ms.certificate.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.ms.certificate.config.RabbitMQConfig.ROUTING_CERTIFICATE_ISSUED;

@Service
public class CertificateService {

    private final CertificateRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public CertificateService(CertificateRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void generateCertificate(Long studentId, Long courseId) {
        validateIds(studentId, courseId);

        // RabbitMQ pode entregar uma mensagem mais de uma vez; a operação precisa ser idempotente.
        if (repository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
            return; 
        }

        // Em produção, esta etapa deve gerar o PDF e armazená-lo em um storage externo.
        String certificateUrl = "https://meu-storage.s3.aws.com/certificados/"
                + courseId + "/" + studentId + ".pdf";

        Certificate certificate = new Certificate();
        certificate.setStudentId(studentId);
        certificate.setCourseId(courseId);
        certificate.setPdfUrl(certificateUrl);
        certificate.setIssuedAt(LocalDateTime.now());

        Certificate savedCertificate = repository.save(certificate);

        CertificateIssuedEvent event = new CertificateIssuedEvent(
            savedCertificate.getStudentId(),
            savedCertificate.getCourseId(),
            savedCertificate.getPdfUrl(),
            savedCertificate.getValidationCode()
        );

        // O Enrollment Service usa este evento para atualizar a matrícula.
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME, 
            ROUTING_CERTIFICATE_ISSUED,
            event
        );
    }

    private void validateIds(Long studentId, Long courseId) {
        if (studentId == null || studentId <= 0 || courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("studentId e courseId devem ser positivos");
        }
    }
}