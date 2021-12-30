package com.example.enactusapp.Event;

public class BlinkEvent {

    private final boolean isLeftEye;

    public BlinkEvent(boolean isLeftEye) {
        this.isLeftEye = isLeftEye;
    }

    public boolean isLeftEye() {
        return isLeftEye;
    }
}
