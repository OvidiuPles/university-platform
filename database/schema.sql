CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    student_id VARCHAR(50),
    token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    student_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    course_code VARCHAR(20) UNIQUE NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    professor_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id),
    session_token VARCHAR(255) UNIQUE NOT NULL,
    start_time TIMESTAMP NOT NULL,
    expiration_time TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES sessions(id),
    student_id BIGINT NOT NULL REFERENCES students(id),
    check_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(session_id, student_id)
);

CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id),
    course_id BIGINT NOT NULL REFERENCES courses(id),
    grade_value NUMERIC(5,2) NOT NULL,
    grade_type VARCHAR(50) NOT NULL,
    description VARCHAR(70),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_grades_course ON grades(course_id);

INSERT INTO students (student_id, name, email) VALUES
('S001', 'Alice Pop', 'alice@university'),
('S002', 'Ion Popescu', 'ion@university'),
('S003', 'Andrei Vanic', 'andrei@university'),
('S004', 'David Brumaru', 'david@university'),
('S005', 'Evea Stan', 'eva@university');

INSERT INTO courses (course_code, course_name, professor_name) VALUES
('CS101', 'Computer Science Basics', 'Dr. Popescu'),
('CS201', 'Data Structures and Algorithms', 'Dr. Ionescu'),
('CS301', 'Database Systems', 'Dr. Johnson');

-- passwords: admin / proffesor / student.
INSERT INTO users (name, email, password_hash, role, student_id) VALUES
('Admin User', 'admin', '$2a$10$lhMBGpSbEea7kOsdC25xDO655mH2ZVkCFXoCLaSa0N4mgcvkUqlFu', 'ADMIN', NULL),
('Dr. Popescu', 'professor', '$2a$10$01hwtlCdQKjpXYmRFcYrau6EW/PUriMe2.rbmvS3evlyz98XYuYwG', 'PROFESSOR', NULL),
('Alice Pop', 'student', '$2a$10$E50ONLQsQ3Ckv8C4bLAGMuSbO1mAfQ7EDavGAHneRYDYojFpL3SW.', 'STUDENT', 'S001');
