package com.example.enactusapp.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.baidu.tts.client.SpeechError;
import com.example.enactusapp.Bluetooth.BluetoothHelper;
import com.example.enactusapp.Bluetooth.BluetoothHelper.OnReadDataListener;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Entity.Coordinate;
import com.example.enactusapp.Entity.GazePoint;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.BackCameraEvent;
import com.example.enactusapp.Event.BluetoothEvent;
import com.example.enactusapp.Event.GazeEvent.GazeCoordEvent;
import com.example.enactusapp.Event.GazeEvent.GazeEvent;
import com.example.enactusapp.Event.GazeEvent.GazeEyeMovementEvent;
import com.example.enactusapp.Event.MessageEvent.ReceiveMessageEvent;
import com.example.enactusapp.Event.NotificationEvent;
import com.example.enactusapp.Event.SelectObjectEvent;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Event.UpdateTokenEvent;
import com.example.enactusapp.Event.WebSocketEvent;
import com.example.enactusapp.EyeTracker.PointView;
import com.example.enactusapp.Fragment.Contact.ContactFragment;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.Notification.NotificationFragment;
import com.example.enactusapp.Fragment.ObjectDetection.ObjectDetectionFragment;
import com.example.enactusapp.Fragment.Profile.ProfileFragment;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.Markov.Listener.MarkovListener;
import com.example.enactusapp.Markov.MarkovHelper;
import com.example.enactusapp.R;
import com.example.enactusapp.STT.Listener.STTListener;
import com.example.enactusapp.STT.RecogResult;
import com.example.enactusapp.STT.STTHelper;
import com.example.enactusapp.Service.GazeService;
import com.example.enactusapp.Service.WebSocketService;
import com.example.enactusapp.TTS.TTSHelper;
import com.example.enactusapp.TTS.Listener.TTSListener;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.UI.BottomBar;
import com.example.enactusapp.UI.BottomBarTab;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.AppUtils;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.ScreenUtils;
import com.example.enactusapp.Utils.SimulateUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.hc.bluetoothlibrary.DeviceModule;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;
import pl.droidsonroids.gif.GifImageView;

public class MainFragment extends SupportFragment implements TTSListener, STTListener, OnTaskCompleted, OnReadDataListener, MarkovListener {

    private static final String TAG = MainFragment.class.getSimpleName();

    private static final int UPDATE_TOKEN = 1;

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int THIRD = 2;
    private static final int TOTAL_TABS = 3;

    private static final int CONTACT_TAB = FIRST;
    private static final int OBJECT_DETECTION_TAB = SECOND;
    private static final int SPEAK_TAB = 2;
    private static final int GAZE_TAB = 3;
    private static final int PROFILE_TAB = THIRD + 2;

    private static final int EYE_CONTROL_ID = 1;
    private static final int GREETING_ID = 2;
    private static final int QNA_ID = 3;
    private static final int MUSCLE_CONTROL_ID = 4;

    private static final int MUSCLE_CONTROL_LEFT_ID = 1;
    private static final int MUSCLE_CONTROL_RIGHT_ID = 2;
    private static final int MUSCLE_CONTROL_BOTH_ID = 3;

    private final SupportFragment[] mFragments = new SupportFragment[TOTAL_TABS];

    // 当前tab的位置
    private int curPosition = 0;
    // 肌肉控制tab的位置
    private int muscleControlPosition = 0;
    // 前一个FRAGMENT的位置（不包括MIDDLE_TAB和GAZE_TAB）
    private int preFragmentPosition = 0;

    // 凝视点
    private final GazeCoordEvent mGazeCoordEvent = new GazeCoordEvent(new GazePoint());
    private final GazeEyeMovementEvent mGazeEyeMovementEvent = new GazeEyeMovementEvent(new GazePoint());
    // 眼睛是在凝视或在移动
    private int fixationCounter = 0;
    private int preEyeMovementState = EyeMovementState.FIXATION;

    // Web Socket数据
    private final WebSocketEvent mWebSocketEvent = new WebSocketEvent();
    private Coordinate preCoordinate = new Coordinate();

    private static final CustomThreadPool sThreadPoolFirebase = new CustomThreadPool(Thread.NORM_PRIORITY);
    private static final CustomThreadPool sThreadPoolLoadDataSets = new CustomThreadPool(Thread.NORM_PRIORITY);

