package com.attendance.service;

import com.attendance.model.Role;
import com.attendance.model.User;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User student(String email, String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setName("Test Student");
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.setRole(Role.STUDENT);
        return user;
    }

    @Test
    void registerRejectsBlankName() {
        assertThatThrownBy(() -> authService.register("  ", "a@b.com", "secret123", Role.STUDENT))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Name is required!");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsBlankEmail() {
        assertThatThrownBy(() -> authService.register("Name", "", "secret123", Role.STUDENT))
                .hasMessage("Email is required!");
    }

    @Test
    void registerRejectsShortPassword() {
        assertThatThrownBy(() -> authService.register("Name", "a@b.com", "short", Role.STUDENT))
                .hasMessage("Password must be at least 6 characters");
    }

    @Test
    void registerRejectsNullRole() {
        assertThatThrownBy(() -> authService.register("Name", "a@b.com", "secret123", null))
                .hasMessage("Role is required");
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register("  Jane  ", "  Jane@University  ", "secret123", Role.STUDENT);

        assertThat(saved.getEmail()).isEqualTo("jane@university");
        assertThat(saved.getName()).isEqualTo("Jane");
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registerTranslatesDuplicateEmail() {
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> authService.register("Name", "a@b.com", "secret123", Role.STUDENT))
                .hasMessage("Email already registered");
    }

    @Test
    void loginWithUnknownEmailFails() {
        when(userRepository.findByEmail("ghost@university")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@university", "whatever"))
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(student("a@b.com", "correct-password")));

        assertThatThrownBy(() -> authService.login("a@b.com", "wrong-password"))
                .hasMessage("Invalid email or password");
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSuccessIssuesUuidToken() {
        User user = student("a@b.com", "correct-password");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.login("A@B.com", "correct-password");

        assertThat(result.getToken()).isNotBlank();
        verify(userRepository).save(user);
    }

    @Test
    void findByTokenReturnsEmptyForNullOrBlank() {
        assertThat(authService.findByToken(null)).isEmpty();
        assertThat(authService.findByToken("   ")).isEmpty();
        verify(userRepository, never()).findByToken(any());
    }

    @Test
    void findByTokenLooksUpRepository() {
        User user = student("a@b.com", "pw123456");
        when(userRepository.findByToken("tok-123")).thenReturn(Optional.of(user));

        assertThat(authService.findByToken("tok-123")).containsSame(user);
    }

    @Test
    void logoutClearsTokenWhenUserFound() {
        User user = student("a@b.com", "pw123456");
        user.setToken("tok-123");
        when(userRepository.findByToken("tok-123")).thenReturn(Optional.of(user));
        authService.logout("tok-123");

        assertThat(user.getToken()).isNull();
        verify(userRepository).save(user);
    }
}
