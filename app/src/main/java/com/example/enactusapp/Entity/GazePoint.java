package com.example.enactusapp.Entity;

public class GazePoint {

    private float gazePointX;
    private float gazePointY;

    public GazePoint(float gazePointX, float gazePointY) {
        this.gazePointX = gazePointX;
        this.gazePointY = gazePointY;
    }

    public float getGazePointX() {
        return gazePointX;
    }

    public float getGazePointY() {
        return gazePointY;
    }
}
