package com.ms.certificate.repository;

import com.ms.certificate.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    // Método usado para o aluno consultar o próprio certificado
    Optional<Certificate> findByStudentIdAndCourseId(Long studentId, Long courseId);

    // Método NOVO usado para o endpoint público de validação (recrutadores, empresas)
    Optional<Certificate> findByValidationCode(String validationCode);

}