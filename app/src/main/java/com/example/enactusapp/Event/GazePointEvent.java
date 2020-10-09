package com.example.enactusapp.Event;

import com.example.enactusapp.Entity.GazePoint;

public class GazePointEvent {

    private GazePoint mGazePoint;

    public GazePointEvent(GazePoint mGazePoint) {
        this.mGazePoint = mGazePoint;
    }

    public GazePoint getGazePoint() {
        return mGazePoint;
    }

    public void setGazePoint(GazePoint mGazePoint) {
        this.mGazePoint = mGazePoint;
    }
}
