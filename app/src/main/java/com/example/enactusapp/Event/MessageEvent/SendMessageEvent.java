package com.example.enactusapp.Event.MessageEvent;

public class SendMessageEvent {

    private final String message;

    public SendMessageEvent() {
        this.message = null;
    }

    public SendMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
