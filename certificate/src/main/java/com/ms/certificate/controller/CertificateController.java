package com.ms.certificate.controller;

import com.ms.certificate.model.Certificate;
import com.ms.certificate.repository.CertificateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateRepository repository;

    public CertificateController(CertificateRepository repository) {
        this.repository = repository;
    }

    // 1. Endpoint para o Aluno pegar o link do PDF
    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<Certificate> getCertificate(
            @PathVariable Long studentId, 
            @PathVariable Long courseId) {

        return repository.findByStudentIdAndCourseId(studentId, courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); 
    }

    // 2. Endpoint público para validação do código do certificado (ex: recrutadores)
    @GetMapping("/validate/{validationCode}")
    public ResponseEntity<Certificate> validateCertificate(
            @PathVariable String validationCode) {

        // Nota: você precisará criar o método findByValidationCode(String code) no seu CertificateRepository
        return repository.findByValidationCode(validationCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); 
    }
}