package com.attendance.service;

import com.attendance.config.RabbitMQConfig;
import com.attendance.dto.AttendanceUpdate;
import com.attendance.dto.CheckInRequest;
import com.attendance.model.Attendance;
import com.attendance.model.Session;
import com.attendance.model.Student;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.SessionRepository;
import com.attendance.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final RabbitTemplate rabbitTemplate;
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

    public void queueCheckIn(CheckInRequest request) {
        log.info("Queuing check-in request for student: {} in session token: {}", 
                 request.getStudentId(), request.getSessionToken());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ATTENDANCE_QUEUE, request);
    }

    @RabbitListener(queues = RabbitMQConfig.ATTENDANCE_QUEUE)
    @Transactional
    public void processCheckIn(CheckInRequest request) {
        try {
            log.info("Processing check-in for student: {}", request.getStudentId());

            Session session = sessionRepository.findBySessionToken(request.getSessionToken())
                .orElseThrow(() -> new RuntimeException("Invalid session token"));

            if (!session.getIsActive()) {
                throw new RuntimeException("Session is not active");
            }
            
            if (session.isExpired()) {
                throw new RuntimeException("Session has expired");
            }

            Student student = studentRepository.findByStudentId(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

            if (attendanceRepository.findBySessionIdAndStudentId(session.getId(), student.getId()).isPresent()) {
                log.warn("Duplicate attendance attempt for student: {} in session: {}", 
                         student.getStudentId(), session.getId());
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
                student.getName() + " checked in successfully"
            );
            
            messagingTemplate.convertAndSend("/topic/attendance/" + session.getId(), update);
            
            log.info("Check-in processed successfully for student: {}", student.getStudentId());
            
        } catch (Exception e) {
            log.error("Error processing check-in: {}", e.getMessage(), e);
        }
    }

    public List<Attendance> getSessionAttendance(Long sessionId) {
        return attendanceRepository.findBySessionId(sessionId);
    }

    public Map.Entry<Session, List<Attendance>> getSessionAttendanceByToken(String sessionToken) {
        Session session = sessionRepository.findBySessionToken(sessionToken)
            .orElseThrow(() -> new RuntimeException("No session found for this token. Please check and try again."));
        List<Attendance> attendanceList = attendanceRepository.findBySessionId(session.getId());
        return Map.entry(session, attendanceList);
    }

    public List<Attendance> getStudentAttendanceHistory(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRepository.findByStudentId(student.getId());
    }
}
