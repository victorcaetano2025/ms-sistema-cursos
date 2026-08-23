package com.ms.enrollment.repository;

import com.ms.enrollment.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Não esqueça de importar o Optional!

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Adicione esta linha abaixo para o Spring gerar o SQL automaticamente:
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

}