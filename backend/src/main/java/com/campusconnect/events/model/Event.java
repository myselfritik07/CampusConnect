package com.campusconnect.events.model;

public class Event {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String venue;
    private String startTime;

    public Event() {
    }

    public Event(Long id, String title, String description, String category, String venue, String startTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.startTime = startTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
}
