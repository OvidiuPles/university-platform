package com.attendance.service;

import com.attendance.config.RabbitMQConfig;
import com.attendance.dto.AttendanceUpdate;
import com.attendance.dto.CheckInRequest;
import com.attendance.model.Attendance;
import com.attendance.model.Session;
import com.attendance.model.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.SessionRepository;
import com.attendance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AttendanceService attendanceService;

    private CheckInRequest request() {
        return new CheckInRequest("token-abc", 1L);
    }

    private Session activeSession() {
        Session s = new Session();
        s.setId(5L);
        s.setSessionToken("token-abc");
        s.setIsActive(true);
        s.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        return s;
    }

    @Test
    void validateRejectsUnknownToken() {
        when(sessionRepository.findBySessionToken("token-abc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.validateCheckIn(request()))
                .hasMessage("Invalid QR code");
    }

    @Test
    void validateRejectsEndedSession() {
        Session s = activeSession();
        s.setIsActive(false);
        when(sessionRepository.findBySessionToken("token-abc")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> attendanceService.validateCheckIn(request()))
                .hasMessage("This session has been ended");
    }

    @Test
    void validateRejectsExpiredSession() {
        Session s = activeSession();
        s.setExpirationTime(LocalDateTime.now().minusMinutes(1));
        when(sessionRepository.findBySessionToken("token-abc")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> attendanceService.validateCheckIn(request()))
                .hasMessage("This QR code has expired");
    }

    @Test
    void queueCheckInPublishesToAttendanceQueue() {
        CheckInRequest req = request();

        attendanceService.queueCheckIn(req);
        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.ATTENDANCE_QUEUE, req);
    }

    @Test
    void processCheckInPersistsAndBroadcasts() {
        Session session = activeSession();
        User student = new User();
        student.setId(1L);
        student.setName("Jane");
        when(sessionRepository.findBySessionToken("token-abc")).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findBySessionIdAndStudentId(5L, 1L)).thenReturn(Optional.empty());
        when(attendanceRepository.countBySessionId(5L)).thenReturn(1L);

        attendanceService.processCheckIn(request());

        verify(attendanceRepository).save(any(Attendance.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/attendance/5"), any(AttendanceUpdate.class));
    }

    @Test
    void processCheckInSkipsDuplicateWithoutSavingOrBroadcasting() {
        Session session = activeSession();
        User student = new User();
        student.setId(1L);
        when(sessionRepository.findBySessionToken("token-abc")).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findBySessionIdAndStudentId(5L, 1L))
                .thenReturn(Optional.of(new Attendance()));

        attendanceService.processCheckIn(request());

        verify(attendanceRepository, never()).save(any(Attendance.class));
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
