-- ============================================================
-- LMS Database Schema
-- Database: MySQL 8.0
-- Run as root or privileged user
-- ============================================================

DROP DATABASE IF EXISTS lms;
CREATE DATABASE lms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lms;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150)        NOT NULL,
    email         VARCHAR(255)        NOT NULL UNIQUE,
    password_hash VARCHAR(255)        NOT NULL,
    role          ENUM('STUDENT','INSTRUCTOR') NOT NULL DEFAULT 'STUDENT',
    created_date  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role  (role)
);

-- ============================================================
-- 2. COURSES
-- ============================================================
CREATE TABLE courses (
    course_id     INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200)        NOT NULL,
    description   TEXT,
    instructor_id INT                 NOT NULL,
    status        ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    created_date  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (instructor_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_courses_instructor (instructor_id),
    INDEX idx_courses_status     (status)
);

-- ============================================================
-- 3. MODULES
-- ============================================================
CREATE TABLE modules (
    module_id    INT AUTO_INCREMENT PRIMARY KEY,
    course_id    INT          NOT NULL,
    title        VARCHAR(200) NOT NULL,
    display_order INT         NOT NULL DEFAULT 1,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    INDEX idx_modules_course (course_id)
);

-- ============================================================
-- 4. LESSONS
-- ============================================================
CREATE TABLE lessons (
    lesson_id    INT AUTO_INCREMENT PRIMARY KEY,
    module_id    INT          NOT NULL,
    title        VARCHAR(200) NOT NULL,
    content      LONGTEXT,
    content_url  VARCHAR(500),
    content_type ENUM('TEXT','VIDEO','PDF','LINK') NOT NULL DEFAULT 'TEXT',
    display_order INT         NOT NULL DEFAULT 1,
    FOREIGN KEY (module_id) REFERENCES modules(module_id) ON DELETE CASCADE,
    INDEX idx_lessons_module (module_id)
);

-- ============================================================
-- 5. ENROLLMENTS
-- ============================================================
CREATE TABLE enrollments (
    enrollment_id   INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT         NOT NULL,
    course_id       INT         NOT NULL,
    enrollment_date DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('ACTIVE','COMPLETED','DROPPED') NOT NULL DEFAULT 'ACTIVE',
    UNIQUE KEY uq_enrollment (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES courses(course_id) ON DELETE CASCADE,
    INDEX idx_enrollments_student (student_id),
    INDEX idx_enrollments_course  (course_id)
);

-- ============================================================
-- 6. ASSIGNMENTS
-- ============================================================
CREATE TABLE assignments (
    assignment_id  INT AUTO_INCREMENT PRIMARY KEY,
    course_id      INT          NOT NULL,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    deadline       DATETIME     NOT NULL,
    max_marks      INT          NOT NULL DEFAULT 100,
    allow_late     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    INDEX idx_assignments_course (course_id)
);

-- ============================================================
-- 7. SUBMISSIONS
-- ============================================================
CREATE TABLE submissions (
    submission_id   INT AUTO_INCREMENT PRIMARY KEY,
    assignment_id   INT          NOT NULL,
    student_id      INT          NOT NULL,
    file_path       VARCHAR(500),
    original_name   VARCHAR(255),
    submission_date DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('SUBMITTED','GRADED','LATE') NOT NULL DEFAULT 'SUBMITTED',
    UNIQUE KEY uq_submission (assignment_id, student_id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id)    REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_submissions_assignment (assignment_id),
    INDEX idx_submissions_student    (student_id)
);

-- ============================================================
-- 8. GRADES
-- ============================================================
CREATE TABLE grades (
    grade_id       INT AUTO_INCREMENT PRIMARY KEY,
    submission_id  INT     NOT NULL UNIQUE,
    score          DECIMAL(5,2) NOT NULL,
    feedback       TEXT,
    graded_date    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    graded_by      INT      NOT NULL,
    FOREIGN KEY (submission_id) REFERENCES submissions(submission_id) ON DELETE CASCADE,
    FOREIGN KEY (graded_by)     REFERENCES users(user_id),
    INDEX idx_grades_submission (submission_id)
);

-- ============================================================
-- 9. PROGRESS
-- ============================================================
CREATE TABLE progress (
    progress_id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id           INT     NOT NULL,
    course_id            INT     NOT NULL,
    completed_lessons    INT     NOT NULL DEFAULT 0,
    total_lessons        INT     NOT NULL DEFAULT 0,
    percentage           DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    last_updated         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_progress (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES courses(course_id) ON DELETE CASCADE,
    INDEX idx_progress_student (student_id)
);

-- ============================================================
-- 10. LESSON COMPLETION (tracks which lessons a student finished)
-- ============================================================
CREATE TABLE lesson_completions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT      NOT NULL,
    lesson_id   INT      NOT NULL,
    completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_lesson_completion (student_id, lesson_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (lesson_id)  REFERENCES lessons(lesson_id) ON DELETE CASCADE
);

-- ============================================================
-- 11. NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT          NOT NULL,
    message         VARCHAR(500) NOT NULL,
    link            VARCHAR(300),
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_read (is_read)
);
