package com.ms.progress.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tb_student_progress")
public class StudentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long enrollmentId; // ID da matrícula gerada no enrollment-service
    private Long studentId;
    private Long courseId;

    private Double completionPercentage = 0.0;
    private Boolean isCourseCompleted = false;
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "studentProgress", cascade = CascadeType.ALL)
    private List<StudentLessonStatus> lessonStatuses = new ArrayList<>();
}
