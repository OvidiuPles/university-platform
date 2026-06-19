package com.attendance.service;

import com.attendance.dto.GradeRequest;
import com.attendance.model.Course;
import com.attendance.model.Grade;
import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.GradeRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private GradeService gradeService;

    private GradeRequest request(BigDecimal value, String type) {
        GradeRequest r = new GradeRequest();
        r.setStudentId(1L);
        r.setCourseId(2L);
        r.setGradeValue(value);
        r.setGradeType(type);
        return r;
    }

    private User userWithRole(Role role) {
        User u = new User();
        u.setId(1L);
        u.setRole(role);
        return u;
    }

    private Course course() {
        Course c = new Course();
        c.setId(2L);
        c.setCourseCode("CS101");
        return c;
    }

    @Test
    void createGradeRejectsNullValue() {
        assertThatThrownBy(() -> gradeService.createGrade(request(null, "Exam")))
                .hasMessage("Grade value is required");
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void createGradeRejectsValueBelowOne() {
        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("0.5"), "Exam")))
                .hasMessage("Grade must be between 1 and 10");
    }

    @Test
    void createGradeRejectsValueAboveTen() {
        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("10.5"), "Exam")))
                .hasMessage("Grade must be between 1 and 10");
    }

    @Test
    void createGradeRejectsBlankType() {
        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("8"), "  ")))
                .hasMessage("Grade type is required");
    }

    @Test
    void createGradeRejectsTooLongType() {
        String longType = "x".repeat(51);
        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("8"), longType)))
                .hasMessage("Grade type must be 50 characters or fewer");
    }

    @Test
    void createGradeRejectsMissingStudent() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("8"), "Exam")))
                .hasMessageContaining("Student not found");
    }

    @Test
    void createGradeRejectsNonStudentUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole(Role.PROFESSOR)));

        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("8"), "Exam")))
                .hasMessageContaining("User is not a student");
    }

    @Test
    void createGradeRejectsMissingCourse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole(Role.STUDENT)));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.createGrade(request(new BigDecimal("8"), "Exam")))
                .hasMessage("Course not found");
    }

    @Test
    void createGradePersistsValidGrade() {
        GradeRequest r = request(new BigDecimal("8.5"), "  Exam  ");
        r.setDescription("  Midterm  ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole(Role.STUDENT)));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course()));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        Grade saved = gradeService.createGrade(r);

        assertThat(saved.getGradeValue()).isEqualByComparingTo("8.5");
        assertThat(saved.getGradeType()).isEqualTo("Exam");
        assertThat(saved.getDescription()).isEqualTo("Midterm");
    }

    @Test
    void deleteGradeRejectsUnknownId() {
        when(gradeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> gradeService.deleteGrade(99L))
                .hasMessage("Grade not found");
        verify(gradeRepository, never()).deleteById(any());
    }

    @Test
    void getGradesForCourseRejectsUnknownCourse() {
        when(courseRepository.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> gradeService.getGradesForCourse(2L))
                .hasMessage("Course not found");
    }
}
