package com.attendance.controller;

import com.attendance.dto.CheckInRequest;
import com.attendance.model.Attendance;
import com.attendance.service.AttendanceService;
import com.attendance.repository.SessionRepository;
import com.attendance.repository.AttendanceRepository;
import com.attendance.model.Session;
import com.attendance.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@CrossOrigin(origins = {"${app.base-url}", "http://localhost:8090", "http://localhost:8080"})
public class StudentController {
    
    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    
    @PostMapping("/validate/checkin")
    public ResponseEntity<Map<String, String>> validateCheckin(@RequestBody CheckInRequest request) {
        try {
            attendanceService.validateCheckIn(request);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Check-in validated"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/checkin")
    public ResponseEntity<Map<String, String>> checkIn(@RequestBody CheckInRequest request,
                                                       @RequestAttribute("currentUser") User user) {
        try {
            request.setUserId(user.getId());
            // check for duplicated requests
            Session session = sessionRepository.findBySessionToken(request.getSessionToken())
                    .orElseThrow(() -> new RuntimeException("Invalid session token"));

            if (attendanceRepository.findBySessionIdAndStudentId(session.getId(), user.getId()).isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "status", "multiple-attempts",
                        "message", "Already checked in"
                ));
            }
            attendanceService.processCheckIn(request);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Check-in submitted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/attendance/history")
    public ResponseEntity<Map<String, Object>> getAttendanceHistory(
            @RequestAttribute("currentUser") User user) {
        try {
            List<Attendance> history =
                    attendanceService.getStudentAttendanceHistory(user.getId());

            long sessionsCount = sessionRepository.count();

            Map<String, Object> response = new HashMap<>();
            response.put("history", history);
            response.put("sessionsCount", sessionsCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
