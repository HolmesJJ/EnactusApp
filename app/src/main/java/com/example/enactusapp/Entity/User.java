package com.example.enactusapp.Entity;

public class User {

    private int id;
    private String username;
    private String name;
    private String thumbnail;
    private String firebaseToken;

    public User(int id, String username, String name, String thumbnail, String firebaseToken) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.thumbnail = thumbnail;
        this.firebaseToken = firebaseToken;
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
}
