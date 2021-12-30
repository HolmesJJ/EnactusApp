package com.example.enactusapp.Event;

public class BackCameraEvent {

    private final boolean isEnabled;

    public BackCameraEvent(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
