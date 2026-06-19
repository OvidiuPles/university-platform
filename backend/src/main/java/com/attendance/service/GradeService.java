package com.attendance.service;

import com.attendance.dto.GradeRequest;
import com.attendance.model.Course;
import com.attendance.model.Grade;
import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.GradeRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private static final BigDecimal MIN_GRADE = new BigDecimal("1");
    private static final BigDecimal MAX_GRADE = new BigDecimal("10");

    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public Grade createGrade(GradeRequest request) {
        validateGradeValue(request.getGradeValue());
        validateGradeType(request.getGradeType());

        User student = userRepository.findByStudentId(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found: " + request.getStudentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student" + request.getStudentId());
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setCourse(course);
        grade.setGradeValue(request.getGradeValue());
        grade.setGradeType(request.getGradeType().trim());
        grade.setDescription(request.getDescription() == null ? null : request.getDescription().trim());

        Grade saved = gradeRepository.save(grade);
        log.info("Created grade {} for student {} in course {}",
                saved.getGradeValue(), student.getStudentId(), course.getCourseCode());
        return saved;
    }

    @Transactional
    public void deleteGrade(Long gradeId) {
        if (!gradeRepository.existsById(gradeId)) {
            throw new RuntimeException("Grade not found");
        }
        gradeRepository.deleteById(gradeId);
    }

    public List<Grade> getGradesForCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found");
        }
        return gradeRepository.findByCourseIdOrderByStudent_NameAsc(courseId);
    }

    public List<Grade> getGradesForStudent(Long userId) {
        return gradeRepository.findByStudentIdOrderByCourse_CourseCodeAscCreatedAtDesc(userId);
    }

    private void validateGradeValue(BigDecimal value) {
        if (value == null) {
            throw new RuntimeException("Grade value is required");
        }
        if (value.compareTo(MIN_GRADE) < 0 || value.compareTo(MAX_GRADE) > 0) {
            throw new RuntimeException("Grade must be between 1 and 10");
        }
    }

    private void validateGradeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new RuntimeException("Grade type is required");
        }
        if (type.length() > 50) {
            throw new RuntimeException("Grade type must be 50 characters or fewer");
        }
    }
}
