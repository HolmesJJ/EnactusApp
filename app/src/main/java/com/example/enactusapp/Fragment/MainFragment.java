package com.example.enactusapp.Fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ProgressBar;

import com.baidu.tts.client.SpeechError;
import com.example.enactusapp.Bluetooth.BluetoothHelper;
import com.example.enactusapp.Bluetooth.BluetoothHelper.OnReadDataListener;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Entity.GazePoint;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.BackCameraEvent;
import com.example.enactusapp.Event.BluetoothEvent;
import com.example.enactusapp.Event.CalibrationEvent;
import com.example.enactusapp.Event.GazePointEvent;
import com.example.enactusapp.Event.MessageEvent;
import com.example.enactusapp.Event.StartChatEvent;
import com.example.enactusapp.EyeTracker.CalibrationViewer;
import com.example.enactusapp.EyeTracker.GazeDevice;
import com.example.enactusapp.EyeTracker.GazeHelper;
import com.example.enactusapp.EyeTracker.GazeListener;
import com.example.enactusapp.EyeTracker.PointView;
import com.example.enactusapp.Fragment.Contact.ContactFragment;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.Notification.NotificationFragment;
import com.example.enactusapp.Fragment.ObjectDetection.ObjectDetectionFragment;
import com.example.enactusapp.Fragment.Profile.ProfileFragment;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.STT.Listener.STTListener;
import com.example.enactusapp.STT.RecogResult;
import com.example.enactusapp.STT.STTHelper;
import com.example.enactusapp.TTS.TTSHelper;
import com.example.enactusapp.TTS.Listener.TTSListener;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.UI.BottomBar;
import com.example.enactusapp.UI.BottomBarTab;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.GPSUtils;
import com.example.enactusapp.Utils.SimulateUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.hc.bluetoothlibrary.DeviceModule;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class MainFragment extends SupportFragment implements ViewTreeObserver.OnGlobalLayoutListener, GazeListener, TTSListener, STTListener, OnTaskCompleted, OnReadDataListener {

    private static final String TAG = "MainFragment";

    private static final int START_LOCATION_ACTIVITY = 99;
    private static final int UPDATE_TOKEN = 1;

    private static final int MIDDLE_TAB = 2;
    private static final int OBJECT_DETECTION_TAB = 3;

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;
    private static final int FIFTH = 4;

    private static final boolean IS_USE_GAZE_FILER = true;

    private SupportFragment[] mFragments = new SupportFragment[5];

    private TextureView mTvFrontCamera;
    private ProgressBar mPbGaze;
    private BottomBar mBottomBar;
    private PointView mPvPoint;
    private CalibrationViewer mVcCalibration;
    private Button btnStopCalibration;

    // 眼睛是在凝视或在移动
    private int fixationCounter = 0;
    private int currentEyeMovementState = EyeMovementState.FIXATION;

    private GazePoint mGazePoint;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread = new HandlerThread("background");
    private ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();

    private static CustomThreadPool sThreadPoolFirebase = new CustomThreadPool(Thread.NORM_PRIORITY);

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) (lhs.getWidth() * lhs.getHeight()) -
                    (long) (rhs.getWidth() * rhs.getHeight()));
        }
    }

    public static MainFragment newInstance() {
        Bundle args = new Bundle();
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        initHandler();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SupportFragment firstFragment = findFragment(ContactFragment.class);
        if (firstFragment == null) {
            mFragments[FIRST] = ContactFragment.newInstance();
            mFragments[SECOND] = DialogFragment.newInstance();
            mFragments[THIRD] = ObjectDetectionFragment.newInstance();
            mFragments[FOURTH] = ProfileFragment.newInstance();
            mFragments[FIFTH] = NotificationFragment.newInstance();

            loadMultipleRootFragment(R.id.fl_main_container, FIRST,
                    mFragments[FIRST],
                    mFragments[SECOND],
                    mFragments[THIRD],
                    mFragments[FOURTH],
                    mFragments[FIFTH]
            );
        } else {
            mFragments[FIRST] = firstFragment;
            mFragments[SECOND] = findFragment(DialogFragment.class);
            mFragments[THIRD] = findFragment(ObjectDetectionFragment.class);
            mFragments[FOURTH] = findFragment(ProfileFragment.class);
            mFragments[FIFTH] = findFragment(NotificationFragment.class);
        }
    }

    private void initView(View view) {

        mTvFrontCamera = (TextureView) view.findViewById(R.id.tv_front_camera);
        mPbGaze = (ProgressBar) view.findViewById(R.id.pb_gaze);
        mTvFrontCamera.getViewTreeObserver().addOnGlobalLayoutListener(this);
        mPvPoint = (PointView) view.findViewById(R.id.pv_point);
        mVcCalibration = (CalibrationViewer) view.findViewById(R.id.cv_calibration);
        btnStopCalibration = (Button) view.findViewById(R.id.btn_stop_calibration);
        btnStopCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GazeHelper.getInstance().stopCalibration();
                hideCalibrationView();
            }
        });

        mBottomBar = (BottomBar) view.findViewById(R.id.bottomBar);

        mBottomBar.addItem(new BottomBarTab(_mActivity, R.drawable.ic_contact, getString(R.string.contact)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_dialog, getString(R.string.dialog)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_mic, getString(R.string.speak)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_object_detection, getString(R.string.objectDetection)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_profile, getString(R.string.profile)));

        mBottomBar.setOnTabSelectedListener(new BottomBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position, int prePosition) {
                if (prePosition == OBJECT_DETECTION_TAB && position != OBJECT_DETECTION_TAB) {
                    EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(false));
                } else if (prePosition != OBJECT_DETECTION_TAB && position == OBJECT_DETECTION_TAB) {
                    EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(true));
                }
                if (position != MIDDLE_TAB) {
                    if (position > 2) {
                        position = position - 1;
                    }
                    if (prePosition > 2) {
                        prePosition = prePosition - 1;
                    }
                    showHideFragment(mFragments[position], mFragments[prePosition]);
                } else {
                    ToastUtils.showShortSafe("Start Speaking");
                    STTHelper.getInstance().setSpeaking(true);
                    STTHelper.getInstance().start();
                }
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {
                if (position == MIDDLE_TAB) {
                    ToastUtils.showShortSafe("Stop Speaking");
                    STTHelper.getInstance().stop();
                    STTHelper.getInstance().setSpeaking(false);
                }
            }
        });

        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mGreetingBroadcastReceiver, new IntentFilter(MessageType.GREETING.getValue()));
        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mNormalBroadcastReceiver, new IntentFilter(MessageType.NORMAL.getValue()));

        setOffsetOfView();
    }

    public GazePoint getGazePoint() {
        return mGazePoint;
    }

    private void setCalibrationPoint(final float x, final float y) {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnStopCalibration.setVisibility(View.VISIBLE);
                mVcCalibration.setVisibility(View.VISIBLE);
                mVcCalibration.changeDraw(true, null);
                mVcCalibration.setPointPosition(x, y);
                mVcCalibration.setPointAnimationPower(0);
            }
        });
    }

    private void setCalibrationProgress(final float progress) {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVcCalibration.setPointAnimationPower(progress);
            }
        });
    }

    private void hideCalibrationView() {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVcCalibration.setVisibility(View.GONE);
                btnStopCalibration.setVisibility(View.GONE);
            }
        });
    }

    // 注视坐标或校准坐标仅作为绝对坐标（即全屏屏幕）传输，但是Android视图的坐标系是相对坐标系，不考虑操作栏，状态栏和导航栏
    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(mPvPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                mPvPoint.setOffset(x, y);
                mVcCalibration.setOffset(x, y);
            }
        });
    }

    private void showGazePoint(final float x, final float y, final int type) {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPvPoint.setType(type == TrackingState.TRACKING ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                mPvPoint.setPosition(x, y);
            }
        });
    }

    private void initHandler() {
        backgroundThread.start();
        if (backgroundHandler == null) {
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void releaseHandler() {
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
            backgroundHandler = null;
        }
        backgroundThread.quitSafely();
    }

    public void showNotificationFragment() {
        showHideFragment(mFragments[FIFTH], mFragments[mBottomBar.getCurrentItemPosition() > 2 ? mBottomBar.getCurrentItemPosition() - 1 : mBottomBar.getCurrentItemPosition()]);
        mBottomBar.setVisibility(View.GONE);
    }

    public void hideNotificationFragment() {
        showHideFragment(mFragments[mBottomBar.getCurrentItemPosition() > 2 ? mBottomBar.getCurrentItemPosition() - 1 : mBottomBar.getCurrentItemPosition()], mFragments[FIFTH]);
        mBottomBar.setVisibility(View.VISIBLE);
    }

    private String convertToJSONUpdateToken(int userId, String firebaseToken) {
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("Id", userId);
            jsonMsg.put("FirebaseToken", firebaseToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONUpdateToken(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int code = jsonObject.getInt("code");
            String message = jsonObject.getString("message");
            if (code == 1) {

            } else {
                ToastUtils.showShortSafe(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        sThreadPoolFirebase.execute(() -> {
            // APP多次重启后token就失效，因此每次启动都直接删除旧的token，重新刷新新的token
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId();
            } catch (Exception e) {
                e.fillInStackTrace();
            }
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                ToastUtils.showShortSafe("FireBase Token Error!");
                                return;
                            }
                            try {
                                // Get new Instance ID token
                                String fireBaseToken = task.getResult().getToken();
                                Log.i(TAG, "fireBaseToken: " + fireBaseToken);
                                Config.setFirebaseToken(fireBaseToken);
                                HttpAsyncTaskPost updateTokenTask = new HttpAsyncTaskPost(MainFragment.this, UPDATE_TOKEN);
                                String jsonData = convertToJSONUpdateToken(Config.sUserId, fireBaseToken);
                                updateTokenTask.execute(Constants.IP_ADDRESS + "update_token.php", jsonData, null);
                            } catch (Exception e) {
                                ToastUtils.showShortSafe("FireBase Token Error!");
                            }
                        }
                    });
        });
        if (!GPSUtils.isOpenGPS(_mActivity)) {
            startLocation();
        }
        Log.i(TAG, "Gaze Version: " + GazeTracker.getVersionName());
        GazeHelper.getInstance().initGaze(_mActivity, this);
        TTSHelper.getInstance().initTTS(this);
        STTHelper.getInstance().initSTT(this);
        BluetoothHelper.getInstance().initBluetooth(_mActivity, this);
    }

    @Override
    public void onGlobalLayout() {
        mTvFrontCamera.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onGazeInitSuccess() {
        GazeHelper.getInstance().showAvailableDevices();
        GazeDevice.Info gazeDeviceInfo = GazeHelper.getInstance().showCurrentDeviceInfo();
        ToastUtils.showShortSafe(gazeDeviceInfo.modelName + " x: " + gazeDeviceInfo.screen_origin_x + ", y: " + gazeDeviceInfo.screen_origin_y + " " + GazeHelper.getInstance().isCurrentDeviceFound());
        if (mTvFrontCamera.isAvailable()) {
            GazeHelper.getInstance().setCameraPreview(mTvFrontCamera);
        }
        GazeHelper.getInstance().startTracking();
    }

    @Override
    public void onGazeInitFail(int error) {
        String err = "";
        if (error == InitializationErrorType.ERROR_CAMERA_PERMISSION) {
            err = "Gaze required permission not granted";
        } else if (error == InitializationErrorType.ERROR_AUTHENTICATE) {
            err = "Gaze authentication failed";
        } else  {
            err = "Init gaze library fail";
        }
        ToastUtils.showShortSafe(err);
    }

    @Override
    public void onGazeCoord(long timestamp, float x, float y, int state) {
        if (!IS_USE_GAZE_FILER) {
            if (state == TrackingState.TRACKING) {
                mGazePoint = new GazePoint(x, y);
                showGazePoint(x, y, state);
            } else {
                fixationCounter = 0;
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPbGaze.setProgress(0);
                    }
                });
            }
        }
    }

    @Override
    public void onFilteredGazeCoord(long timestamp, float x, float y, int state) {
        if (IS_USE_GAZE_FILER) {
            if (state == TrackingState.TRACKING) {
                Log.i(TAG, "showGazePoint: (" + x + "x" + y + ")");
                mGazePoint = new GazePoint(x, y);
                showGazePoint(x, y, state);
            } else {
                fixationCounter = 0;
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPbGaze.setProgress(0);
                    }
                });
            }
        }
    }

    @Override
    public void onGazeCalibrationProgress(float progress) {
        setCalibrationProgress(progress);
    }

    @Override
    public void onGazeCalibrationNextPoint(float x, float y) {
        setCalibrationPoint(x, y);
        // 设置好校准坐标后，等待1秒钟，收集样品，然后在眼睛找到坐标后进行校准
        backgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GazeHelper.getInstance().startCollectSamples();
            }
        }, 1000);
    }

    @Override
    public void onGazeCalibrationFinished() {
        hideCalibrationView();
        Config.setLastCalibratedTime(System.currentTimeMillis());
        Config.setIsCalibrated(true);
    }

    @Override
    public void onGazeEyeMovement(long timestamp, long duration, float x, float y, int state) {
        // Log.i(TAG, "check eyeMovement timestamp: " + timestamp + " (" + x + "x" + y + ") : " + type);
        if (state == EyeMovementState.FIXATION) {
            if (currentEyeMovementState == EyeMovementState.FIXATION) {
                fixationCounter++;
                if (fixationCounter <= 25) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPbGaze.setProgress(fixationCounter * 4);
                        }
                    });
                    if (fixationCounter == 25) {
                        SimulateUtils.simulateClick(_mActivity, (int)x, (int)y);
                        if (mBottomBar.getCurrentItemPosition() == 3 && getGazePoint() != null) {
                            EventBusActivityScope.getDefault(_mActivity).post(new GazePointEvent(getGazePoint()));
                        }
                    }
                } else {
                    fixationCounter = 0;
                }
            } else {
                currentEyeMovementState = EyeMovementState.FIXATION;
                fixationCounter = 0;
            }
        } else if (state == EyeMovementState.SACCADE) {
            if (currentEyeMovementState != EyeMovementState.SACCADE) {
                currentEyeMovementState = EyeMovementState.SACCADE;
                fixationCounter = 0;
            }
        } else {
            fixationCounter = 0;
        }
    }

    @Override
    public void onGazeImage(long timestamp, byte[] image) {
        // Log.i(TAG, "onGazeImage");
        // FileUtils.writeYuvToDisk(640, 480, 100, image, Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow" + File.separator + "GAZE.jpg");
    }

    @Override
    public void onGazeStarted() {
        Log.i(TAG, "onGazeStarted");
        GazeHelper.getInstance().startCalibration(CalibrationModeType.FIVE_POINT);
    }

    @Override
    public void onGazeStopped(int error) {
        Log.i(TAG, "onGazeStopped");
        if (error != StatusErrorType.ERROR_NONE) {
            switch (error) {
                case StatusErrorType.ERROR_CAMERA_START:
                    ToastUtils.showShortSafe("ERROR_CAMERA_START");
                    break;
                case StatusErrorType.ERROR_CAMERA_INTERRUPT:
                    ToastUtils.showShortSafe("ERROR_CAMERA_INTERRUPT");
                    break;
            }
        }
    }

    // TTS
    @Override
    public void onTTSInitSuccess() {
        Log.i(TAG, "初始化成功");
        ToastUtils.showShortSafe("onTTSInitSuccess");
    }

    @Override
    public void onTTSInitFailed() {
        Log.i(TAG, "初始化失败");
        ToastUtils.showShortSafe("onTTSInitFailed");
    }

    @Override
    public void onTTSSynthesizeStart(String utteranceId) {
        Log.i(TAG, "准备开始合成, 序列号: " + utteranceId);
    }

    @Override
    public void onTTSSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress) {
        Log.i(TAG, "合成进度回调, progress：" + progress + ";序列号:" + utteranceId);
    }

    @Override
    public void onTTSSynthesizeFinish(String utteranceId) {
        Log.i(TAG, "合成结束回调, 序列号:" + utteranceId);
    }

    @Override
    public void onTTSSpeechStart(String utteranceId) {
        Log.i(TAG, "播放开始回调, 序列号: " + utteranceId);
    }

    @Override
    public void onTTSSpeechProgressChanged(String utteranceId, int progress) {
        Log.i(TAG, "播放进度回调, progress: " + progress + "; 序列号: " + utteranceId);
    }

    @Override
    public void onTTSSpeechFinish(String utteranceId) {
        Log.i(TAG, "播放结束回调, 序列号: " + utteranceId);
    }

    @Override
    public void onTTSError(String utteranceId, SpeechError speechError) {
        Log.e(TAG, "检测到错误");
        ToastUtils.showShortSafe("错误发生: " + speechError.description + ", 错误编码: " + speechError.code + ", 序列号: " + utteranceId);
    }

    // STT
    // 引擎准备完毕
    @Override
    public void onSTTAsrReady() {
        Log.i(TAG, "onSTTAsrReady");
    }

    @Override
    public void onSTTAsrBegin() {
        Log.i(TAG, "onSTTAsrBegin");
    }

    @Override
    public void onSTTAsrEnd() {
        Log.i(TAG, "onSTTAsrEnd");
    }

    @Override
    public void onSTTAsrPartialResult(String[] results, RecogResult recogResult) {
        Log.i(TAG, "onSTTAsrPartialResult results: " + Arrays.toString(results) + ", recogResult: " + recogResult.toString());
    }

    @Override
    public void onSTTAsrOnlineNluResult(String nluResult) {
        Log.i(TAG, "onSTTAsrOnlineNluResult nluResult: " + nluResult);
    }

    @Override
    public void onSTTAsrFinalResult(String[] results, RecogResult recogResult) {
        Log.i(TAG, "onSTTAsrFinalResult results: " + Arrays.toString(results) + ", recogResult: " + recogResult.toString());
        EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent(null, results[0]));
        if (mBottomBar.getCurrentItemPosition() == 0) {
            showHideFragment(mFragments[1], mFragments[0]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 3) {
            showHideFragment(mFragments[1], mFragments[2]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 4) {
            showHideFragment(mFragments[1], mFragments[3]);
            mBottomBar.setCurrentItem(1);
        }
    }

    @Override
    public void onSTTAsrFinish(RecogResult recogResult) {
        Log.i(TAG, "onSTTAsrFinish recogResult: " + recogResult.toString());
    }

    @Override
    public void onSTTAsrFinishError(int errorCode, int subErrorCode, String descMessage, RecogResult recogResult) {
        Log.i(TAG, "onSTTAsrFinishError errorCode: "+ errorCode + ", subErrorCode: " + subErrorCode + ", " + descMessage +", recogResult: " + recogResult.toString());
    }

    @Override
    public void onSTTAsrLongFinish() {
        Log.i(TAG, "onSTTAsrLongFinish");
    }

    @Override
    public void onSTTAsrVolume(int volumePercent, int volume) {
        Log.i(TAG, "onSTTAsrVolume 音量百分比" + volumePercent + " ; 音量" + volume);
    }

    @Override
    public void onSTTAsrAudio(byte[] data, int offset, int length) {
        Log.i(TAG, "onSTTAsrAudio 音频数据回调, length:" + data.length);
    }

    // 结束识别
    @Override
    public void onSTTAsrExit() {
        Log.i(TAG, "onSTTAsrExit");
        if (STTHelper.getInstance().isSpeaking()) {
            mBottomBar.setCurrentItem(MIDDLE_TAB);
        }
    }

    @Override
    public void onSTTOfflineLoaded() {
        Log.i(TAG, "onSTTOfflineLoaded");
    }

    @Override
    public void onSTTOfflineUnLoaded() {
        Log.i(TAG, "onSTTOfflineUnLoaded");
    }

    // Bluetooth
    @Override
    public void readData(String mac, byte[] data) {
        Log.i(TAG, "Bluetooth readData mac: " + mac +", data: " + Arrays.toString(data));
        // A运动，B放松
        // [-23, -128, -102, -23, -127, -109, 49, -17, -68, -102, 66, 13, 10, -23, -128, -102, -23, -127, -109, 50, -17, -68, -102, 65, 13, 10]
        String channel1 = "B";
        String channel2 = "B";
        if (data[10] == 65) {
            channel1 = "A";
        }
        if (data[23] == 65) {
            channel2 = "A";
        }
        Log.i(TAG, "BluetoothEvent: channel1 " + channel1 +", channel2: " + channel2);
        EventBusActivityScope.getDefault(_mActivity).post(new BluetoothEvent(channel1, channel2, getCurrentItemPosition()));
    }

    @Override
    public void reading(boolean isStart) {
        Log.i(TAG, "Bluetooth reading isStart: " + isStart);
    }

    @Override
    public void connectSucceed() {
        Log.i(TAG, "Bluetooth connectSucceed");
    }

    @Override
    public void errorDisconnect(DeviceModule deviceModule) {
        Log.i(TAG, "Bluetooth errorDisconnect: ");
    }

    @Override
    public void readNumber(int number) {
        Log.i(TAG, "Bluetooth readNumber number: " + number);
    }

    @Override
    public void readLog(String className, String data, String lv) {
        Log.i(TAG, "Bluetooth readLog className: " + className + ", data: " + data + ", lv: " + lv);
    }

    @Override
    public void onTaskCompleted(String response, int requestId) {
        if (requestId == UPDATE_TOKEN) {
            retrieveFromJSONUpdateToken(response);
        }
    }

    public void startBrotherFragment(SupportFragment targetFragment) {
        start(targetFragment);
    }

    public int getCurrentItemPosition() {
        return mBottomBar.getCurrentItemPosition();
    }

    private BroadcastReceiver mGreetingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("id", -1);
            String username = intent.getStringExtra("username");
            String name = intent.getStringExtra("name");
            String firebaseToken = intent.getStringExtra("firebaseToken");
            double longitude = intent.getDoubleExtra("longitude", 9999);
            double latitude = intent.getDoubleExtra("latitude", 9999);
            String message = intent.getStringExtra("message");
            String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + id + ".jpg";
            showNotificationFragment();
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent(new User(id, username, name, thumbnail, firebaseToken, longitude, latitude), message));
        }
    };

    private BroadcastReceiver mNormalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("id", -1);
            String username = intent.getStringExtra("username");
            String name = intent.getStringExtra("name");
            String firebaseToken = intent.getStringExtra("firebaseToken");
            double longitude = intent.getDoubleExtra("longitude", 9999);
            double latitude = intent.getDoubleExtra("latitude", 9999);
            String message = intent.getStringExtra("message");
            String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + id + ".jpg";
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent(new User(id, username, name, thumbnail, firebaseToken, longitude, latitude), message));
            if (mBottomBar.getCurrentItemPosition() == 0) {
                showHideFragment(mFragments[1], mFragments[0]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 3) {
                showHideFragment(mFragments[1], mFragments[2]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 4) {
                showHideFragment(mFragments[1], mFragments[3]);
                mBottomBar.setCurrentItem(1);
            }
        }
    };

    //开启位置权限
    private void startLocation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(_mActivity);
        builder.setTitle("Tips")
                .setMessage("Please turn on your GPS")
                .setCancelable(false)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, START_LOCATION_ACTIVITY);
                    }
                }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,Intent data) {
        if (requestCode == START_LOCATION_ACTIVITY) {
            if (!GPSUtils.isOpenGPS(_mActivity)) {
                startLocation();
            }
        }
    }

    @Subscribe
    public void onStartChatEvent(StartChatEvent event) {
        EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent(event.getUser(), "Hi, " + Config.sName + ", How are you?"));
        hideNotificationFragment();
        if (mBottomBar.getCurrentItemPosition() == 0) {
            showHideFragment(mFragments[1], mFragments[0]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 3) {
            showHideFragment(mFragments[1], mFragments[2]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 4) {
            showHideFragment(mFragments[1], mFragments[3]);
            mBottomBar.setCurrentItem(1);
        }
    }

    @Subscribe
    public void onCalibrationEvent(CalibrationEvent event) {
        if (event.isStartCalibration()) {
            GazeHelper.getInstance().startCalibration(CalibrationModeType.FIVE_POINT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setOffsetOfView();
        GazeHelper.getInstance().setCameraPreview(mTvFrontCamera);
        GazeHelper.getInstance().startTracking();
    }

    @Override
    public void onPause() {
        GazeHelper.getInstance().stopTracking();
        GazeHelper.getInstance().removeCameraPreview();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        releaseHandler();
        if (viewLayoutChecker != null) {
            viewLayoutChecker.releaseChecker();
        }
        BluetoothHelper.getInstance().releaseBluetooth();
        STTHelper.getInstance().releaseSTT();
        TTSHelper.getInstance().releaseTTS();
        GazeHelper.getInstance().stopTracking();
        GazeHelper.getInstance().releaseGaze();
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
