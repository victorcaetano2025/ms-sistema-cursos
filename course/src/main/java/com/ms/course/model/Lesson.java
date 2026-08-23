package com.ms.course.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tb_lesson")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String videoUrl;
    private Integer orderIndex; // Ordem da lição (1, 2, 3...)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;
}