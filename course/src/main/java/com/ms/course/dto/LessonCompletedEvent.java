package com.ms.course.dto;

public record LessonCompletedEvent(
    Long studentId,
    Long courseId,
    Long lessonId,
    Long totalLessons
) {}