package com.ms.progress.dto;

// Evento lançado quando um aluno completa uma lição em um curso. 
// Este evento é produzido pelo serviço de progresso e consumido por outros serviços interessados

public record LessonCompletedEvent(
    Long studentId,
    Long courseId,
    Long lessonId,
    Long totalLessons
) {}