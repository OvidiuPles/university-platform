package com.attendance.controller;

import com.attendance.dto.GradeRequest;
import com.attendance.model.Attendance;
import com.attendance.model.Course;
import com.attendance.model.Grade;
import com.attendance.model.Role;
import com.attendance.model.Session;
import com.attendance.model.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.GradeRepository;
import com.attendance.repository.SessionRepository;
import com.attendance.repository.UserRepository;
import com.attendance.service.AuthService;
import com.attendance.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;
    private final GradeService gradeService;
    private final AuthService authService;

    @GetMapping("/students")
    public ResponseEntity<List<User>> listStudents() {
        return ResponseEntity.ok(userRepository.findByRole(Role.STUDENT));
    }


    @PutMapping("/students/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            User s = userRepository.findById(id)
                    .filter(u -> u.getRole() == Role.STUDENT)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            if (body.containsKey("studentId")) s.setStudentId(body.get("studentId"));
            if (body.containsKey("name")) s.setName(body.get("name"));
            if (body.containsKey("email")) s.setEmail(body.get("email"));
            return ResponseEntity.ok(userRepository.save(s));
        } catch (DataIntegrityViolationException e) {
            return error("Student ID or e-mail already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            boolean isStudent = userRepository.findById(id)
                    .map(u -> u.getRole() == Role.STUDENT)
                    .orElse(false);
            if (!isStudent) return error("Student not found");
            userRepository.deleteById(id);
            return ok("Student deleted");
        } catch (DataIntegrityViolationException e) {
            return error("Cannot delete, student has related attendance or grades");
        }

    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> listCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(@RequestBody Map<String, String> body) {
        try {
            Course c = new Course();
            c.setCourseCode(required(body, "courseCode"));
            c.setCourseName(required(body, "courseName"));
            c.setProfessorName(required(body, "professorName"));
            c.setCreatedAt(LocalDateTime.now());
            return ResponseEntity.ok(courseRepository.save(c));
        } catch (DataIntegrityViolationException e) {
            return error("Course code already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Course c = courseRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            if (body.containsKey("courseCode")) c.setCourseCode(body.get("courseCode"));
            if (body.containsKey("courseName")) c.setCourseName(body.get("courseName"));
            if (body.containsKey("professorName")) c.setProfessorName(body.get("professorName"));
            return ResponseEntity.ok(courseRepository.save(c));
        } catch (DataIntegrityViolationException e) {
            return error("Course code already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            if (!courseRepository.existsById(id)) return error("Course not found");
            courseRepository.deleteById(id);
            return ok("Course deleted");
        } catch (DataIntegrityViolationException e) {
            return error("Cannot delete course, has related sessions or grades");
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> listSessions() {
        return ResponseEntity.ok(sessionRepository.findAll());
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> body) {
        try {
            Long courseId = asLong(body.get("courseId"));
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            Session s = new Session();
            s.setCourse(course);
            s.setSessionToken(asString(body.get("sessionToken")));
            s.setStartTime(asDateTime(body.get("startTime")));
            s.setExpirationTime(asDateTime(body.get("expirationTime")));
            s.setIsActive(asBoolean(body.get("isActive"), true));
            s.setCreatedAt(LocalDateTime.now());
            return ResponseEntity.ok(sessionRepository.save(s));
        } catch (DataIntegrityViolationException e) {
            return error("Session token already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Session s = sessionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
            if (body.containsKey("courseId")) {
                Long courseId = asLong(body.get("courseId"));
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new RuntimeException("Course not found"));
                s.setCourse(course);
            }
            if (body.containsKey("sessionToken")) s.setSessionToken(asString(body.get("sessionToken")));
            if (body.containsKey("startTime")) s.setStartTime(asDateTime(body.get("startTime")));
            if (body.containsKey("expirationTime")) s.setExpirationTime(asDateTime(body.get("expirationTime")));
            if (body.containsKey("isActive")) s.setIsActive(asBoolean(body.get("isActive"), true));
            return ResponseEntity.ok(sessionRepository.save(s));
        } catch (DataIntegrityViolationException e) {
            return error("Session token already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id) {
        try {
            if (!sessionRepository.existsById(id)) return error("Session not found");
            sessionRepository.deleteById(id);
            return ok("Session deleted");
        } catch (DataIntegrityViolationException e) {
            return error("Cannot delete session!");
        }
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<Attendance>> listAttendance() {
        return ResponseEntity.ok(attendanceRepository.findAll());
    }

    @PostMapping("/attendance")
    public ResponseEntity<?> createAttendance(@RequestBody Map<String, Object> body) {
        try {
            Long sessionId = asLong(body.get("sessionId"));
            String studentId = asString(body.get("studentId")); // personal ID, not the PK
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found!"));
            User student = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
            Attendance a = new Attendance();
            a.setSession(session);
            a.setStudent(student);
            a.setCheckInTime(body.get("checkInTime") != null
                    ? asDateTime(body.get("checkInTime"))
                    : LocalDateTime.now());
            return ResponseEntity.ok(attendanceRepository.save(a));
        } catch (DataIntegrityViolationException e) {
            return error("Attendance already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PutMapping("/attendance/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Attendance a = attendanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Attendance not found!"));
            if (body.containsKey("sessionId")) {
                Long sessionId = asLong(body.get("sessionId"));
                Session session = sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new RuntimeException("Session not found"));
                a.setSession(session);
            }
            if (body.containsKey("studentId")) {
                String studenId = asString(body.get("studentId"));
                User student = userRepository.findByStudentId(studenId)
                        .orElseThrow(() -> new RuntimeException("Student not found: " + studenId));
                a.setStudent(student);
            }
            if (body.containsKey("checkInTime")) a.setCheckInTime(asDateTime(body.get("checkInTime")));
            return ResponseEntity.ok(attendanceRepository.save(a));
        } catch (DataIntegrityViolationException e) {
            return error("Attendance already exists");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/attendance/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long id) {
        if (!attendanceRepository.existsById(id)) return error("Attendance not found");
        attendanceRepository.deleteById(id);
        return ok("Attendance deleted");
    }

    @GetMapping("/grades")
    public ResponseEntity<List<Grade>> listGrades() {
        return ResponseEntity.ok(gradeRepository.findAll());
    }

    @PostMapping("/grades")
    public ResponseEntity<?> createGrade(@RequestBody GradeRequest request) {
        try {
            return ResponseEntity.ok(gradeService.createGrade(request));
        } catch (DataIntegrityViolationException e) {
            return error("Could not save grade");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    
    @PutMapping("/grades/{id}")
    public ResponseEntity<?> updateGrade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Grade g = gradeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Grade not found!"));
            if (body.containsKey("studentId")) {
                User student = userRepository.findByStudentId(asString(body.get("studentId")))
                        .orElseThrow(() -> new RuntimeException("Student not found"));
                g.setStudent(student);
            }
            if (body.containsKey("courseId")) {
                Course course = courseRepository.findById(asLong(body.get("courseId")))
                        .orElseThrow(() -> new RuntimeException("Course not found!"));
                g.setCourse(course);
            }
            if (body.containsKey("gradeValue")) {
                java.math.BigDecimal v = new java.math.BigDecimal(asString(body.get("gradeValue")));
                if (v.compareTo(new java.math.BigDecimal("1")) < 0
                        || v.compareTo(new java.math.BigDecimal("10")) > 0) {
                    throw new RuntimeException("Grade must be between 1 and 10");
                }
                g.setGradeValue(v);
            }
            if (body.containsKey("gradeType")) {
                String t = asString(body.get("gradeType"));
                if (t == null || t.trim().isEmpty()) throw new RuntimeException("Grade type is required");
                if (t.length() > 50) throw new RuntimeException("Grade type must be 50 characters or fewer");
                g.setGradeType(t.trim());
            }
            if (body.containsKey("description")) {
                Object d = body.get("description");
                g.setDescription(d == null ? null : d.toString().trim());
            }
            return ResponseEntity.ok(gradeRepository.save(g));
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @DeleteMapping("/grades/{id}")
    public ResponseEntity<?> deleteGrade(@PathVariable Long id) {
        try {
            gradeService.deleteGrade(id);
            return ok("Grade deleted!");
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    private static String required(Map<String, String> body, String key) {
        String v = body.get(key);
        if (v == null || v.trim().isEmpty()) {
            throw new RuntimeException(key + " is required");
        }
        return v.trim();
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long asLong(Object o) {
        if (o == null) throw new RuntimeException("Missing numeric value");
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private static Boolean asBoolean(Object o, boolean defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }

    private static LocalDateTime asDateTime(Object o) {
        if (o == null) throw new RuntimeException("Missing date/time value");
        return LocalDateTime.parse(o.toString());
    }

    private static ResponseEntity<?> error(String msg) {
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", msg));
    }

    private static ResponseEntity<?> ok(String msg) {
        return ResponseEntity.ok(Map.of("status", "success", "message", msg));
    }
}
