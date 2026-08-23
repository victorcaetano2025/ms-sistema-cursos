package com.ms.course.repository;


import com.ms.course.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

	long countByCourseId(Long courseId);
}