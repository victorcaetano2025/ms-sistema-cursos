package com.ms.progress.dto;

public record EnrollmentCreatedEvent(
    Long enrollmentId,
    Long studentId,
    Long courseId
) {}