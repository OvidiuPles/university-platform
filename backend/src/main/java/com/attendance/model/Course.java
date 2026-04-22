package com.attendance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "course_code", unique = true, nullable = false)
    private String courseCode;
    
    @Column(name = "course_name", nullable = false)
    private String courseName;
    
    @Column(name = "professor_name", nullable = false)
    private String professorName;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
