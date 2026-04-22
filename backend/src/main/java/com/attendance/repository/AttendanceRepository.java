package com.attendance.repository;

import com.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    Optional<Attendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    
    List<Attendance> findBySessionId(Long sessionId);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.session.id = :sessionId")
    long countBySessionId(Long sessionId);
    
    List<Attendance> findByStudentId(Long studentId);
}
