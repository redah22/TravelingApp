package com.traveling.app.travelpath.models;

import java.util.List;

public class TripPath {
    private String name; // e.g., "Economique", "Equilibré", "Confort"
    private List<TripStep> steps;
    private int totalBudget;
    private int durationHours;
    private UserPreferences.EffortLevel effortLevel;
    private boolean isLiked;

    public TripPath() {}

    public TripPath(String name, List<TripStep> steps, int totalBudget, int durationHours, UserPreferences.EffortLevel effortLevel) {
        this.name = name;
        this.steps = steps;
        this.totalBudget = totalBudget;
        this.durationHours = durationHours;
        this.effortLevel = effortLevel;
        this.isLiked = false;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<TripStep> getSteps() { return steps; }
    public void setSteps(List<TripStep> steps) { this.steps = steps; }

    public int getTotalBudget() { return totalBudget; }
    public void setTotalBudget(int totalBudget) { this.totalBudget = totalBudget; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public UserPreferences.EffortLevel getEffortLevel() { return effortLevel; }
    public void setEffortLevel(UserPreferences.EffortLevel effortLevel) { this.effortLevel = effortLevel; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
}