    private Handler backgroundHandler;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private ProgressBar mPbGaze;
    private PointView mPvPoint;
    private BottomBar mBottomBar;
    private GifImageView mGivLoading;

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(((long) lhs.getWidth() * lhs.getHeight()) -
                    ((long) rhs.getWidth() * rhs.getHeight()));
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
        EventBus.getDefault().register(this);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SupportFragment firstFragment = findFragment(ContactFragment.class);
        if (firstFragment == null) {
            mFragments[FIRST] = ContactFragment.newInstance();
            mFragments[SECOND] = ObjectDetectionFragment.newInstance();
            mFragments[THIRD] = ProfileFragment.newInstance();

            loadMultipleRootFragment(R.id.fl_main_container, FIRST,
                    mFragments[FIRST],
                    mFragments[SECOND],
                    mFragments[THIRD]
            );
        } else {
            mFragments[FIRST] = firstFragment;
            mFragments[SECOND] = findFragment(ObjectDetectionFragment.class);
            mFragments[THIRD] = findFragment(ProfileFragment.class);
        }
    }

    private void initView(View view) {
        mGivLoading = (GifImageView) view.findViewById(R.id.giv_loading);
        mPbGaze = (ProgressBar) view.findViewById(R.id.pb_gaze);
        mPvPoint = (PointView) view.findViewById(R.id.pv_point);
        mPvPoint.setPosition(-1, -1);
        mBottomBar = (BottomBar) view.findViewById(R.id.bottomBar);
        mBottomBar.addItem(new BottomBarTab(ContextUtils.getContext(), R.drawable.ic_contact, getString(R.string.contact)))
                .addItem(new BottomBarTab(ContextUtils.getContext(), R.drawable.ic_object_detection, getString(R.string.objectDetection)))
                .addItem(new BottomBarTab(ContextUtils.getContext(), R.drawable.ic_mic, getString(R.string.speak)))
                .addItem(new BottomBarTab(ContextUtils.getContext(), R.drawable.ic_gaze, getString(R.string.gaze)))
                .addItem(new BottomBarTab(ContextUtils.getContext(), R.drawable.ic_profile, getString(R.string.profile)));
        mBottomBar.setSpeakTabPositions(SPEAK_TAB);
        mBottomBar.setGazeTabPositions(GAZE_TAB);
        mBottomBar.setOnTabSelectedListener(new BottomBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position, int prePosition) {
                Log.i(TAG, "Current Position: " + position + ", Previous Position: " + prePosition);
                curPosition = position;
                moveTab(curPosition);
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {
                Log.i(TAG, "Current Position: " + position);
                curPosition = position;
                if (position == SPEAK_TAB) {
                    ToastUtils.showShortSafe("Stop Speaking");
                    STTHelper.getInstance().stop();
                    STTHelper.getInstance().setSpeaking(false);
                }
            }
        });
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        sThreadPoolFirebase.execute(() -> {
            // APP多次重启后token就失效，因此每次启动都直接删除旧的token，重新刷新新的token
            // FirebaseMessaging.getInstance().deleteToken();
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String token) {
                            // Get new Instance ID token
                            Log.i(TAG, "Firebase Token: " + token);
                            Config.setFirebaseToken(token);
                            HttpAsyncTaskPost updateTokenTask = new HttpAsyncTaskPost(MainFragment.this, UPDATE_TOKEN);
                            String jsonData = convertToJSONUpdateToken(Config.sUserId, token);
                            updateTokenTask.execute(Constants.IP_ADDRESS + "api/Account/EditFirebaseToken", jsonData, null);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            ToastUtils.showShortSafe("FireBase Token Error: " + e.getMessage());
                        }
                    });
        });
        TTSHelper.getInstance().initTTS(this);
        STTHelper.getInstance().initSTT(this);
        if (!MarkovHelper.getInstance().isInitialized()) {
            MarkovHelper.getInstance().initMarkov(ContextUtils.getContext());
        }
        if (!MarkovHelper.getInstance().isDataSetsLoaded()) {
            mGivLoading.setVisibility(View.VISIBLE);
            sThreadPoolLoadDataSets.execute(() -> {
                MarkovHelper.getInstance().addMarkovListener(this);
                MarkovHelper.getInstance().loadDataSets();
            });
        }
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            if (Config.sUseMode == SpUtilValueConstants.DEFAULT_MODE) {
                startGazeService();
            } if (Config.sUseMode == SpUtilValueConstants.SOCKET_MODE) {
                startWebSocketService();
            }
        } else if (Config.sUseMode == SpUtilValueConstants.MUSCLE_CONTROL_MODE) {
            BluetoothHelper.getInstance().initBluetooth(ContextUtils.getContext(), this);
        }
    }

    public void startGazeService() {
        Intent serviceIntent = new Intent(ContextUtils.getContext(), GazeService.class);
        serviceIntent.setAction(Constants.GAZE_SERVICE_START);
        ContextUtils.getContext().startForegroundService(serviceIntent);
    }

    public void stopGazeService() {
        Intent serviceIntent = new Intent(ContextUtils.getContext(), GazeService.class);
        serviceIntent.setAction(Constants.GAZE_SERVICE_STOP);
        ContextUtils.getContext().stopService(serviceIntent);
    }

    public void startWebSocketService() {
        Intent serviceIntent = new Intent(ContextUtils.getContext(), WebSocketService.class);
        serviceIntent.setAction(Constants.WEB_SOCKET_SERVICE_START);
        ContextUtils.getContext().startForegroundService(serviceIntent);
    }

    public void stopWebSocketService() {
        Intent serviceIntent = new Intent(ContextUtils.getContext(), WebSocketService.class);
        serviceIntent.setAction(Constants.WEB_SOCKET_SERVICE_STOP);
        ContextUtils.getContext().stopService(serviceIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new GazeEvent(false));
    }

    @Subscribe
    public void onUpdateTokenEvent(UpdateTokenEvent updateTokenEvent) {
        // Get new Instance ID token
        String firebaseToken = updateTokenEvent.getToken();
        Log.i(TAG, "Firebase Token: " + firebaseToken);
        Config.setFirebaseToken(firebaseToken);
        HttpAsyncTaskPost updateTokenTask = new HttpAsyncTaskPost(MainFragment.this, UPDATE_TOKEN);
        String jsonData = convertToJSONUpdateToken(Config.sUserId, firebaseToken);
        updateTokenTask.execute(Constants.IP_ADDRESS + "api/Account/EditFirebaseToken", jsonData, null);
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

    @Subscribe
    public void onGazeCoordEvent(GazeCoordEvent gazeCoordEvent) {
        if (!(getTopFragment() instanceof MainFragment)) {
            return;
        }
        synchronized (mGazeCoordEvent) {
            mGazeCoordEvent.setGazePoint(gazeCoordEvent.getGazePoint());
            int state = mGazeCoordEvent.getGazePoint().getState();
            if (state == TrackingState.TRACKING) {
                showGazePoint(mGazeCoordEvent.getGazePoint());
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

    private void showGazePoint(GazePoint gazePoint) {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPvPoint.setType(gazePoint.getState() == TrackingState.TRACKING ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                mPvPoint.setPosition(gazePoint.getGazePointX(), gazePoint.getGazePointY());
            }
        });
    }

    @Subscribe
    public void onGazeEyeMovementEvent(GazeEyeMovementEvent gazeEyeMovementEvent) {
        if (!(getTopFragment() instanceof MainFragment)) {
            return;
        }
        synchronized (mGazeEyeMovementEvent) {
            mGazeEyeMovementEvent.setGazePoint(gazeEyeMovementEvent.getGazePoint());
            countFixation(mGazeEyeMovementEvent);
        }
    }

    @Subscribe
    public void onWebSocketEvent(WebSocketEvent webSocketEvent) {
        if (!(getTopFragment() instanceof MainFragment)) {
            return;
        }
        synchronized (mWebSocketEvent) {
            mWebSocketEvent.setMessage(webSocketEvent.getMessage());
            try {
                JSONObject messageJSON = new JSONObject(mWebSocketEvent.getMessage());
                int id = messageJSON.getInt("id");
                if (id == EYE_CONTROL_ID) {
                    JSONObject contentJSON = messageJSON.getJSONObject("coordinate");
                    int x = contentJSON.getInt("x");
                    int y = contentJSON.getInt("y");
                    int height = contentJSON.getInt("height");
                    int width = contentJSON.getInt("width");
                    int newX = (int) (x / (width * 1.0) * ScreenUtils.getScreenRealWidth(ContextUtils.getContext()));
                    int newY = (int) (y / (height * 1.0) * ScreenUtils.getScreenRealHeight(ContextUtils.getContext()));
                    mPvPoint.setPosition(newX, newY);
                    Coordinate coordinate = new Coordinate(newX, newY);
                    countFixation(coordinate);
                } else if (id == GREETING_ID) {
                    String thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + 2 + ".jpg";
                    User user = new User(2, "C1234567D", "Zhang Zhiyao", thumbnail, Constants.UNSPECIFIED_FIREBASE_TOKEN, 103.81, 1.272);
                    EventBus.getDefault().post(new NotificationEvent(user, "Zhang Zhiyao says hello to you", MessageType.GREETING));
                } else if (id == QNA_ID) {
                    JSONObject contentJSON = messageJSON.getJSONObject("content");
                    String qna = contentJSON.getString("qna");
                    startBrotherFragment(DialogFragment.newInstance(2, Constants.UNSPECIFIED_FIREBASE_TOKEN, qna, false));
                } else if (id == MUSCLE_CONTROL_ID) {
                    JSONObject contentJSON = messageJSON.getJSONObject("content");
                    int action = contentJSON.getInt("muscle");
                    if (action == MUSCLE_CONTROL_LEFT_ID) {
                        EventBusActivityScope.getDefault(_mActivity).post(new MuscleControlLeftEvents(muscleControlPosition));
                    } else if (action == MUSCLE_CONTROL_RIGHT_ID) {
                        EventBusActivityScope.getDefault(_mActivity).post(new MuscleControlRightEvents(muscleControlPosition));
                    } else {
                        if (curPosition < PROFILE_TAB) {
                            curPosition++;
                            // 肌肉控制还不支持GAZE功能
                            if (curPosition == GAZE_TAB) {
                                curPosition++;
                            }
                        }
                        if (curPosition > PROFILE_TAB) {
                            curPosition = 0;
                        }
                        moveTab(curPosition);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void moveTab(int curPosition) {
        if (curPosition == CONTACT_TAB || curPosition == OBJECT_DETECTION_TAB || curPosition == PROFILE_TAB) {
            if (curPosition == PROFILE_TAB) {
                curPosition = THIRD;
            }
            if (preFragmentPosition == OBJECT_DETECTION_TAB && curPosition != OBJECT_DETECTION_TAB) {
                EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(false));
            } else if (preFragmentPosition != OBJECT_DETECTION_TAB && curPosition == OBJECT_DETECTION_TAB) {
                EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(true));
            }
            showHideFragment(mFragments[curPosition], mFragments[preFragmentPosition]);
            // 记录当前Fragment的位置
            preFragmentPosition = curPosition;
            muscleControlPosition = curPosition;
        } else {
            mBottomBar.setCurrentItem(CONTACT_TAB);
            if (curPosition == SPEAK_TAB) {
                ToastUtils.showShortSafe("Start Speaking");
                STTHelper.getInstance().setSpeaking(true);
                STTHelper.getInstance().start();
            }
            if (curPosition == GAZE_TAB) {
                EventBus.getDefault().post(new GazeEvent(true));
                AppUtils.hideApp(ContextUtils.getContext());
            }
        }
    }

    private void countFixation(GazeEyeMovementEvent gazeEyeMovementEvent) {
        int state = gazeEyeMovementEvent.getGazePoint().getState();
        if (state == EyeMovementState.FIXATION) {
            if (preEyeMovementState == EyeMovementState.FIXATION) {
                fixationCounter++;
                if (fixationCounter <= 25) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPbGaze.setProgress(fixationCounter * 4);
                        }
                    });
                    if (fixationCounter == 25) {
                        int x = (int) gazeEyeMovementEvent.getGazePoint().getGazePointX();
                        int y = (int) gazeEyeMovementEvent.getGazePoint().getGazePointY();
                        SimulateUtils.simulateClick(_mActivity, x, y);
                        if (mBottomBar.getCurrentItemPosition() == OBJECT_DETECTION_TAB) {
                            EventBusActivityScope.getDefault(_mActivity).post(new SelectObjectEvent(gazeEyeMovementEvent.getGazePoint()));
                        }
                    }
                } else {
                    fixationCounter = 0;
                }
            } else {
                preEyeMovementState = EyeMovementState.FIXATION;
                fixationCounter = 0;
            }
        } else if (state == EyeMovementState.SACCADE) {
            if (preEyeMovementState != EyeMovementState.SACCADE) {
                preEyeMovementState = EyeMovementState.SACCADE;
                fixationCounter = 0;
            }
        } else {
            fixationCounter = 0;
        }
    }

    private void countFixation(Coordinate coordinate) {
        // FIXATION
        if ((Math.abs(preCoordinate.getX() - coordinate.getX()) < 80) && (Math.abs(preCoordinate.getY() - coordinate.getY()) < 80)) {
            fixationCounter++;
            if (fixationCounter <= 50) {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPbGaze.setProgress(fixationCounter * 2);
                    }
                });
                if (fixationCounter == 50) {
                    SimulateUtils.simulateClick2(_mActivity, coordinate.getX(), coordinate.getY());
                    ToastUtils.showShortSafe("Click: " +  coordinate.getX() + ", " + coordinate.getY());
                    if (mBottomBar.getCurrentItemPosition() == OBJECT_DETECTION_TAB) {
                        GazePoint gazePoint = new GazePoint(coordinate.getX(), coordinate.getY(), TrackingState.TRACKING);
                        EventBusActivityScope.getDefault(_mActivity).post(new SelectObjectEvent(gazePoint));
                    }
                }
            } else {
                fixationCounter = 0;
            }
        }
        // SACCADE
        else {
            preCoordinate = coordinate;
            fixationCounter = 0;
        }
    }

    @Subscribe
    public void onNotificationEvent(NotificationEvent event) {
        User user = event.getUser();
        int id = user.getId();
        String firebaseToken = user.getFirebaseToken();
        String name = user.getName();
        String message = event.getMessage();
        startWithPopTo(NotificationFragment.newInstance(id, firebaseToken, name, message), MainFragment.class, false);
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
        mBottomBar.setCurrentItem(CONTACT_TAB);
        String message = results.length > 0 ? results[0] : "";
        // 若当前位置MainFragment
        if (getTopFragment() instanceof MainFragment) {
            startBrotherFragment(DialogFragment.newInstance(Constants.UNSPECIFIED_USER_ID, Constants.UNSPECIFIED_FIREBASE_TOKEN, message, false));
        }
        // 若当前位置DialogFragment
        if (getTopFragment() instanceof DialogFragment) {
            EventBusActivityScope.getDefault(_mActivity).post(new ReceiveMessageEvent(message));
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
        // 若当前位置MainFragment
        if (getTopFragment() instanceof MainFragment) {
            // 需要再次点击SPEAK_TAB结束STT
            if (STTHelper.getInstance().isSpeaking()) {
                mBottomBar.setCurrentItem(SPEAK_TAB);
            }
        }
        // 若当前位置DialogFragment
        if (getTopFragment() instanceof DialogFragment) {
            // 需要再次点击SPEAK_TAB结束STT
            int dialogFragmentSpeakTabPosition = ((DialogFragment) getTopFragment()).getSpeakTabPosition();
            BottomBar dialogFragmentBottomBar = ((DialogFragment) getTopFragment()).getBottomBar();
            if (dialogFragmentBottomBar != null && STTHelper.getInstance().isSpeaking()) {
                dialogFragmentBottomBar.setCurrentItem(dialogFragmentSpeakTabPosition);
            }
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

    // Markov
    @Override
    public void onDataSetsLoaded() {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGivLoading.setVisibility(View.GONE);
            }
        });
        ToastUtils.showShortSafe("Load data sets successfully...");
    }

    @Override
    public void onDataSetsError() {
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGivLoading.setVisibility(View.GONE);
            }
        });
        ToastUtils.showShortSafe("Load data sets error...");
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
    public void onTaskCompleted(String response, int requestId, String... others) {
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

    @Override
    public void onDestroyView() {
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            if (Config.sUseMode == SpUtilValueConstants.DEFAULT_MODE) {
                stopGazeService();
            } if (Config.sUseMode == SpUtilValueConstants.SOCKET_MODE) {
                stopWebSocketService();
            }
        } else if (Config.sUseMode == SpUtilValueConstants.MUSCLE_CONTROL_MODE) {
            BluetoothHelper.getInstance().releaseBluetooth();
        }
        STTHelper.getInstance().releaseSTT();
        TTSHelper.getInstance().releaseTTS();
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }
}
