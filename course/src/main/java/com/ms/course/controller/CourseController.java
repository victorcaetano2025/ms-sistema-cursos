package com.ms.course.controller;

import com.ms.course.model.Course;
import com.ms.course.model.Lesson;
import com.ms.course.repository.CourseRepository;
import com.ms.course.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import com.ms.course.service.CourseContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseContentService courseContentService;
    private final CourseRepository courseRepository; // Adicionamos o repositório aqui
    private final LessonRepository lessonRepository;

    public CourseController(CourseContentService courseContentService,
            CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseContentService = courseContentService;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    public record CourseRequest(String title, String description) {}

    public record LessonRequest(String title, String videoUrl, Integer orderIndex) {}

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CourseRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("title é obrigatório");
        }

        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());

        return ResponseEntity.status(HttpStatus.CREATED).body(courseRepository.save(course));
    }

    @PostMapping("/{courseId}/lessons")
    public ResponseEntity<Lesson> createLesson(
            @PathVariable Long courseId,
            @RequestBody LessonRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("title é obrigatório");
        }

        Lesson lesson = new Lesson();
        lesson.setTitle(request.title());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setOrderIndex(request.orderIndex());
        lesson.setCourse(course);

        return ResponseEntity.status(HttpStatus.CREATED).body(lessonRepository.save(lesson));
    }

    // Retorna o curso real do banco de dados (que já tem a lista de lições dentro dele)
    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourse(@PathVariable Long courseId) {
        return courseRepository.findById(courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); 
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<Void> completeLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam Long studentId) { 

        courseContentService.completeLesson(studentId, courseId, lessonId);
        
        return ResponseEntity.ok().build();
    }
}