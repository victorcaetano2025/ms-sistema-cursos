package com.ms.certificate.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tb_certificate")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, updatable = false)
    private String validationCode = UUID.randomUUID().toString(); // Código para validar no site

    private Long studentId;
    private Long courseId;

    private String pdfUrl; // Link do AWS S3 ou Storage local

    private LocalDateTime issuedAt = LocalDateTime.now();
}