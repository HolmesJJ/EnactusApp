package com.example.enactusapp.EyeTracker;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.locks.ReentrantLock;

import camp.visual.gazetracker.calibration.CalibrationHelper;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.EyeMovementCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.GazeTrackerCallback;
import camp.visual.gazetracker.callback.ImageCallback;
import camp.visual.gazetracker.callback.StatusCallback;
import camp.visual.gazetracker.debug.DebugLogger;
import camp.visual.gazetracker.device.DeviceMeta;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.util.ScreenSize;
import camp.visual.libgaze.Gaze;
import camp.visual.libgaze.callbacks.LibGazeCallback;
import camp.visual.libgaze.callbacks.LibGazeInitializationCallback;
import camp.visual.libgaze.camera.CameraImageCallback;
import camp.visual.libgaze.camera.CameraStatusCallback;

public class GazeTracker {
    private static final String TAG = GazeTracker.class.getSimpleName();
    private HandlerThread lifeThread = new HandlerThread("lifeThread");
    private Handler lifeHandler;
    private Context context;
    private InitializationCallback initializationCallback;
    private GazeCallback gazeCallback;
    private CalibrationCallback calibrationCallback;
    private EyeMovementCallback eyeMovementCallback;
    private ImageCallback imageCallback;
    private StatusCallback statusCallback;
    private OneEuroFilterManager oneEuroFilterManager;
    private Gaze gaze;
    private static final int RELEASE_NONE = 0;
    private static final int RELEASE_TRY = 1;
    private static final int RELEASE_DONE = 2;
    private int currentReleaseState = 0;
    private ReentrantLock lock = new ReentrantLock(true);
    @Keep
    private static final String VERSION_NAME = "2.0.0";
    private LibGazeInitializationCallback libGazeInitializationCallback = new LibGazeInitializationCallback() {
        public void onInitialize(Gaze gaze, int error) {
            if (GazeTracker.this.initializationCallback != null) {
                if (gaze != null) {
                    GazeTracker.this.gaze = gaze;
                    if (GazeTracker.this.imageCallback != null) {
                        gaze.setCameraImageCallback(GazeTracker.this.cameraBufferCallback);
                    }

                    gaze.setCameraStatusCallback(GazeTracker.this.cameraErrorCallback);
                    DebugLogger.i(GazeTracker.TAG, new Object[]{"init success"});
                    GazeTracker.this.initializationCallback.onInitialized(GazeTracker.this, 0);
                } else {
                    DebugLogger.i(GazeTracker.TAG, new Object[]{"init fail " + error});
                    int returnError = 0;
                    switch(error) {
                        case 1:
                            returnError = 1;
                            break;
                        case 2:
                            returnError = 2;
                            break;
                        case 3:
                            returnError = 3;
                    }

                    GazeTracker.this.initializationCallback.onInitialized((GazeTracker)null, returnError);
                    GazeTracker.this.release();
                }
            }

        }
    };
    long befTimestamp = -1L;
    private LibGazeCallback libGazeCallback = new LibGazeCallback() {
        public void onGaze(long timestamp, float x, float y, boolean isCalibrating) {
            if (GazeTracker.this.isAvailable()) {
                if (GazeTracker.this.befTimestamp == -1L) {
                    GazeTracker.this.befTimestamp = timestamp;
                }

                PointF screenPx = CalibrationHelper.modifyCamera2Screen(new PointF(x, y));
                int type = 0;
                DebugLogger.i(GazeTracker.TAG, new Object[]{"chk onGaze duration " + (timestamp - GazeTracker.this.befTimestamp) + " at " + timestamp});
                if (x != -1001.0F && y != -1001.0F) {
                    DebugLogger.i(GazeTracker.TAG, new Object[]{"chk onGaze " + x + "x" + y + " to " + screenPx.x + "x" + screenPx.y + ", isCalibrating " + isCalibrating});
                    ScreenSize screenSize = new ScreenSize();
                    screenSize.setScreenSize(GazeTracker.this.context);
                    if (0.0F > screenPx.x || 0.0F > screenPx.y || (float)screenSize.screenWidth < screenPx.x || (float)screenSize.screenHeight < screenPx.y) {
                        type = 3;
                    }

                    if (isCalibrating) {
                        type = 1;
                    }

                    if (GazeTracker.this.gazeCallback != null) {
                        GazeTracker.this.gazeCallback.onGaze(timestamp, screenPx.x, screenPx.y, type);
                        if (GazeTracker.this.oneEuroFilterManager.filterPoint(timestamp, screenPx.x, screenPx.y)) {
                            PointF filteredPx = GazeTracker.this.oneEuroFilterManager.getFilteredPoint();
                            if (Float.isNaN(filteredPx.x) && Float.isNaN(filteredPx.y)) {
                                GazeTracker.this.oneEuroFilterManager = new OneEuroFilterManager(30.0F);
                                DebugLogger.w(GazeTracker.TAG, new Object[]{"chk filter nan scr " + screenPx + " reinit oneEuro Filter"});
                            } else {
                                DebugLogger.i(GazeTracker.TAG, new Object[]{"chk filter okay scr " + screenPx + ", filter " + filteredPx});
                                GazeTracker.this.gazeCallback.onFilteredGaze(timestamp, filteredPx.x, filteredPx.y, type);
                            }
                        }
                    }

                    GazeTracker.this.befTimestamp = timestamp;
                } else {
                    type = 2;
                    screenPx.x = 0.0F;
                    screenPx.y = 0.0F;
                    if (GazeTracker.this.gazeCallback != null) {
                        GazeTracker.this.gazeCallback.onGaze(timestamp, screenPx.x, screenPx.y, type);
                        GazeTracker.this.gazeCallback.onFilteredGaze(timestamp, screenPx.x, screenPx.y, type);
                    }
                }

            }
        }

        public void onEyeMovement(long timestamp, long duration, float x, float y, int state) {
            if (GazeTracker.this.eyeMovementCallback != null) {
                PointF screenPx = CalibrationHelper.modifyCamera2Screen(new PointF(x, y));
                GazeTracker.this.eyeMovementCallback.onEyeMovement(timestamp, duration, screenPx.x, screenPx.y, state);
            }

        }

        public void onCalibrationProgress(float progress) {
            DebugLogger.d(GazeTracker.TAG, new Object[]{"onCalibrationProgress"});
            if (GazeTracker.this.calibrationCallback != null) {
                GazeTracker.this.calibrationCallback.onCalibrationProgress(progress);
            }

        }

        public void onCalibrationNextPoint(float x, float y) {
            DebugLogger.d(GazeTracker.TAG, new Object[]{"onCalibrationNextPoint"});
            if (GazeTracker.this.calibrationCallback != null) {
                PointF px = CalibrationHelper.modifyCamera2Screen(new PointF(x, y));
                GazeTracker.this.calibrationCallback.onCalibrationNextPoint(px.x, px.y);
            }

        }

        public void onCalibrationFinished() {
            DebugLogger.i(GazeTracker.TAG, new Object[]{"onCalibrationFinished"});
            if (GazeTracker.this.calibrationCallback != null) {
                GazeTracker.this.calibrationCallback.onCalibrationFinished();
            }

        }
    };
    private CameraStatusCallback cameraErrorCallback = new CameraStatusCallback() {
        public void onCameraOpen() {
            DebugLogger.i(GazeTracker.TAG, new Object[]{"onCameraOpened"});
            if (GazeTracker.this.statusCallback != null) {
                GazeTracker.this.statusCallback.onStarted();
            }

        }

        public void onCameraClose() {
            DebugLogger.i(GazeTracker.TAG, new Object[]{"onCameraClose"});
            if (GazeTracker.this.statusCallback != null) {
                GazeTracker.this.statusCallback.onStopped(0);
            }

        }

        public void onCameraError() {
            DebugLogger.i(GazeTracker.TAG, new Object[]{"onCameraError"});
            if (GazeTracker.this.statusCallback != null) {
                GazeTracker.this.statusCallback.onStopped(1);
            }

        }

        public void onCameraDisconnect() {
            DebugLogger.i(GazeTracker.TAG, new Object[]{"onCameraDisconnect"});
            if (GazeTracker.this.statusCallback != null) {
                GazeTracker.this.statusCallback.onStopped(2);
            }

        }
    };
    private CameraImageCallback cameraBufferCallback = new CameraImageCallback() {
        public void onImage(long timestamp, byte[] buffer) {
            if (GazeTracker.this.imageCallback != null) {
                GazeTracker.this.imageCallback.onImage(timestamp, buffer);
            }

        }
    };

