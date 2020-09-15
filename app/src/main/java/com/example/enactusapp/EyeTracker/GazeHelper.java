package com.example.enactusapp.EyeTracker;

import android.content.Context;
import android.view.TextureView;

import com.example.enactusapp.Constants.Constants;

import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.EyeMovementCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.ImageCallback;
import camp.visual.gazetracker.callback.StatusCallback;

public class GazeHelper implements InitializationCallback, GazeCallback, CalibrationCallback, EyeMovementCallback, ImageCallback, StatusCallback {

    private static final String TAG = "GazeHelper";

    private GazeDevice mGazeDevice;
    private GazeTracker mGazeTracker;
    private GazeListener mGazeListener;

    private GazeHelper() {
    }

    private static class SingleInstance {
        private static GazeHelper INSTANCE = new GazeHelper();
    }

    public static GazeHelper getInstance() {
        return SingleInstance.INSTANCE;
    }

    public void initGaze(Context context, GazeListener gazeListener) {
        this.mGazeDevice = new GazeDevice();
        this.mGazeListener = gazeListener;

        // 红米10X
        this.mGazeDevice.addDeviceInfo("M2004J7BC", -34f, -3.5f);
        // 华为Mate30Pro
        this.mGazeDevice.addDeviceInfo("LIO-AN00", -46f, -3.5f);
        GazeTracker.initGazeTracker(context, this.mGazeDevice, Constants.GAZE_LICENSE_KEY, this);
    }

    public void releaseGaze() {
        if (isGazeNonNull()) {
            this.mGazeTracker.removeCallbacks();
            GazeTracker.deinitGazeTracker(this.mGazeTracker);
            this.mGazeTracker = null;
        }
        this.mGazeListener = null;
        this.mGazeDevice = null;
    }

    public boolean startCalibration(int calibrationType) {
        boolean isSuccess = false;
        if (isGazeNonNull() && isTracking()) {
            isSuccess = this.mGazeTracker.startCalibration(calibrationType);
        }
        return isSuccess;
    }

    public void stopCalibration() {
        if (isGazeNonNull()) {
            this.mGazeTracker.stopCalibration();
        }
    }

    // 收集用于校准的样本
    public boolean startCollectSamples() {
        boolean isSuccess = false;
        if (isGazeNonNull()) {
            isSuccess = this.mGazeTracker.startCollectSamples();
        }
        return isSuccess;
    }

    public void startTracking() {
        if (isGazeNonNull()) {
            this.mGazeTracker.startTracking();
        }
    }

    public boolean isTracking() {
        if (isGazeNonNull()) {
            return this.mGazeTracker.isTracking();
        }
        return false;
    }

    public void stopTracking() {
        if (isGazeNonNull()) {
            this.mGazeTracker.stopTracking();
        }
    }

    // 默认30fps
    public boolean setTrackingFPS(int fps) {
        if (fps > 0 && fps <= 30) {
            return this.mGazeTracker.setTrackingFPS(fps);
        }
        return false;
    }

    public void setCameraPreview(TextureView preview) {
        if (isGazeNonNull()) {
            this.mGazeTracker.setCameraPreview(preview);
        }
    }

    public void removeCameraPreview() {
        if (isGazeNonNull()) {
            this.mGazeTracker.removeCameraPreview();
        }
    }

    public boolean isCurrentDeviceFound() {
        return this.mGazeDevice.isCurrentDeviceFound();
    }

    public GazeDevice.Info showCurrentDeviceInfo() {
        return this.mGazeDevice.getCurrentDeviceInfo();
    }

    public void showAvailableDevices() {
        GazeDevice.Info[] infos = this.mGazeDevice.getAvailableDevices();
        for (GazeDevice.Info info : infos) {
            System.out.println("Available Devices -- Model: " + info.modelName + ", x: " + info.screen_origin_x + ", y: " + info.screen_origin_y);
        }
    }

    public boolean isGazeNonNull() {
        return this.mGazeTracker != null;
    }

    // GazeInitCallback
    @Override
    public void onInitialized(GazeTracker gazeTracker, int error) {
        if (gazeTracker != null) {
            this.mGazeTracker = gazeTracker;
            this.mGazeTracker.setGazeCallback(this);
            this.mGazeTracker.setCalibrationCallback(this);
            this.mGazeTracker.setEyeMovementCallback(this);
            this.mGazeTracker.setImageCallback(this);
            this.mGazeTracker.setStatusCallback(this);
            this.mGazeListener.onGazeInitSuccess();
        } else {
            this.mGazeListener.onGazeInitFail(error);
        }
    }

    // GazeCoordCallback
    @Override
    public void onGaze(long timestamp, float x, float y, int state) {
        mGazeListener.onGazeCoord(timestamp, x, y, state);
    }

    @Override
    public void onFilteredGaze(long timestamp, float x, float y, int state) {
        mGazeListener.onFilteredGazeCoord(timestamp, x, y, state);
    }

    // GazeCalibrationCallback
    @Override
    public void onCalibrationProgress(float progress) {
        mGazeListener.onGazeCalibrationProgress(progress);
    }

    @Override
    public void onCalibrationNextPoint(final float x, final float y) {
        mGazeListener.onGazeCalibrationNextPoint(x, y);
    }

    @Override
    public void onCalibrationFinished() {
        mGazeListener.onGazeCalibrationFinished();
    }

    // GazeEyeMovementCallback
    @Override
    public void onEyeMovement(long timestamp, long duration, float x, float y, int state) {
        mGazeListener.onGazeEyeMovement(timestamp, duration, x, y, state);
    }

    // GazeImageCallback
    @Override
    public void onImage(long timestamp, byte[] image) {
        mGazeListener.onGazeImage(timestamp, image);
    }

    // GazeStatusCallback
    @Override
    public void onStarted() {
        mGazeListener.onGazeStarted();
    }

    @Override
    public void onStopped(int error) {
        mGazeListener.onGazeStopped(error);
    }
}
