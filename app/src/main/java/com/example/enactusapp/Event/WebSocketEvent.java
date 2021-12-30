package com.example.enactusapp.Event;

public class WebSocketEvent {

    private String message;

    public WebSocketEvent() {
        this.message = "";
    }

    public WebSocketEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
