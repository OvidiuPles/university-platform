package com.attendance;

import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private String registerStudentAndLogin() {
        ResponseEntity<Map> register = rest.postForEntity("/api/auth/register", Map.of(
                "name", "Jane Student",
                "email", "jane@university",
                "password", "secret123",
                "role", "STUDENT"), Map.class);
        assertThat(register.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<Map> login = rest.postForEntity("/api/auth/login", Map.of(
                "email", "jane@university",
                "password", "secret123"), Map.class);
        assertThat(login.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) login.getBody().get("token");
    }

    private ResponseEntity<Map> get(String path, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    @Test
    void studentTokenCanReachStudentEndpoint() {
        String token = registerStudentAndLogin();

        ResponseEntity<Map> response = get("/api/student/attendance/history", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void studentTokenIsForbiddenOnAdminEndpoint() {
        String token = registerStudentAndLogin();

        ResponseEntity<Map> response = get("/api/admin/users", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingTokenIsUnauthorized() {
        ResponseEntity<Map> response = get("/api/student/attendance/history", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
