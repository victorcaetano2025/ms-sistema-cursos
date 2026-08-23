package com.ms.enrollment.dto;

public record EnrollmentCreatedEvent(
    Long enrollmentId,
    Long studentId,
    Long courseId
) {}

