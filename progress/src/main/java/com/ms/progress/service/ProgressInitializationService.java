package com.ms.progress.service;

import com.ms.progress.model.StudentProgress;
import com.ms.progress.repository.StudentProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressInitializationService {

    private final StudentProgressRepository repository;

    public ProgressInitializationService(StudentProgressRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void initializeProgress(Long enrollmentId, Long studentId, Long courseId) {
        // 1. Verifica se já existe para evitar duplicidade (idempotência)
        // Isso é muito importante caso o RabbitMQ entregue a mensagem duas vezes!
        if (repository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
            System.out.println("Progresso já inicializado para o aluno " + studentId + " no curso " + courseId);
            return;
        }

        // 2. Cria o progresso inicial (0%)
        StudentProgress progress = new StudentProgress();
        progress.setEnrollmentId(enrollmentId);
        progress.setStudentId(studentId);
        progress.setCourseId(courseId);
        progress.setCompletionPercentage(0.0);
        progress.setIsCourseCompleted(false);

        /* 
         * 💡 DICA DE ARQUITETURA:
         * Como o progress-service não tem acesso ao banco de dados do course-service, 
         * ele não sabe quantas aulas esse curso tem logo de cara.
         * 
         * Como resolver isso? 
         * Você pode deixar para criar os registros de `StudentLessonStatus` dinamicamente 
         * conforme o aluno assiste as aulas (no updateLessonProgress), 
         * OU usar o Spring Cloud OpenFeign para fazer uma chamada HTTP rápida 
         * (GET /api/courses/{courseId}/lessons) só para pegar os IDs das aulas e preencher aqui.
         */

        // 3. Salva no banco de dados
        repository.save(progress);
        
        System.out.println("Progresso inicial (0%) criado com sucesso para o aluno: " + studentId);
    }
}