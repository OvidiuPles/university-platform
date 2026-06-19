package com.attendance;

import com.attendance.dto.CheckInRequest;
import com.attendance.model.Course;
import com.attendance.model.Role;
import com.attendance.model.Session;
import com.attendance.model.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.SessionRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CheckInFlowIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;

    private User student;
    private Session session;

    @BeforeEach
    void seed() {
        attendanceRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();

        Course course = new Course();
        course.setCourseCode("CS101");
        course.setCourseName("test course");
        course.setProfessorName("prof test");
        course = courseRepository.save(course);

        student = new User();
        student.setName("studentt");
        student.setEmail("sttest@university");
        student.setPasswordHash("test-hash");
        student.setRole(Role.STUDENT);
        student.setToken("student-token-123");
        student = userRepository.save(student);

        session = new Session();
        session.setCourse(course);
        session.setSessionToken("session-token-abc");
        session.setStartTime(LocalDateTime.now());
        session.setExpirationTime(LocalDateTime.now().plusMinutes(10));
        session.setIsActive(true);
        session = sessionRepository.save(session);
    }

    private ResponseEntity<Map> checkIn(String bearerToken, String sessionToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        CheckInRequest body = new CheckInRequest(sessionToken, null);
        return rest.exchange("/api/student/checkin", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
    }

    @Test
    void checkInIsQueuedThenPersistedAsynchronously() {
        ResponseEntity<Map> response = checkIn("student-token-123", "session-token-abc");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "success");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(attendanceRepository.findBySessionIdAndStudentId(session.getId(), student.getId()))
                        .isPresent());
        assertThat(attendanceRepository.countBySessionId(session.getId())).isEqualTo(1L);
    }

    @Test
    void duplicateCheckInDoesNotCreateSecondRow() {
        checkIn("student-token-123", "session-token-abc");
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(attendanceRepository.countBySessionId(session.getId())).isEqualTo(1L));

        ResponseEntity<Map> second = checkIn("student-token-123", "session-token-abc");

        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody()).containsEntry("status", "multiple-attempts");
    }

    @Test
    void validateRejectsEndedSession() {
        session.setIsActive(false);
        sessionRepository.save(session);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("student-token-123");
        CheckInRequest body = new CheckInRequest("session-token-abc", null);
        ResponseEntity<Map> response = rest.exchange("/api/student/validate/checkin", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "This session has been ended");
    }
}
