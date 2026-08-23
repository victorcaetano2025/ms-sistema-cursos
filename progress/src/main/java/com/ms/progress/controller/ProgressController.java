package com.ms.progress.controller;

import com.ms.progress.model.StudentProgress;
import com.ms.progress.repository.StudentProgressRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final StudentProgressRepository repository;

    public ProgressController(StudentProgressRepository repository) {
        this.repository = repository;
    }
//segunda parte para o front-end, para pegar o progresso do aluno em um curso específico
    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<StudentProgress> getStudentProgress(
            @PathVariable Long studentId, 
            @PathVariable Long courseId) {

        return repository.findByStudentIdAndCourseId(studentId, courseId)
                .map(progress -> ResponseEntity.ok(progress))
                .orElse(ResponseEntity.notFound().build()); // Retorna 404 se o aluno não tiver começado
    }

   
}