package com.attendance.controller;

import com.attendance.dto.SessionResponse;
import com.attendance.model.Attendance;
import com.attendance.model.Course;
import com.attendance.model.Role;
import com.attendance.model.Session;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.SessionRepository;
import com.attendance.repository.UserRepository;
import com.attendance.service.AttendanceService;
import com.attendance.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
@CrossOrigin(origins = {"${app.base-url}", "http://localhost:8090", "http://localhost:8080"})
public class ProfessorController {
    
    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Value("${app.qr.default-expiration-minutes:10}")
    private int defaultExpirationMinutes;

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    @GetMapping("/session/config")
    public ResponseEntity<Map<String, Object>> getSessionConfig() {
        return ResponseEntity.ok(Map.of(
            "defaultExpirationMinutes", defaultExpirationMinutes
        ));
    }

    @PostMapping("/session/start")
    public ResponseEntity<SessionResponse> startSession(
            @RequestParam Long courseId,
            @RequestParam(required = false) Integer expirationMinutes) {
        try {
            SessionResponse response = sessionService.startSession(courseId, expirationMinutes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<Map<String, String>> endSession(@PathVariable Long sessionId) {
        try {
            sessionService.endSession(sessionId);
            return ResponseEntity.ok(Map.of("message", "Session ended successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/session/history")
    public ResponseEntity<?> getSessionHistory(@RequestParam String token) {
        try {
            var entry = attendanceService.getSessionAttendanceByToken(token);
            var session = entry.getKey();
            var attendanceList = entry.getValue();

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("sessionToken",   session.getSessionToken());
            response.put("courseCode",      session.getCourse().getCourseCode());
            response.put("courseName",     session.getCourse().getCourseName());
            response.put("professorName",  session.getCourse().getProfessorName());
            response.put("startTime",      session.getStartTime());
            response.put("expirationTime", session.getExpirationTime());
            response.put("isActive",       session.getIsActive());
            response.put("totalAttendance", attendanceList.size());

            List<Map<String, Object>> students = new java.util.ArrayList<>();
            for (var a : attendanceList) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("id",          a.getStudent().getId());
                row.put("name",        a.getStudent().getName());
                row.put("email",       a.getStudent().getEmail());
                row.put("checkInTime", a.getCheckInTime());
                students.add(row);
            }
            response.put("students", students);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/attendance-rate")
    public ResponseEntity<?> getCourseAttendanceRate(@RequestParam Long courseId) {
        try {
            if (!courseRepository.existsById(courseId)) {
                throw new RuntimeException("Course not found");
            }

            LocalDateTime now = LocalDateTime.now();
            List<Session> completedSessions = sessionRepository.findByCourseId(courseId).stream()
                    .filter(s -> Boolean.FALSE.equals(s.getIsActive()) || s.getExpirationTime().isBefore(now))
                    .toList();
            int totalSessions = completedSessions.size();

            Map<Long, Set<Long>> attendedSessionsByStudent = new java.util.HashMap<>();
            for (Session session : completedSessions) {
                List<Attendance> attendanceList = attendanceRepository.findBySessionId(session.getId());
                for (Attendance a : attendanceList) {
                    attendedSessionsByStudent
                            .computeIfAbsent(a.getStudent().getId(), k -> new HashSet<>())
                            .add(session.getId());
                }
            }

            List<Map<String, Object>> students = userRepository.findByRole(Role.STUDENT).stream()
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .map(student -> {
                        int attended = attendedSessionsByStudent.getOrDefault(student.getId(), Set.of()).size();
                        BigDecimal rate = totalSessions == 0
                                ? null
                                : BigDecimal.valueOf(attended)
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);

                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", student.getId());
                        row.put("rate", rate);
                        return row;
                    })
                    .toList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("courseId", courseId);
            response.put("students", students);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
