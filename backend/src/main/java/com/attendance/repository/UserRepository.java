package com.attendance.repository;

import com.attendance.model.Role;
import com.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByToken(String token);
    Optional<User> findByStudentId(String studentId);
    List<User> findByRole(Role role);
}
