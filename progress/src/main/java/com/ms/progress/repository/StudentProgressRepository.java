package com.ms.progress.repository;

import com.ms.progress.model.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {
    // Método customizado para buscar o progresso específico de um aluno num curso
    Optional<StudentProgress> findByStudentIdAndCourseId(Long studentId, Long courseId);
}