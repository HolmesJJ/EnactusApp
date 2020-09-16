package com.example.enactusapp.Entity;

public class User {

    private int id;
    private String username;
    private String name;
    private String thumbnail;
    private String firebaseToken;
    private double longitude;
    private double latitude;

    public User(int id, String username, String name, String thumbnail, String firebaseToken, double longitude, double latitude) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.thumbnail = thumbnail;
        this.firebaseToken = firebaseToken;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getFirebaseToken() {
        return firebaseToken;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
