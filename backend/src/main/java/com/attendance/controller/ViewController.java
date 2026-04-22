package com.attendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/professor")
    public String professorDashboard() {
        return "professor-dashboard";
    }

    @GetMapping("/checkin")
    public String studentCheckIn() {
        return "student-checkin";
    }

    @GetMapping("/student/history")
    public String studentHistory() {
        return "student-history";
    }

    @GetMapping("/professor/history")
    public String professorHistory() {
        return "professor-history";
    }
}
