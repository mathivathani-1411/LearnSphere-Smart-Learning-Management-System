package com.lms.model;

/**
 * Lesson entity — a single piece of content within a module.
 */
public class Lesson {
    private int lessonId;
    private int moduleId;
    private String title;
    private String content;
    private String contentUrl;
    private String contentType; // TEXT | VIDEO | PDF | LINK
    private int displayOrder;

    public Lesson() {}

    public Lesson(int lessonId, int moduleId, String title, String content,
                  String contentUrl, String contentType, int displayOrder) {
        this.lessonId = lessonId;
        this.moduleId = moduleId;
        this.title = title;
        this.content = content;
        this.contentUrl = contentUrl;
        this.contentType = contentType;
        this.displayOrder = displayOrder;
    }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public int getModuleId() { return moduleId; }
    public void setModuleId(int moduleId) { this.moduleId = moduleId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentUrl() { return contentUrl; }
    public void setContentUrl(String contentUrl) { this.contentUrl = contentUrl; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
