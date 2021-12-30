package com.example.enactusapp.Entity;

import camp.visual.gazetracker.state.TrackingState;

public class GazePoint {

    private float gazePointX;
    private float gazePointY;
    private int state;

    public GazePoint() {
        this.gazePointX = -1;
        this.gazePointY = -1;
        this.state = TrackingState.OUT_OF_SCREEN;
    }

    public GazePoint(float gazePointX, float gazePointY, int state) {
        this.gazePointX = gazePointX;
        this.gazePointY = gazePointY;
        this.state = state;
    }

    public float getGazePointX() {
        return gazePointX;
    }

    public void setGazePointX(float gazePointX) {
        this.gazePointX = gazePointX;
    }

    public float getGazePointY() {
        return gazePointY;
    }

    public void setGazePointY(float gazePointY) {
        this.gazePointY = gazePointY;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
