package com.attendance.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceUpdate {
    private Long sessionId;
    private String studentName;
    private long totalCount;
    private String message;
}
