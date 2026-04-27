package com.traveling.app.travelpath.models;

import java.util.List;

public class UserPreferences {
    private String destination;
    private List<String> activities; // e.g., "Restauration", "Loisirs", "Découverte", "Culture", "Nature", "Shopping"
    private List<String> mandatoryPlaces;
    private int budgetMax;
    private int durationDays;
    private EffortLevel effortLevel; // "Faible", "Modéré", "Elevé"
    private WeatherPreference weatherPreference; // "Froid", "Chaleur", "Humidité"

    public enum EffortLevel { FAIBLE, MODERE, ELEVE }
    public enum WeatherPreference { FROID, CHALEUR, HUMIDITE }

    public UserPreferences() {}

    public UserPreferences(String destination, List<String> activities, List<String> mandatoryPlaces, int budgetMax, int durationDays, EffortLevel effortLevel, WeatherPreference weatherPreference) {
        this.destination = destination;
        this.activities = activities;
        this.mandatoryPlaces = mandatoryPlaces;
        this.budgetMax = budgetMax;
        this.durationDays = durationDays;
        this.effortLevel = effortLevel;
        this.weatherPreference = weatherPreference;
    }

    // Getters and Setters
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public List<String> getActivities() { return activities; }
    public void setActivities(List<String> activities) { this.activities = activities; }

    public List<String> getMandatoryPlaces() { return mandatoryPlaces; }
    public void setMandatoryPlaces(List<String> mandatoryPlaces) { this.mandatoryPlaces = mandatoryPlaces; }

    public int getBudgetMax() { return budgetMax; }
    public void setBudgetMax(int budgetMax) { this.budgetMax = budgetMax; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public EffortLevel getEffortLevel() { return effortLevel; }
    public void setEffortLevel(EffortLevel effortLevel) { this.effortLevel = effortLevel; }

    public WeatherPreference getWeatherPreference() { return weatherPreference; }
    public void setWeatherPreference(WeatherPreference weatherPreference) { this.weatherPreference = weatherPreference; }
}
