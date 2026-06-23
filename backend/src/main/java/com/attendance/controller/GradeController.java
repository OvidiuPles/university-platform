package com.attendance.controller;

import com.attendance.dto.GradeRequest;
import com.attendance.model.Grade;
import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.repository.UserRepository;
import com.attendance.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final UserRepository userRepository;

    @PostMapping("/professor/grades")
    public ResponseEntity<?> createGrade(@RequestBody GradeRequest request) {
        try {
            Grade grade = gradeService.createGrade(request);
            return ResponseEntity.ok(toGradeRow(grade));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/professor/grades/{gradeId}")
    public ResponseEntity<Map<String, String>> deleteGrade(@PathVariable Long gradeId) {
        try {
            gradeService.deleteGrade(gradeId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Grade deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/professor/grades")
    public ResponseEntity<?> getCourseGrades(@RequestParam Long courseId) {
        try {
            List<Grade> grades = gradeService.getGradesForCourse(courseId);
            Map<Long, Map<String, Object>> byStudent = new LinkedHashMap<>();

            userRepository.findByRole(Role.STUDENT).stream()
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .forEach(student -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", student.getId());
                        row.put("name", student.getName());
                        row.put("email", student.getEmail());
                        row.put("grades", new ArrayList<Map<String, Object>>());
                        byStudent.put(student.getId(), row);
                    });

            for (Grade g : grades) {
                Map<String, Object> studentRow = byStudent.get(g.getStudent().getId());
                if (studentRow == null) continue;
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) studentRow.get("grades");
                list.add(toGradeRow(g));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("courseId", courseId);
            response.put("students", new ArrayList<>(byStudent.values()));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/student/grades")
    public ResponseEntity<?> getStudentGrades(@RequestAttribute("currentUser") User user) {
        try {
            List<Grade> grades = gradeService.getGradesForStudent(user.getId());

            Map<Long, Map<String, Object>> byCourse = new LinkedHashMap<>();
            for (Grade g : grades) {
                Long courseId = g.getCourse().getId();
                Map<String, Object> courseRow = byCourse.computeIfAbsent(courseId, k -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("courseId", g.getCourse().getId());
                    row.put("courseCode", g.getCourse().getCourseCode());
                    row.put("courseName", g.getCourse().getCourseName());
                    row.put("professorName", g.getCourse().getProfessorName());
                    row.put("grades", new ArrayList<Map<String, Object>>());
                    return row;
                });
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) courseRow.get("grades");
                list.add(toGradeRow(g));
            }

            for (Map<String, Object> row : byCourse.values()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) row.get("grades");
                row.put("count", list.size());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", user.getId());
            response.put("totalGrades", grades.size());
            response.put("courses", new ArrayList<>(byCourse.values()));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private Map<String, Object> toGradeRow(Grade g) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", g.getId());
        row.put("studentId", g.getStudent().getId());
        row.put("studentName", g.getStudent().getName());
        row.put("courseId", g.getCourse().getId());
        row.put("courseCode", g.getCourse().getCourseCode());
        row.put("courseName", g.getCourse().getCourseName());
        row.put("gradeValue", g.getGradeValue());
        row.put("gradeType", g.getGradeType());
        row.put("description", g.getDescription());
        row.put("createdAt", g.getCreatedAt());
        return row;
    }
}
