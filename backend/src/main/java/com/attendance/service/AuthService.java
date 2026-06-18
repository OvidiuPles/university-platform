package com.attendance.service;

import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(String name, String email, String rawPassword, Role role, String studentId) {
        if (name == null || name.trim().isEmpty()) throw new RuntimeException("Name is required!");
        if (email == null || email.trim().isEmpty()) throw new RuntimeException("Email is required!");
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        if (role == null) throw new RuntimeException("Role is required");

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStudentId(role == Role.STUDENT && studentId != null && !studentId.isBlank()
                ? studentId.trim() : null);
        user.setCreatedAt(LocalDateTime.now());
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Email already registered");
        }
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email == null ? null : email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        user.setToken(UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    public void logout(String token) {
        userRepository.findByToken(token).ifPresent(user -> {
            user.setToken(null);
            userRepository.save(user);
        });
    }

    public Optional<User> findByToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return userRepository.findByToken(token);
    }
}
