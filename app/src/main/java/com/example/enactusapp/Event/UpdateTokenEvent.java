package com.example.enactusapp.Event;

public class UpdateTokenEvent {

    private final String token;

    public UpdateTokenEvent(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