    public static String getVersionName() {
        return "2.0.0";
    }

    public static void initGazeTracker(Context context, GazeDevice gazeDevice, String licenseKey, @NonNull InitializationCallback callback) {
        new GazeTracker(context, gazeDevice, licenseKey, callback);
    }

    public static void deinitGazeTracker(GazeTracker trueGaze) {
        trueGaze.release();
    }

    private GazeTracker(@NonNull Context context, @NonNull GazeDevice gazeDevice, @Nullable final String licenseKey, InitializationCallback initializationCallback) {
        this.context = context;
        GazeDevice.Info info = gazeDevice.getCurrentDeviceInfo();
        DeviceMeta deviceMeta = new DeviceMeta(context, info.screen_origin_x, info.screen_origin_y);
        CalibrationHelper.init(context, deviceMeta);
        this.setInitializationCallback(initializationCallback);
        this.lifeThread.start();
        this.lifeHandler = new Handler(this.lifeThread.getLooper());
        this.lifeHandler.post(new Runnable() {
            public void run() {
                GazeTracker.this.initGaze(licenseKey);
            }
        });
        this.oneEuroFilterManager = new OneEuroFilterManager(30.0F);
    }

    private void initGaze(String licenseKey) {
        Gaze.initGaze(this.context, this.libGazeInitializationCallback, this.libGazeCallback, (TextureView)null, licenseKey);
    }

