package com.ms.course.dto;

public record EnrollmentCreatedEvent(
    Long enrollmentId,
    Long studentId,
    Long courseId
) {}