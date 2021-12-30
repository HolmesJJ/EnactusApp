package com.example.enactusapp.Event.MessageEvent;

import com.example.enactusapp.Entity.User;

public class SendMessageEvent {

    private final String message;

    public SendMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
