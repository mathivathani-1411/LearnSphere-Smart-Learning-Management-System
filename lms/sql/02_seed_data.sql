-- ============================================================
-- LMS Sample Data — run AFTER 01_schema.sql
-- Passwords are BCrypt hashes of "password123"
-- ============================================================
USE lms;

-- --------------------------------------------------------
-- USERS  (2 instructors + 3 students)
-- --------------------------------------------------------
INSERT INTO users (name, email, password_hash, role) VALUES
('Alice Johnson',    'alice@lms.com',   '$2a$12$K8G5GZbvV7j/gN6JfFaBMuY7Wh0w0IGKZ7FqO7j4NmHKZr0a2XDWG', 'INSTRUCTOR'),
('Bob Smith',        'bob@lms.com',     '$2a$12$K8G5GZbvV7j/gN6JfFaBMuY7Wh0w0IGKZ7FqO7j4NmHKZr0a2XDWG', 'INSTRUCTOR'),
('Charlie Brown',    'charlie@lms.com', '$2a$12$K8G5GZbvV7j/gN6JfFaBMuY7Wh0w0IGKZ7FqO7j4NmHKZr0a2XDWG', 'STUDENT'),
('Diana Prince',     'diana@lms.com',   '$2a$12$K8G5GZbvV7j/gN6JfFaBMuY7Wh0w0IGKZ7FqO7j4NmHKZr0a2XDWG', 'STUDENT'),
('Edward Norton',    'edward@lms.com',  '$2a$12$K8G5GZbvV7j/gN6JfFaBMuY7Wh0w0IGKZ7FqO7j4NmHKZr0a2XDWG', 'STUDENT');

-- --------------------------------------------------------
-- COURSES
-- --------------------------------------------------------
INSERT INTO courses (title, description, instructor_id, status) VALUES
('Java Programming for Beginners',  'Learn Java from scratch with hands-on projects.', 1, 'PUBLISHED'),
('Web Development with HTML & CSS', 'Build beautiful websites with modern HTML5 and CSS3.', 1, 'PUBLISHED'),
('Data Structures & Algorithms',    'Master DSA concepts in Java.', 2, 'PUBLISHED'),
('Python Basics',                   'Introduction to Python for complete beginners.', 2, 'DRAFT');

-- --------------------------------------------------------
-- MODULES
-- --------------------------------------------------------
INSERT INTO modules (course_id, title, display_order) VALUES
(1, 'Getting Started with Java',  1),
(1, 'Object-Oriented Programming',2),
(1, 'Collections & Generics',     3),
(2, 'HTML Fundamentals',          1),
(2, 'CSS Styling',                2),
(3, 'Arrays & Strings',           1),
(3, 'Linked Lists & Trees',       2);

-- --------------------------------------------------------
-- LESSONS
-- --------------------------------------------------------
INSERT INTO lessons (module_id, title, content, content_type, display_order) VALUES
(1, 'Introduction to Java',     'Java is a high-level, class-based, object-oriented programming language...', 'TEXT', 1),
(1, 'Setting Up JDK & IDE',     'Step-by-step guide to install JDK 17 and IntelliJ IDEA...', 'TEXT', 2),
(1, 'Hello World Program',      'Your first Java program: System.out.println("Hello, World!");', 'TEXT', 3),
(2, 'Classes and Objects',      'A class is a blueprint from which individual objects are created...', 'TEXT', 1),
(2, 'Inheritance in Java',      'Inheritance is a mechanism in which one class acquires the properties...', 'TEXT', 2),
(2, 'Polymorphism & Interfaces','Polymorphism means many forms...', 'TEXT', 3),
(3, 'ArrayList & LinkedList',   'Java Collections Framework provides...', 'TEXT', 1),
(3, 'HashMap & HashSet',        'Hash-based collections provide O(1) average-time access...', 'TEXT', 2),
(4, 'HTML Document Structure',  '<!DOCTYPE html> declares the document type...', 'TEXT', 1),
(4, 'HTML Forms & Inputs',      'HTML forms collect user input...', 'TEXT', 2),
(5, 'CSS Selectors',            'CSS selectors select the HTML element you want to style...', 'TEXT', 1),
(5, 'Flexbox Layout',           'Flexbox provides an efficient way to layout, align and distribute space...', 'TEXT', 2),
(6, 'Array Basics',             'An array is a collection of items stored at contiguous memory locations...', 'TEXT', 1),
(6, 'Sorting Algorithms',       'Bubble sort, Selection sort, Insertion sort, Merge sort, Quick sort...', 'TEXT', 2),
(7, 'Singly Linked List',       'A linked list is a linear data structure where elements are not stored...', 'TEXT', 1),
(7, 'Binary Search Tree',       'A Binary Search Tree is a rooted binary tree where...', 'TEXT', 2);

