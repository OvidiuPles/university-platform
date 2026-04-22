# Real-Time Attendance System

## Objective
Create a classroom attendance system with QR code generation and real-time tracking

## Requirements
### Professor Interface
* Generate unique QR codes for each class session
* QR code must contain: timestamp, course ID, optional GPS coordinates
* Set expiration time for QR codes (default: 10 minutes from class start)
* View real-time attendance count and student list

### Student Interface
* Scan QR code to mark attendance
* Receive confirmation of successful check-in
* View personal attendance history

### System features
* QR codes automatically expire after specified time
* Real-time attendance counter visible to professor
* Handle concurrent student check-ins
* Prevent duplicate attendance marking