package com.attendance.service;

import com.attendance.dto.SessionResponse;
import com.attendance.model.Course;
import com.attendance.model.Session;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private SessionService sessionService;

    @BeforeEach
    void setDefaultExpiration() {
        ReflectionTestUtils.setField(sessionService, "defaultExpirationMinutes", 10);
    }

    private Course course() {
        Course c = new Course();
        c.setId(7L);
        c.setCourseCode("CS101");
        c.setCourseName("Intro");
        return c;
    }

    @Test
    void startSessionRejectsMissingCourse() throws Exception {
        when(courseRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.startSession(7L, null))
                .hasMessage("Course not found");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void startSessionUsesDefaultExpirationWhenNotProvided() throws Exception {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course()));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(qrCodeService.generateQRCode(anyString())).thenReturn("qr-base64");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        SessionResponse response = sessionService.startSession(7L, null);

        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();
        long minutes = Duration.between(saved.getStartTime(), saved.getExpirationTime()).toMinutes();
        assertThat(minutes).isEqualTo(10);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getSessionToken()).startsWith("course-7-time-");
        assertThat(response.getQrCodeBase64()).isEqualTo("qr-base64");
        assertThat(response.getAttendanceCount()).isZero();
    }

    @Test
    void startSessionHonoursCustomExpiration() throws Exception {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course()));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(qrCodeService.generateQRCode(anyString())).thenReturn("qr-base64");

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        sessionService.startSession(7L, 45);

        verify(sessionRepository).save(captor.capture());
        long minutes = Duration.between(captor.getValue().getStartTime(),
                captor.getValue().getExpirationTime()).toMinutes();
        assertThat(minutes).isEqualTo(45);
    }

    @Test
    void endSessionDeactivatesSession() {
        Session session = new Session();
        session.setId(3L);
        session.setIsActive(true);
        session.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionService.endSession(3L);

        assertThat(session.getIsActive()).isFalse();
        verify(sessionRepository).save(session);
    }

    @Test
    void endSessionRejectsMissingSession() {
        when(sessionRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.endSession(3L))
                .hasMessage("Session not found");
        verify(sessionRepository, never()).save(any());
    }
}
