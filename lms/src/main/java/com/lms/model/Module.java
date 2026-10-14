package com.lms.model;

/**
 * Module entity — a group of lessons within a course.
 */
public class Module {
    private int moduleId;
    private int courseId;
    private String title;
    private int displayOrder;

    public Module() {}

    public Module(int moduleId, int courseId, String title, int displayOrder) {
        this.moduleId = moduleId;
        this.courseId = courseId;
        this.title = title;
        this.displayOrder = displayOrder;
    }

    public int getModuleId() { return moduleId; }
    public void setModuleId(int moduleId) { this.moduleId = moduleId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
