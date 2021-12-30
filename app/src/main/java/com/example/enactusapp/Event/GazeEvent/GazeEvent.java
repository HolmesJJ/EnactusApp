package com.example.enactusapp.Event.GazeEvent;

public class GazeEvent {

    private final boolean isStart;

    public GazeEvent(boolean isStart) {
        this.isStart = isStart;
    }

    public boolean isStart() {
        return isStart;
    }
}
