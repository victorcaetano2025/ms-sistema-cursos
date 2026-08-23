package com.ms.certificate.dto;

public record CourseCompletedEvent(
    Long studentId,
    Long courseId
) {}