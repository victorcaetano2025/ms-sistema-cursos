package com.ms.enrollment.dto;

public record CertificateIssuedEvent(
    Long studentId,
    Long courseId,
    String pdfUrl,
    String validationCode
) {}