package com.attendance.config;

import com.attendance.controller.AuthController;
import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Role requiredRole = requiredRole(request.getRequestURI());
        if (requiredRole == null) {
            return true;
        }

        String token = AuthController.extractToken(request.getHeader("Authorization"));
        Optional<User> user = authService.findByToken(token);
        if (user.isEmpty()) {
            return deny(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
        }
        if (user.get().getRole() != requiredRole) {
            return deny(response, HttpServletResponse.SC_FORBIDDEN, requiredRole.name() + " access required");
        }
        return true;
    }

    private static Role requiredRole(String uri) {
        if (uri.startsWith("/api/admin")) return Role.ADMIN;
        if (uri.startsWith("/api/professor")) return Role.PROFESSOR;
        if (uri.startsWith("/api/student")) return Role.STUDENT;
        return null;
    }

    private static boolean deny(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"error\",\"message\":\"" + message + "\"}");
        return false;
    }
}
