package com.attendance.controller;

import com.attendance.dto.SessionResponse;
import com.attendance.model.Attendance;
import com.attendance.model.Course;
import com.attendance.repository.CourseRepository;
import com.attendance.service.AttendanceService;
import com.attendance.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/professor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfessorController {
    
    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final CourseRepository courseRepository;

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
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

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionResponse> getSessionDetails(@PathVariable Long sessionId) {
        try {
            SessionResponse response = sessionService.getSessionDetails(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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

    @GetMapping("/session/{sessionId}/attendance")
    public ResponseEntity<List<Attendance>> getAttendanceList(@PathVariable Long sessionId) {
        List<Attendance> attendanceList = attendanceService.getSessionAttendance(sessionId);
        return ResponseEntity.ok(attendanceList);
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
                row.put("studentId",   a.getStudent().getStudentId());
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
}
