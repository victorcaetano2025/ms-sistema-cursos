package com.ms.progress.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_student_lesson_status")
public class StudentLessonStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_progress_id")
    @JsonIgnore
    private StudentProgress studentProgress;

    private Long lessonId; // ID da lição que veio do course-service
    private Boolean isLessonCompleted = false;
    private LocalDateTime completedAt;
}