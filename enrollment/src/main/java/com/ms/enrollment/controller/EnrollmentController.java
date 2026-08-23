package com.ms.enrollment.controller;

import com.ms.enrollment.model.Enrollment;
import com.ms.enrollment.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // Criamos um DTO simples para receber os dados no body da requisição
    public record EnrollmentRequest(Long studentId, Long courseId) {}

    @PostMapping
    public ResponseEntity<Enrollment> enrollStudent(@RequestBody EnrollmentRequest request) {
        // Chama o Service que salva no banco e dispara o evento no RabbitMQ
        Enrollment savedEnrollment = enrollmentService.createEnrollment(request.studentId(), request.courseId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEnrollment);
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<Enrollment> getEnrollment(@PathVariable Long enrollmentId) {
        return enrollmentService.findById(enrollmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}