package com.traveling.app.travelpath.models;

import java.util.List;

public class TripStep {
    private String title;
    private String description;
    private String timeSlot; // e.g., "09:00 - 11:00"
    private int budget;
    private double latitude;
    private double longitude;
    private String activityType; // e.g., "Culture"
    private List<String> imageUrls;
    private String transportInfo; // e.g., "15 min de marche (1.2 km)"

    public TripStep() {}

    public TripStep(String title, String description, String timeSlot, int budget, double latitude, double longitude, String activityType, List<String> imageUrls) {
        this.title = title;
        this.description = description;
        this.timeSlot = timeSlot;
        this.budget = budget;
        this.latitude = latitude;
        this.longitude = longitude;
        this.activityType = activityType;
        this.imageUrls = imageUrls;
        this.transportInfo = null;
    }

    public String getTransportInfo() { return transportInfo; }
    public void setTransportInfo(String transportInfo) { this.transportInfo = transportInfo; }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public int getBudget() { return budget; }
    public void setBudget(int budget) { this.budget = budget; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
