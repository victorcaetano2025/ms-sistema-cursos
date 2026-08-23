package com.ms.progress.dto;

// Evento que este serviço vai PRODUZIR
public record CourseCompletedEvent(
    Long studentId,
    Long courseId
) {}