    private boolean isInitialized() {
        return this.gaze != null;
    }

    private boolean isAvailable() {
        if (this.isInitialized() && this.currentReleaseState == 0) {
            return true;
        } else if (this.currentReleaseState == 2) {
            throw new IllegalStateException("gazeTracker Released!!");
        } else {
            return false;
        }
    }

    public void setCallbacks(GazeTrackerCallback... callbacks) {
        if (this.isAvailable()) {
            GazeTrackerCallback[] var2 = callbacks;
            int var3 = callbacks.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                GazeTrackerCallback callback = var2[var4];
                if (callback instanceof GazeCallback) {
                    this.gazeCallback = (GazeCallback)callback;
                } else if (callback instanceof CalibrationCallback) {
                    this.calibrationCallback = (CalibrationCallback)callback;
                } else if (callback instanceof EyeMovementCallback) {
                    this.eyeMovementCallback = (EyeMovementCallback)callback;
                } else if (callback instanceof ImageCallback) {
                    this.imageCallback = (ImageCallback)callback;
                } else if (callback instanceof StatusCallback) {
                    this.statusCallback = (StatusCallback)callback;
                }
            }

        }
    }

    private void setInitializationCallback(InitializationCallback initializationCallback) {
        this.initializationCallback = initializationCallback;
    }

    public void setCalibrationCallback(CalibrationCallback calibrationCallback) {
        if (this.isAvailable()) {
            this.calibrationCallback = calibrationCallback;
        }
    }

    public void setGazeCallback(GazeCallback gazeCallback) {
        if (this.isAvailable()) {
            this.gazeCallback = gazeCallback;
        }
    }

    public void setEyeMovementCallback(EyeMovementCallback eyeMovementCallback) {
        if (this.isAvailable()) {
            this.eyeMovementCallback = eyeMovementCallback;
        }
    }

    public void setImageCallback(ImageCallback imageCallback) {
        if (this.isAvailable()) {
            this.gaze.setCameraImageCallback(this.cameraBufferCallback);
            this.imageCallback = imageCallback;
        }
    }

    public void setStatusCallback(StatusCallback statusCallback) {
        if (this.isAvailable()) {
            this.statusCallback = statusCallback;
        }
    }

    private void removeInitializeCallback() {
        this.initializationCallback = null;
    }

    public void removeGazeCallback() {
        if (this.isAvailable()) {
            this.gazeCallback = null;
        }
    }

    public void removeCalibrationCallback() {
        if (this.isAvailable()) {
            this.calibrationCallback = null;
        }
    }

    public void removeEyeMovementCallback() {
        if (this.isAvailable()) {
            this.eyeMovementCallback = null;
        }
    }

    public void removeImageCallback() {
        if (this.isAvailable()) {
            this.imageCallback = null;
            this.gaze.removeCameraImageCallback();
        }
    }

    public void removeStatusCallback() {
        if (this.isAvailable()) {
            this.statusCallback = null;
        }
    }

    public void removeCallbacks() {
        if (this.isAvailable()) {
            this.removeInitializeCallback();
            this.removeCalibrationCallback();
            this.removeGazeCallback();
            this.removeImageCallback();
            this.removeEyeMovementCallback();
        }
    }

    public void startTracking() {
        if (this.isAvailable()) {
            this.gaze.startTracking();
        }
    }

    public void stopTracking() {
        if (this.isAvailable()) {
            this.gaze.stopTracking();
        }
    }

    public boolean isTracking() {
        return !this.isAvailable() ? false : this.gaze.isTracking();
    }

    public boolean setTrackingFPS(int fps) {
        return !this.isAvailable() ? false : this.gaze.setTrackingFPS(fps);
    }

    public boolean startCalibration(int mode, float left, float top, float right, float bottom) {
        if (!this.isAvailable()) {
            return false;
        } else if (!this.setCalibrationScreenRegion(left, top, right, bottom)) {
            return false;
        } else if (mode != 0 && mode != 5) {
            return mode == 1 ? this.gaze.startOnePointCalibration() : false;
        } else {
            return this.gaze.startCalibration();
        }
    }

    public boolean startCalibration(int mode) {
        RectF screenRegion = CalibrationHelper.getWholeScreenRegion();
        return this.startCalibration(mode, screenRegion.left, screenRegion.top, screenRegion.right, screenRegion.bottom);
    }

    public boolean startCalibration(float left, float top, float right, float bottom) {
        return this.startCalibration(0, left, top, right, bottom);
    }

    public boolean startCalibration() {
        return this.startCalibration(0);
    }

    public boolean startCollectSamples() {
        return !this.isAvailable() ? false : this.gaze.startCollectSamples();
    }

    public void stopCalibration() {
        if (this.isAvailable()) {
            this.gaze.stopCalibration();
        }
    }

    private boolean setCalibrationScreenRegion(float min_x, float min_y, float max_x, float max_y) {
        if (!this.isAvailable()) {
            return false;
        } else {
            return !CalibrationHelper.isRegionInWholeScreen(min_x, min_y, max_x, max_y) ? false : this.setCalibrationCameraRegion(min_x, min_y, max_x, max_y);
        }
    }

    private boolean setCalibrationCameraRegion(float min_x, float min_y, float max_x, float max_y) {
        RectF region = CalibrationHelper.modifyScreen2Camera(min_x, min_y, max_x, max_y);
        return this.gaze.setCalibrationRegion(region.left, region.top, region.right, region.bottom);
    }

    public boolean setCameraPreview(TextureView cameraPreview) {
        return !this.isAvailable() ? false : this.gaze.setCameraPreview(cameraPreview);
    }

    public void removeCameraPreview() {
        if (this.isAvailable()) {
            this.gaze.removeCameraPreview();
        }
    }

    private void releaseTrueGaze() {
        if (this.isInitialized() && Gaze.deinitGaze(this.gaze)) {
            this.gaze = null;
        }

        this.removeCallbacks();
        CalibrationHelper.release();
        this.lifeThread.quitSafely();
        this.currentReleaseState = 2;
    }

    private void release() {
        this.lock.lock();

        try {
            if (this.currentReleaseState != 0) {
                return;
            }

            this.currentReleaseState = 1;
            this.lifeHandler.post(new Runnable() {
                public void run() {
                    GazeTracker.this.releaseTrueGaze();
                }
            });
        } finally {
            this.lock.unlock();
        }

    }
}
