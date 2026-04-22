package com.attendance.service;

import com.attendance.dto.SessionResponse;
import com.attendance.model.Course;
import com.attendance.model.Session;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.CourseRepository;
import com.attendance.repository.SessionRepository;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final SessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final QRCodeService qrCodeService;
    
    @Value("${app.qr.default-expiration-minutes:10}")
    private int defaultExpirationMinutes;

    @Transactional
    public SessionResponse startSession(Long courseId, Integer expirationMinutes) 
            throws WriterException, IOException {
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));

        String sessionToken = "course-" + courseId + "-time-" + System.currentTimeMillis();

        int expiration = (expirationMinutes != null) ? expirationMinutes : defaultExpirationMinutes;
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime expirationTime = startTime.plusMinutes(expiration);

        Session session = new Session();
        session.setCourse(course);
        session.setSessionToken(sessionToken);
        session.setStartTime(startTime);
        session.setExpirationTime(expirationTime);
        session.setIsActive(true);
        
        session = sessionRepository.save(session);
        String qrCodeBase64 = qrCodeService.generateQRCode(sessionToken);

        SessionResponse response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setSessionToken(sessionToken);
        response.setCourseCode(course.getCourseCode());
        response.setCourseName(course.getCourseName());
        response.setStartTime(startTime);
        response.setExpirationTime(expirationTime);
        response.setQrCodeBase64(qrCodeBase64);
        response.setAttendanceCount(0L);
        
        return response;
    }

    public SessionResponse getSessionDetails(Long sessionId) throws WriterException, IOException {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        long attendanceCount = attendanceRepository.countBySessionId(sessionId);
        
        String qrCodeBase64 = qrCodeService.generateQRCode(session.getSessionToken());
        
        SessionResponse response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setSessionToken(session.getSessionToken());
        response.setCourseCode(session.getCourse().getCourseCode());
        response.setCourseName(session.getCourse().getCourseName());
        response.setStartTime(session.getStartTime());
        response.setExpirationTime(session.getExpirationTime());
        response.setQrCodeBase64(qrCodeBase64);
        response.setAttendanceCount(attendanceCount);
        
        return response;
    }

    @Transactional
    public void endSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setIsActive(false);
        sessionRepository.save(session);
    }
}
