package com.example.enactusapp.Event.MessageEvent;

import com.example.enactusapp.Entity.User;

public class ReceiveMessageEvent {

    private final User user;
    private final String message;

    public ReceiveMessageEvent(String message) {
        this.user = null;
        this.message = message;
    }

    public ReceiveMessageEvent(User user, String message) {
        this.user = user;
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }
}
