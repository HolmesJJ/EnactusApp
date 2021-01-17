package com.example.enactusapp.Event;

import com.example.enactusapp.Entity.User;

public class NotificationEvent {

    private User user;
    private String message;

    public NotificationEvent(User user, String message) {
        this.user = user;
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
