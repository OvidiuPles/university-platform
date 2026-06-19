package com.attendance.config;

import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthInterceptor interceptor;

    private User userWithRole(Role role) {
        User u = new User();
        u.setId(1L);
        u.setRole(role);
        return u;
    }

    private MockHttpServletRequest request(String method, String uri, String authHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        return request;
    }

    @Test
    void optionsRequestsBypassAuth() throws Exception {
        boolean result = interceptor.preHandle(
                request("OPTIONS", "/api/admin/users", null), new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }

    @Test
    void unguardedPathPassesThrough() throws Exception {
        boolean result = interceptor.preHandle(
                request("POST", "/api/auth/login", null), new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        when(authService.findByToken(null)).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(
                request("GET", "/api/student/attendance/history", null), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        when(authService.findByToken("bad-token")).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(
                request("GET", "/api/student/attendance/history", "Bearer bad-token"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void wrongRoleIsForbidden() throws Exception {
        when(authService.findByToken("student-token")).thenReturn(Optional.of(userWithRole(Role.STUDENT)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(
                request("GET", "/api/admin/users", "Bearer student-token"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void correctRoleSetsCurrentUserAndProceeds() throws Exception {
        User student = userWithRole(Role.STUDENT);
        when(authService.findByToken("student-token")).thenReturn(Optional.of(student));
        MockHttpServletRequest request = request("GET", "/api/student/attendance/history", "Bearer student-token");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute("currentUser")).isSameAs(student);
    }
}
