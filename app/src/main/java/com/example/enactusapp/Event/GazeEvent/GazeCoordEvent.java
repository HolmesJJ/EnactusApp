package com.example.enactusapp.Event.GazeEvent;

import com.example.enactusapp.Entity.GazePoint;

public class GazeCoordEvent {

    private GazePoint mGazePoint;

    public GazeCoordEvent(GazePoint mGazePoint) {
        this.mGazePoint = mGazePoint;
    }

    public GazePoint getGazePoint() {
        return mGazePoint;
    }

    public void setGazePoint(GazePoint mGazePoint) {
        this.mGazePoint = new GazePoint(mGazePoint.getGazePointX(), mGazePoint.getGazePointY(), mGazePoint.getState());
    }
}
