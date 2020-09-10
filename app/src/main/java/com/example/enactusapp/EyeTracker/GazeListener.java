package com.example.enactusapp.EyeTracker;

public interface GazeListener {

    // GazeInitCallback
    void onGazeInitSuccess();
    void onGazeInitFail(int error);

    // GazeCoordCallback
    void onGazeCoord(long timestamp, float x, float y, int state);
    void onFilteredGazeCoord(long timestamp, float x, float y, int state);

    // GazeCalibrationCallback
    void onGazeCalibrationProgress(float progress);
    void onGazeCalibrationNextPoint(final float x, final float y);
    void onGazeCalibrationFinished();

    // GazeEyeMovementCallback
    void onGazeEyeMovement(long timestamp, long duration, float x, float y, int state);

    // GazeImageCallback
    void onGazeImage(long timestamp, byte[] image);

    // GazeStatusCallback
    void onGazeStarted();
    void onGazeStopped(int error);
}
