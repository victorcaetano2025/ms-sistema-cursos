package com.ms.certificate.service;

import com.ms.certificate.dto.CertificateIssuedEvent;
import com.ms.certificate.model.Certificate;
import com.ms.certificate.repository.CertificateRepository;
import com.ms.certificate.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

import static com.ms.certificate.config.RabbitMQConfig.ROUTING_CERTIFICATE_ISSUED;

@Service
public class CertificateService {

    private final CertificateRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public CertificateService(
            CertificateRepository repository,
            RabbitTemplate rabbitTemplate,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisher = eventPublisher;
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

        // O evento só será encaminhado ao RabbitMQ depois do commit do certificado.
        eventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishCertificateIssued(CertificateIssuedEvent event) {
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