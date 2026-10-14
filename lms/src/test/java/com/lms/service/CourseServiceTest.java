package com.lms.service;

import com.lms.dao.CourseDAO;
import com.lms.dao.EnrollmentDAO;
import com.lms.dao.NotificationDAO;
import com.lms.model.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CourseService Unit Tests")
class CourseServiceTest {

    @Mock private CourseDAO courseDAO;
    @Mock private EnrollmentDAO enrollmentDAO;
    @Mock private NotificationDAO notificationDAO;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseDAO, enrollmentDAO, notificationDAO);
    }

    @Test
    @DisplayName("Create course creates draft course for instructor")
    void testCreateCourse() throws SQLException {
        when(courseDAO.create(any(Course.class))).thenReturn(10);

        Course c = courseService.createCourse("Java Enterprise Systems", "Comprehensive course", 1);

        assertNotNull(c);
        assertEquals(10, c.getCourseId());
        assertEquals("Java Enterprise Systems", c.getTitle());
        assertEquals("DRAFT", c.getStatus());
        assertEquals(1, c.getInstructorId());
    }

    @Test
    @DisplayName("Update course fails if requester does not own course")
    void testUpdateCourseAccessDenied() throws SQLException {
        Course existing = new Course();
        existing.setCourseId(10);
        existing.setInstructorId(1); // owned by instructor 1

        when(courseDAO.findById(10)).thenReturn(Optional.of(existing));

        // instructor 2 tries to update
        assertThrows(SecurityException.class, () ->
            courseService.updateCourse(10, "New Title", "New Desc", "PUBLISHED", 2)
        );
        verify(courseDAO, never()).update(any(Course.class));
    }

    @Test
    @DisplayName("Enroll student fails if course is not published")
    void testEnrollUnpublishedCourse() throws SQLException {
        Course draftCourse = new Course();
        draftCourse.setCourseId(5);
        draftCourse.setStatus("DRAFT");

        when(courseDAO.findById(5)).thenReturn(Optional.of(draftCourse));

        assertThrows(IllegalArgumentException.class, () ->
            courseService.enroll(100, 5)
        );
        verify(enrollmentDAO, never()).enroll(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Enroll student fails if already enrolled")
    void testDuplicateEnrollment() throws SQLException {
        Course pubCourse = new Course();
        pubCourse.setCourseId(5);
        pubCourse.setStatus("PUBLISHED");

        when(courseDAO.findById(5)).thenReturn(Optional.of(pubCourse));
        when(enrollmentDAO.isEnrolled(100, 5)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
            courseService.enroll(100, 5)
        );
        verify(enrollmentDAO, never()).enroll(100, 5);
    }

    @Test
    @DisplayName("Successful enrollment calls DAO and sends notification")
    void testEnrollSuccess() throws SQLException {
        Course pubCourse = new Course();
        pubCourse.setCourseId(5);
        pubCourse.setTitle("Modern Cloud Architecture");
        pubCourse.setStatus("PUBLISHED");

        when(courseDAO.findById(5)).thenReturn(Optional.of(pubCourse));
        when(enrollmentDAO.isEnrolled(100, 5)).thenReturn(false);

        courseService.enroll(100, 5);

        verify(enrollmentDAO, times(1)).enroll(100, 5);
        verify(notificationDAO, times(1)).create(eq(100), anyString(), anyString());
    }
}
