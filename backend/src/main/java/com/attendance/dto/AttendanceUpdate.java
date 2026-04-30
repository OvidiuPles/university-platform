package com.attendance.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceUpdate {
    private Long sessionId;
    private String studentName;
    private long totalCount;
    private String message;
    private LocalDateTime checkInTime;
}
