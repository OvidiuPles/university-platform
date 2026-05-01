package com.attendance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class GradeRequest {
    private String studentId;
    private Long courseId;
    private BigDecimal gradeValue;
    private String gradeType;
    private String description;
}
