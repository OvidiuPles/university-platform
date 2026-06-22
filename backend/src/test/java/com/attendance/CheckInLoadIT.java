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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CheckInLoadIT extends AbstractIntegrationTest {

    private static final int STUDENTS = 300;
    private static final String SESSION_TOKEN = "load-session-token";

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

    private Session session;
    private final List<String> tokens = new ArrayList<>();

    @BeforeEach
    void seed() {
        attendanceRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();
        tokens.clear();

        Course course = new Course();
        course.setCourseCode("load123");
        course.setCourseName("laod123");
        course.setProfessorName("prof load");
        course = courseRepository.save(course);

        List<User> students = new ArrayList<>(STUDENTS);
        for (int i = 0; i < STUDENTS; i++) {
            String token = "load-token-" + i;
            User s = new User();
            s.setName("student-" + i);
            s.setEmail("student" + i + "@university");
            s.setPasswordHash("test-hash");
            s.setRole(Role.STUDENT);
            s.setToken(token);
            students.add(s);
            tokens.add(token);
        }
        userRepository.saveAll(students);

        session = new Session();
        session.setCourse(course);
        session.setSessionToken(SESSION_TOKEN);
        session.setStartTime(LocalDateTime.now());
        session.setExpirationTime(LocalDateTime.now().plusMinutes(10));
        session.setIsActive(true);
        session = sessionRepository.save(session);
    }

    @Test
    void threeHundredConcurrentCheckInsAreAllPersisted() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(STUDENTS);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (String token : tokens) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);
                    CheckInRequest body = new CheckInRequest(SESSION_TOKEN, null);
                    ResponseEntity<Map> resp = rest.exchange("/api/student/checkin",
                            HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

                    if (resp.getStatusCode().is2xxSuccessful()
                            && resp.getBody() != null
                            && "success".equals(resp.getBody().get("status"))) {
                        accepted.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown();

        boolean allSent = finished.await(60, TimeUnit.SECONDS);
        long submitMs = (System.nanoTime() - start) / 1_000_000;
        pool.shutdownNow();

        assertThat(allSent)
                .as("all requests should be returned")
                .isTrue();
        assertThat(accepted.get())
                .as("every request should be accepted")
                .isEqualTo(STUDENTS);
        assertThat(rejected.get())
                .as("there should be no rejected request").isZero();

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(attendanceRepository.countBySessionId(session.getId()))
                        .isEqualTo((long) STUDENTS));
        long totalMs = (System.nanoTime() - start) / 1_000_000;
        
        System.out.println("****LOAD TEST RESULTS****");
        System.out.printf("Load: %d concurrent checkins \n", STUDENTS);
        System.out.printf("HTTP submit: %d ms \n", submitMs);
        System.out.printf("Full processing: %d ms \n", totalMs);
    }
}
