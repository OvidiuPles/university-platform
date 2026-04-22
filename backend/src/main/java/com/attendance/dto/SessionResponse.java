package com.attendance.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {
    private Long sessionId;
    private String sessionToken;
    private String courseCode;
    private String courseName;
    private LocalDateTime startTime;
    private LocalDateTime expirationTime;
    private String qrCodeBase64;
    private long attendanceCount;
}
