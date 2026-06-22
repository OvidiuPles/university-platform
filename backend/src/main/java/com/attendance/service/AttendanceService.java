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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    

    public void validateCheckIn(CheckInRequest request) {
        Session session = sessionRepository.findBySessionToken(request.getSessionToken())
            .orElseThrow(() -> new RuntimeException("Invalid QR code"));

        if (!session.getIsActive()) {
            throw new RuntimeException("This session has been ended");
        }

        if (session.isExpired()) {
            throw new RuntimeException("This QR code has expired");
        }
    }

    @Transactional
    public void processCheckIn(CheckInRequest request) {
        log.info("Processing check-in for user: {}", request.getUserId());

        Session session = sessionRepository.findBySessionToken(request.getSessionToken())
            .orElseThrow(() -> new RuntimeException("Invalid session token"));

        if (!session.getIsActive()) {
            throw new RuntimeException("Session is not active");
        }

        if (session.isExpired()) {
            throw new RuntimeException("Session has expired");
        }

        User student = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("Student not found"));

        if (attendanceRepository.findBySessionIdAndStudentId(session.getId(), student.getId()).isPresent()) {
            log.warn("Duplicate attendance attempt for student: {} in session: {}",
                     student.getId(), session.getId());
            return;
        }

        Attendance attendance = new Attendance();
        attendance.setSession(session);
        attendance.setStudent(student);
        attendance.setCheckInTime(LocalDateTime.now());

        attendanceRepository.save(attendance);

        long totalCount = attendanceRepository.countBySessionId(session.getId());

        AttendanceUpdate update = new AttendanceUpdate(
            session.getId(),
            student.getName(),
            totalCount,
            student.getName() + " checked in successfully",
            attendance.getCheckInTime()
        );

        messagingTemplate.convertAndSend("/topic/attendance/" + session.getId(), update);

        log.info("Check-in processed successfully for student: {}", student.getId());
    }

    public Map.Entry<Session, List<Attendance>> getSessionAttendanceByToken(String sessionToken) {
        Session session = sessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new RuntimeException("No session found for this token. Please check and try again."));
        List<Attendance> attendanceList = attendanceRepository.findBySessionId(session.getId());
        return Map.entry(session, attendanceList);
    }

    public List<Attendance> getStudentAttendanceHistory(Long userId) {
        return attendanceRepository.findByStudentId(userId);
    }
}
