package com.example.enactusapp.Event;

import com.example.enactusapp.Entity.GazePoint;

public class SelectObjectEvent {

    private final GazePoint mGazePoint;

    public SelectObjectEvent(GazePoint mGazePoint) {
        this.mGazePoint = mGazePoint;
    }

    public GazePoint getGazePoint() {
        return mGazePoint;
    }
}
