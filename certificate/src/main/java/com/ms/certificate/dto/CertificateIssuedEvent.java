package com.ms.certificate.dto;

public record CertificateIssuedEvent(
    Long studentId,
    Long courseId,
    String pdfUrl,
    String validationCode
) {}