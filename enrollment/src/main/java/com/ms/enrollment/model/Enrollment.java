package com.ms.enrollment.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_enrollment")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status; // ACTIVE, CANCELED, COMPLETED

    // Campo atualizado via evento (RabbitMQ) quando o certificado for gerado
    private String certificateUrl; 

    private LocalDateTime enrolledAt = LocalDateTime.now();
}
