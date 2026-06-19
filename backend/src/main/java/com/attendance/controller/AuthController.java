package com.attendance.controller;

import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            Role role = parseRole(body.get("role"));
            if (role == Role.ADMIN) {
                return error("Admin accounts cannot be self-registered");
            }
            User user = authService.register(
                    body.get("name"),
                    body.get("email"),
                    body.get("password"),
                    role);
            return ResponseEntity.ok(authPayload(user));
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            User user = authService.login(body.get("email"), body.get("password"));
            return ResponseEntity.ok(authPayload(user));
        } catch (RuntimeException e) {
            return error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(extractToken(authHeader));
        return ResponseEntity.ok(Map.of("status", "success", "message", "Logged out"));
    }

    public static String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }

    private static Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) throw new RuntimeException("Role is required");
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + raw);
        }
    }

    private static Map<String, Object> authPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", user.getToken());
        payload.put("id", user.getId());
        payload.put("name", user.getName());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole().name());
        return payload;
    }

    private static ResponseEntity<?> error(String msg) {
        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", msg));
    }
}