-- --------------------------------------------------------
-- ENROLLMENTS
-- --------------------------------------------------------
INSERT INTO enrollments (student_id, course_id, status) VALUES
(3, 1, 'ACTIVE'),
(3, 2, 'ACTIVE'),
(4, 1, 'ACTIVE'),
(4, 3, 'ACTIVE'),
(5, 2, 'ACTIVE'),
(5, 3, 'ACTIVE');

-- --------------------------------------------------------
-- ASSIGNMENTS
-- --------------------------------------------------------
INSERT INTO assignments (course_id, title, description, deadline, max_marks, allow_late) VALUES
(1, 'Java Basics Quiz',           'Complete the basic Java programming exercises.', DATE_ADD(NOW(), INTERVAL 7 DAY),  100, FALSE),
(1, 'OOP Design Assignment',      'Design a class hierarchy for a library system.',DATE_ADD(NOW(), INTERVAL 14 DAY), 100, TRUE),
(2, 'Build a Portfolio Page',     'Create a personal portfolio using HTML & CSS.',  DATE_ADD(NOW(), INTERVAL 10 DAY), 100, FALSE),
(3, 'Implement a Linked List',    'Implement a doubly linked list in Java.',        DATE_ADD(NOW(), INTERVAL 7 DAY),  100, FALSE),
(3, 'Sorting Algorithm Analysis', 'Compare the performance of 3 sorting algorithms.',DATE_ADD(NOW(), INTERVAL 5 DAY), 100, TRUE);

-- --------------------------------------------------------
-- SUBMISSIONS (charlie and diana have submitted some)
-- --------------------------------------------------------
INSERT INTO submissions (assignment_id, student_id, file_path, original_name, status) VALUES
(1, 3, 'uploads/submissions/asgn1_charlie.pdf',  'JavaBasics.pdf',  'SUBMITTED'),
(2, 3, 'uploads/submissions/asgn2_charlie.pdf',  'OOPDesign.pdf',   'GRADED'),
(1, 4, 'uploads/submissions/asgn1_diana.pdf',    'Java_Diana.pdf',  'GRADED'),
(3, 5, 'uploads/submissions/asgn3_edward.pdf',   'Portfolio.zip',   'SUBMITTED');

-- --------------------------------------------------------
-- GRADES
-- --------------------------------------------------------
INSERT INTO grades (submission_id, score, feedback, graded_by) VALUES
(2, 88.0, 'Good design! Add more detail to the UML diagram next time.', 1),
(3, 92.5, 'Excellent work! Very clear and well-structured code.', 1);

-- --------------------------------------------------------
-- PROGRESS
-- --------------------------------------------------------
INSERT INTO progress (student_id, course_id, completed_lessons, total_lessons, percentage) VALUES
(3, 1, 5, 8, 62.50),
(3, 2, 3, 4, 75.00),
(4, 1, 3, 8, 37.50),
(4, 3, 2, 4, 50.00),
(5, 2, 1, 4, 25.00),
(5, 3, 0, 4, 0.00);

-- --------------------------------------------------------
-- NOTIFICATIONS
-- --------------------------------------------------------
INSERT INTO notifications (user_id, message, link, is_read) VALUES
(3, 'You have been enrolled in "Java Programming for Beginners".', '/student/my-courses.html', FALSE),
(3, 'Assignment "Java Basics Quiz" is due in 7 days.',           '/student/assignments.html', FALSE),
(3, 'Your OOP Design Assignment has been graded: 88/100.',       '/student/grades.html',      FALSE),
(4, 'Your Java Basics Quiz has been graded: 92.5/100.',          '/student/grades.html',      FALSE),
(4, 'New assignment posted in Data Structures: Sorting Analysis.','/student/assignments.html', TRUE),
(1, 'Charlie submitted "Java Basics Quiz".',                     '/instructor/submissions.html', FALSE),
(1, 'New enrollment: Diana Prince joined Java Programming.',     '/instructor/students.html',    TRUE);
