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

INSERT INTO students (student_id, name, email) VALUES
('S001', 'Alice Johnson', 'alice@university.edu'),
('S002', 'Bob Smith', 'bob@university.edu'),
('S003', 'Carol Williams', 'carol@university.edu'),
('S004', 'David Brown', 'david@university.edu'),
('S005', 'Eve Davis', 'eve@university.edu');

INSERT INTO courses (course_code, course_name, professor_name) VALUES
('CS101', 'Introduction to Computer Science', 'Dr. Smith'),
('CS201', 'Data Structures and Algorithms', 'Dr. Johnson'),
('CS301', 'Database Systems', 'Dr. Williams');
