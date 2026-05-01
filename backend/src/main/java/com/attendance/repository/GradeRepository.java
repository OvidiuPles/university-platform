package com.attendance.repository;

import com.attendance.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByCourseIdOrderByStudent_NameAsc(Long courseId);

    List<Grade> findByStudentIdOrderByCourse_CourseCodeAscCreatedAtDesc(Long studentId);

    List<Grade> findByStudentIdAndCourseIdOrderByCreatedAtDesc(Long studentId, Long courseId);
}
