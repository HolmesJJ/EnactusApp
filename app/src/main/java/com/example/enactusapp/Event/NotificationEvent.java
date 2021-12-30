package com.example.enactusapp.Event;

import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Entity.User;

public class NotificationEvent {

    private final User user;
    private final String message;
    private final MessageType type;

    public NotificationEvent(User user, String message, MessageType type) {
        this.user = user;
        this.message = message;
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getType() {
        return type;
    }
}
