package com.lms.service;

import com.lms.dao.*;
import com.lms.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Unit Tests")
class AssignmentServiceTest {

    @Mock private AssignmentDAO assignmentDAO;
    @Mock private SubmissionDAO submissionDAO;
    @Mock private GradeDAO gradeDAO;
    @Mock private EnrollmentDAO enrollmentDAO;
    @Mock private CourseDAO courseDAO;
    @Mock private NotificationDAO notificationDAO;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(
                assignmentDAO, submissionDAO, gradeDAO,
                enrollmentDAO, courseDAO, notificationDAO
        );
    }

    @Test
    @DisplayName("Create assignment throws exception if deadline is in the past")
    void testCreateAssignmentPastDeadline() throws SQLException {
        Course course = new Course();
        course.setCourseId(1);
        course.setInstructorId(10);
        when(courseDAO.findById(1)).thenReturn(Optional.of(course));

        LocalDateTime past = LocalDateTime.now().minusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
            assignmentService.createAssignment(1, "HW1", "Desc", past, 100, false, 10)
        );
    }

    @Test
    @DisplayName("Create assignment throws SecurityException if instructor does not own course")
    void testCreateAssignmentNotOwner() throws SQLException {
        Course course = new Course();
        course.setCourseId(1);
        course.setInstructorId(10); // owned by 10
        when(courseDAO.findById(1)).thenReturn(Optional.of(course));

        LocalDateTime future = LocalDateTime.now().plusDays(5);
        // instructor 99 tries to create
        assertThrows(SecurityException.class, () ->
            assignmentService.createAssignment(1, "HW1", "Desc", future, 100, false, 99)
        );
    }

    @Test
    @DisplayName("Submit throws SecurityException if student is not enrolled in the course")
    void testSubmitNotEnrolled() throws SQLException {
        Assignment asg = new Assignment();
        asg.setAssignmentId(10);
        asg.setCourseId(1);
        asg.setDeadline(LocalDateTime.now().plusDays(3));

        when(assignmentDAO.findById(10)).thenReturn(Optional.of(asg));
        when(enrollmentDAO.isEnrolled(50, 1)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
            assignmentService.submit(10, 50, "uploads/sub.pdf", "sub.pdf")
        );
    }

    @Test
    @DisplayName("Submit past deadline throws IllegalStateException if allowLate is false")
    void testSubmitPastDeadlineLateDisallowed() throws SQLException {
        Assignment asg = new Assignment();
        asg.setAssignmentId(10);
        asg.setCourseId(1);
        asg.setDeadline(LocalDateTime.now().minusHours(2));
        asg.setAllowLate(false);

        when(assignmentDAO.findById(10)).thenReturn(Optional.of(asg));
        when(enrollmentDAO.isEnrolled(50, 1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
            assignmentService.submit(10, 50, "uploads/sub.pdf", "sub.pdf")
        );
    }

    @Test
    @DisplayName("Submit succeeds and notifies the course instructor")
    void testSubmitSuccess() throws SQLException {
        Assignment asg = new Assignment();
        asg.setAssignmentId(10);
        asg.setCourseId(1);
        asg.setTitle("Homework 1");
        asg.setDeadline(LocalDateTime.now().plusDays(2));

        Course course = new Course();
        course.setCourseId(1);
        course.setInstructorId(10);

        when(assignmentDAO.findById(10)).thenReturn(Optional.of(asg));
        when(enrollmentDAO.isEnrolled(50, 1)).thenReturn(true);
        when(submissionDAO.findByAssignmentAndStudent(10, 50)).thenReturn(Optional.empty());
        when(submissionDAO.create(any(Submission.class))).thenReturn(1);
        when(courseDAO.findById(1)).thenReturn(Optional.of(course));

        Submission sub = assignmentService.submit(10, 50, "submissions/abc.pdf", "abc.pdf");

        assertNotNull(sub);
        assertEquals(1, sub.getSubmissionId());
        verify(notificationDAO, times(1)).create(eq(10), anyString(), anyString());
    }

    @Test
    @DisplayName("getSubmission returns submission for authorized student")
    void testGetSubmissionAuthorized() throws SQLException {
        Submission sub = new Submission();
        sub.setSubmissionId(1);
        sub.setStudentId(50);
        when(submissionDAO.findById(1)).thenReturn(Optional.of(sub));

        Submission result = assignmentService.getSubmission(1, 50, "STUDENT");
        assertNotNull(result);
        assertEquals(1, result.getSubmissionId());
    }

    @Test
    @DisplayName("getSubmission rejects student requesting another student's submission")
    void testGetSubmissionUnauthorizedStudent() throws SQLException {
        Submission sub = new Submission();
        sub.setSubmissionId(1);
        sub.setStudentId(50);
        when(submissionDAO.findById(1)).thenReturn(Optional.of(sub));

        assertThrows(SecurityException.class, () ->
            assignmentService.getSubmission(1, 99, "STUDENT")
        );
    }

    @Test
    @DisplayName("Grading rejects score greater than maxMarks")
    void testGradingScoreExceedsMax() throws SQLException {
        Submission sub = new Submission();
        sub.setSubmissionId(1);
        sub.setAssignmentId(10);
        sub.setStudentId(50);

        Assignment asg = new Assignment();
        asg.setAssignmentId(10);
        asg.setCourseId(1);
        asg.setMaxMarks(100);

        Course course = new Course();
        course.setCourseId(1);
        course.setInstructorId(10);

        when(submissionDAO.findById(1)).thenReturn(Optional.of(sub));
        when(assignmentDAO.findById(10)).thenReturn(Optional.of(asg));
        when(courseDAO.findById(1)).thenReturn(Optional.of(course));

        assertThrows(IllegalArgumentException.class, () ->
            assignmentService.grade(1, 105.0, "Great", 10)
        );
    }

    @Test
    @DisplayName("Grading succeeds with valid score, updates status and notifies student")
    void testGradingSuccess() throws SQLException {
        Submission sub = new Submission();
        sub.setSubmissionId(1);
        sub.setAssignmentId(10);
        sub.setStudentId(50);

        Assignment asg = new Assignment();
        asg.setAssignmentId(10);
        asg.setTitle("Project 1");
        asg.setCourseId(1);
        asg.setMaxMarks(100);

        Course course = new Course();
        course.setCourseId(1);
        course.setInstructorId(10);

        when(submissionDAO.findById(1)).thenReturn(Optional.of(sub));
        when(assignmentDAO.findById(10)).thenReturn(Optional.of(asg));
        when(courseDAO.findById(1)).thenReturn(Optional.of(course));
        when(gradeDAO.findBySubmission(1)).thenReturn(Optional.empty());
        when(gradeDAO.create(any(Grade.class))).thenReturn(5);

        Grade g = assignmentService.grade(1, 95.0, "Excellent submission!", 10);

        assertNotNull(g);
        assertEquals(95.0, g.getScore());
        verify(submissionDAO, times(1)).updateStatus(1, "GRADED");
        verify(notificationDAO, times(1)).create(eq(50), anyString(), anyString());
    }
}
