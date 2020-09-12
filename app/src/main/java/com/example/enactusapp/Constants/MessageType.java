package com.example.enactusapp.Constants;

public enum MessageType {
    GREETING("Greeting Message"),
    NORMAL("Normal Message");

    private String message;

    MessageType(String message) {
        this.message = message;
    }

    public String getValue() {
        return message;
    }
}
