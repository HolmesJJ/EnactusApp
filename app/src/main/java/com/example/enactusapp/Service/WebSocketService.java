package com.example.enactusapp.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Entity.Coordinate;
import com.example.enactusapp.Entity.GazePoint;
import com.example.enactusapp.Event.CalibrationEvent;
import com.example.enactusapp.Event.GazeEvent.GazeEvent;
import com.example.enactusapp.Event.SelectObjectEvent;
import com.example.enactusapp.Event.WebSocketEvent;
import com.example.enactusapp.EyeTracker.CalibrationViewer;
import com.example.enactusapp.EyeTracker.GazeDevice;
import com.example.enactusapp.EyeTracker.GazeHelper;
import com.example.enactusapp.EyeTracker.GazeListener;
import com.example.enactusapp.EyeTracker.PointView;
import com.example.enactusapp.MainActivity;
import com.example.enactusapp.R;
import com.example.enactusapp.UI.TextureViewOutlineProvider;
import com.example.enactusapp.Utils.AppUtils;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.ImageUtils;
import com.example.enactusapp.Utils.ScreenUtils;
import com.example.enactusapp.Utils.SimulateUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.example.enactusapp.WebSocket.Callback.IClientMessageCallback;
import com.example.enactusapp.WebSocket.WebSocketClientManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;

public class WebSocketService extends Service implements ViewTreeObserver.OnGlobalLayoutListener, GazeListener, IClientMessageCallback {

    private static final String TAG = WebSocketService.class.getSimpleName();

    private static final int EYE_CONTROL_ID = 1;

    // 每个功能都需要用一个WindowManager
    private WindowManager wmCamera;
    private WindowManager wmPoint;
    private WindowManager wmCalibration;
    private WindowManager wmMain;
    private WindowManager wmBack;
    private WindowManager wmSlideUp;
    private WindowManager wmSlideDown;
    private WindowManager wmSlideLeft;
    private WindowManager wmSlideRight;
    private WindowManager wmProgressBar;
    // 十字线
    private WindowManager wmCrossPoint;
    private WindowManager wmHorizontalLine;
    private WindowManager wmVerticalLine;
    private WindowManager.LayoutParams wmCrossPointLayoutParams;
    private WindowManager.LayoutParams wmHorizontalLineLayoutParams;
    private WindowManager.LayoutParams wmVerticalLineLayoutParams;

    private View vCamera;
    private View vPoint;
    private View vCalibration;
    private View vMain;
    private View vBack;
    private View vSlideUp;
    private View vSlideDown;
    private View vSlideLeft;
    private View vSlideRight;
    private View vProgressBar;
    // 十字线
    private View vCrossPoint;
    private View vHorizontalLine;
    private View vVerticalLine;

    private CalibrationViewer mVcCalibration;
    private Button mBtnStopCalibration;
    private TextureView mTvFrontCamera;

    private ImageButton ibMain;
    private ImageButton ibBack;
    private ImageButton ibSlideUp;
    private ImageButton ibSlideDown;
    private ImageButton ibSlideLeft;
    private ImageButton ibSlideRight;

    private ProgressBar pbGaze;

    private Handler mainLooper;

    // Calibration
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ViewLayoutChecker viewLayoutChecker;
    private PointView mPvPoint;

    private boolean isCameraViewAttached = false;
    private boolean isPointViewAttached = false;
    private boolean isCalibrationViewAttached = false;
    private boolean isMainViewAttached = false;
    private boolean isBackViewAttached = false;
    private boolean isSlideUpViewAttached = false;
    private boolean isSlideDownViewAttached = false;
    private boolean isSlideLeftViewAttached = false;
    private boolean isSlideRightViewAttached = false;
    // 十字线
    private boolean isCrossPointViewAttached = false;
    private boolean isHorizontalLineViewAttached = false;
    private boolean isVerticalLineViewAttached = false;
    private boolean isProgressBarViewAttached = false;

    private boolean isGazeMode = false;
    private boolean isMoved = false;

    // 眼睛是在凝视或在移动
    private int fixationCounter = 0;

    // Web Socket数据
    private final WebSocketEvent mWebSocketEvent = new WebSocketEvent();
    private Coordinate preCoordinate = new Coordinate();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        createNotificationChannel();
        EventBus.getDefault().register(this);
        initWindowManager();
        initLayout();
        setOnClickListener();
        initMainHandler();
        initHandler();
        showCamera();
        initGaze();
        initWebSocket();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                Constants.GAZE_SERVICE_CHANNEL,
                Constants.GAZE_SERVICE_CHANNEL,
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private void initWindowManager() {
        wmCamera = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmPoint = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmCalibration = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmMain = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmBack = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmSlideUp = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmSlideDown = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmSlideLeft = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmSlideRight = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmProgressBar = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 十字线
        wmCrossPoint = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmHorizontalLine = (WindowManager) getSystemService(WINDOW_SERVICE);
        wmVerticalLine = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void releaseWindowManager() {
        hideCamera();
        hidePoint();
        hideCalibration();
        hideMain();
        hideSlideUp();
        hideSlideDown();
        hideSlideLeft();
        hideSlideRight();
        hideProgressBar();
        hideCrossLines();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(Constants.GAZE_SERVICE_START)) {
            Notification notification = new NotificationCompat.Builder(ContextUtils.getContext(), Constants.GAZE_SERVICE_CHANNEL)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getResources().getString(R.string.gaze_capturing))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .build();
            startForeground(Constants.GAZE_SERVICE_CHANNEL_ID, notification);
            // 系统被杀死后将尝试重新创建服务
            return START_STICKY;
        }  else {
            // 系统被终止后将不会尝试重新创建服务
            return START_NOT_STICKY;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        releaseWindowManager();
        releaseMainHandler();
        releaseHandler();
        if (viewLayoutChecker != null) {
            viewLayoutChecker.releaseChecker();
        }
        GazeHelper.getInstance().stopTracking();
        GazeHelper.getInstance().releaseGaze();
        EventBus.getDefault().unregister(this);
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    private void initLayout() {
        vPoint = LayoutInflater.from(this).inflate(R.layout.layout_gaze_point, null);
        vCalibration = LayoutInflater.from(this).inflate(R.layout.layout_gaze_calibration, null);
        vCamera = LayoutInflater.from(this).inflate(R.layout.layout_gaze_camera, null);
        vMain = LayoutInflater.from(this).inflate(R.layout.layout_gaze_main, null);
        vBack = LayoutInflater.from(this).inflate(R.layout.layout_gaze_back, null);
        vSlideUp = LayoutInflater.from(this).inflate(R.layout.layout_gaze_slide_up, null);
        vSlideDown = LayoutInflater.from(this).inflate(R.layout.layout_gaze_slide_down, null);
        vSlideLeft = LayoutInflater.from(this).inflate(R.layout.layout_gaze_slide_left, null);
        vSlideRight = LayoutInflater.from(this).inflate(R.layout.layout_gaze_slide_right, null);
        vProgressBar = LayoutInflater.from(this).inflate(R.layout.layout_gaze_progress_bar, null);
        // 十字线
        vCrossPoint = LayoutInflater.from(this).inflate(R.layout.layout_gaze_cross_point, null);
        vHorizontalLine = LayoutInflater.from(this).inflate(R.layout.layout_gaze_horizontal_line, null);
        vVerticalLine = LayoutInflater.from(this).inflate(R.layout.layout_gaze_vertical_line, null);
        mVcCalibration = (CalibrationViewer) vCalibration.findViewById(R.id.cv_calibration);
        mPvPoint = (PointView) vPoint.findViewById(R.id.pv_point);
        mBtnStopCalibration = (Button) vCalibration.findViewById(R.id.btn_stop_calibration);
        mTvFrontCamera = (TextureView) vCamera.findViewById(R.id.tv_front_camera);
        mTvFrontCamera.getViewTreeObserver().addOnGlobalLayoutListener(this);
        mTvFrontCamera.setOutlineProvider(new TextureViewOutlineProvider(ImageUtils.dp2px(ContextUtils.getContext(), 5)));
        mTvFrontCamera.setClipToOutline(true);
        ibMain = (ImageButton) vMain.findViewById(R.id.ib_main);
        ibBack = (ImageButton) vBack.findViewById(R.id.ib_back);
        ibSlideUp = (ImageButton) vSlideUp.findViewById(R.id.ib_slide_up);
        ibSlideDown = (ImageButton) vSlideDown.findViewById(R.id.ib_slide_down);
        ibSlideLeft = (ImageButton) vSlideLeft.findViewById(R.id.ib_slide_left);
        ibSlideRight = (ImageButton) vSlideRight.findViewById(R.id.ib_slide_right);
        pbGaze = (ProgressBar) vProgressBar.findViewById(R.id.pb_gaze);
        viewLayoutChecker = new ViewLayoutChecker();
    }

    private void setOnClickListener() {
        mBtnStopCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GazeHelper.getInstance().stopCalibration();
                hideCalibration();
                hidePoint();
            }
        });
        ibMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideMain();
                hideBack();
                hideSlideUp();
                hideSlideDown();
                hideSlideLeft();
                hideSlideRight();
                AppUtils.showApp(ContextUtils.getContext(), MainActivity.class);
            }
        });
        ibBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://blog.csdn.net/weixin_42312511/article/details/106031856
                // 需要两台手机，另一台手机需要已经root而且能够执行adb命令
            }
        });
        ibSlideUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://blog.csdn.net/weixin_42312511/article/details/106031856
                // 需要两台手机，另一台手机需要已经root而且能够执行adb命令
            }
        });
        ibSlideDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://blog.csdn.net/weixin_42312511/article/details/106031856
                // 需要两台手机，另一台手机需要已经root而且能够执行adb命令
            }
        });
        ibSlideLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://blog.csdn.net/weixin_42312511/article/details/106031856
                // 需要两台手机，另一台手机需要已经root而且能够执行adb命令
            }
        });
        ibSlideRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // https://blog.csdn.net/weixin_42312511/article/details/106031856
                // 需要两台手机，另一台手机需要已经root而且能够执行adb命令
            }
        });
    }

    private void showCamera() {
        if (wmCamera == null || vCamera == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 50;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.TOP | Gravity.END;
        wmCamera.addView(vCamera, windowManagerLayoutParams);
        isCameraViewAttached = true;
    }

    private void hideCamera() {
        if (wmCamera != null && vCamera != null && isCameraViewAttached) {
            wmCamera.removeView(vCamera);
            isCameraViewAttached = false;
        }
    }

    private void showPoint() {
        if (wmPoint == null || vPoint == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.CENTER;
        wmPoint.addView(vPoint, windowManagerLayoutParams);
        isPointViewAttached = true;
    }

    private void hidePoint() {
        if (wmPoint != null && vPoint != null && isPointViewAttached) {
            wmPoint.removeView(vPoint);
            isPointViewAttached = false;
        }
    }

    private void showCalibration() {
        if (wmCalibration == null || vCalibration == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.CENTER;
        wmCalibration.addView(vCalibration, windowManagerLayoutParams);
        isCalibrationViewAttached = true;
    }

    private void hideCalibration() {
        if (wmCalibration != null && vCalibration != null && isCalibrationViewAttached) {
            wmCalibration.removeView(vCalibration);
            isCalibrationViewAttached = false;
        }
    }

    private void showMain() {
        if (wmMain == null || vMain == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        wmMain.addView(vMain, windowManagerLayoutParams);
        isMainViewAttached = true;
    }

    private void hideMain() {
        if (wmMain != null && vMain != null && isMainViewAttached) {
            wmMain.removeView(vMain);
            isMainViewAttached = false;
        }
    }

    private void showBack() {
        if (wmBack == null || vBack == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.BOTTOM | Gravity.END;
        wmBack.addView(vBack, windowManagerLayoutParams);
        isBackViewAttached = true;
    }

    private void hideBack() {
        if (wmBack != null && vBack != null && isBackViewAttached) {
            wmBack.removeView(vBack);
            isBackViewAttached = false;
        }
    }

    private void showSlideUp() {
        if (wmSlideUp == null || vSlideUp == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        wmSlideUp.addView(vSlideUp, windowManagerLayoutParams);
        isSlideUpViewAttached = true;
    }

    private void hideSlideUp() {
        if (wmSlideUp != null && vSlideUp != null && isSlideUpViewAttached) {
            wmSlideUp.removeView(vSlideUp);
            isSlideUpViewAttached = false;
        }
    }

    private void showSlideDown() {
        if (wmSlideDown == null || vSlideDown == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        wmSlideDown.addView(vSlideDown, windowManagerLayoutParams);
        isSlideDownViewAttached = true;
    }

    private void hideSlideDown() {
        if (wmSlideDown != null && vSlideDown != null && isSlideDownViewAttached) {
            wmSlideDown.removeView(vSlideDown);
            isSlideDownViewAttached = false;
        }
    }

    private void showSlideLeft() {
        if (wmSlideLeft == null || vSlideLeft == null) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        wmSlideLeft.addView(vSlideLeft, windowManagerLayoutParams);
        isSlideLeftViewAttached = true;
    }

    private void hideSlideLeft() {
        if (wmSlideLeft != null && vSlideLeft != null && isSlideLeftViewAttached) {
            wmSlideLeft.removeView(vSlideLeft);
            isSlideLeftViewAttached = false;
        }
    }

    private void showSlideRight() {
        if (wmSlideRight == null || vSlideRight == null || isSlideRightViewAttached) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 0;
        windowManagerLayoutParams.y = 0;
        windowManagerLayoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        wmSlideRight.addView(vSlideRight, windowManagerLayoutParams);
        isSlideRightViewAttached = true;
    }

    private void hideSlideRight() {
        if (wmSlideRight != null && vSlideRight != null && isSlideRightViewAttached) {
            wmSlideRight.removeView(vSlideRight);
            isSlideRightViewAttached = false;
        }
    }

    private void showProgressBar() {
        if (wmProgressBar == null || vProgressBar == null || isProgressBarViewAttached) {
            return;
        }
        final WindowManager.LayoutParams windowManagerLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        windowManagerLayoutParams.x = 50;
        windowManagerLayoutParams.y = ImageUtils.dp2px(ContextUtils.getContext(), 80); // CameraView的高度;
        windowManagerLayoutParams.gravity = Gravity.TOP | Gravity.END;
        wmProgressBar.addView(vProgressBar, windowManagerLayoutParams);
        isProgressBarViewAttached = true;
    }

    private void hideProgressBar() {
        if (wmProgressBar != null && vProgressBar != null && isProgressBarViewAttached) {
            wmProgressBar.removeView(vProgressBar);
            isProgressBarViewAttached = false;
        }
    }

    // 十字线
    private void showCrossLines() {
        showHorizontalLine();
        showVerticalLine();
        showCrossPoint();
    }

    private void showHorizontalLine() {
        if (wmHorizontalLine == null || vHorizontalLine == null) {
            return;
        }
        wmHorizontalLineLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        wmHorizontalLineLayoutParams.x = 0;
        wmHorizontalLineLayoutParams.y = 0;
        wmHorizontalLineLayoutParams.gravity = Gravity.TOP | Gravity.START;
        wmHorizontalLine.addView(vHorizontalLine, wmHorizontalLineLayoutParams);
        isHorizontalLineViewAttached = true;
    }

    private void showVerticalLine() {
        if (wmVerticalLine == null || vVerticalLine == null) {
            return;
        }
        wmVerticalLineLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        wmVerticalLineLayoutParams.x = 0;
        wmVerticalLineLayoutParams.y = 0;
        wmVerticalLineLayoutParams.gravity = Gravity.TOP | Gravity.START;
        wmVerticalLine.addView(vVerticalLine, wmVerticalLineLayoutParams);
        isVerticalLineViewAttached = true;
    }

    private void showCrossPoint() {
        if (wmCrossPoint == null || vCrossPoint == null) {
            return;
        }
        wmCrossPointLayoutParams = new WindowManager.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        wmCrossPointLayoutParams.x = -ImageUtils.dp2px(ContextUtils.getContext(), 4);
        wmCrossPointLayoutParams.y = -ImageUtils.dp2px(ContextUtils.getContext(), 4);
        wmCrossPointLayoutParams.gravity = Gravity.TOP | Gravity.START;
        wmCrossPoint.addView(vCrossPoint, wmCrossPointLayoutParams);
        isCrossPointViewAttached = true;
    }

    private void hideCrossLines() {
        hideHorizontalLine();
        hideVerticalLine();
        hideCrossPoint();
    }

    private void hideHorizontalLine() {
        if (wmHorizontalLine != null && vHorizontalLine != null && isHorizontalLineViewAttached) {
            wmHorizontalLine.removeView(vHorizontalLine);
            isHorizontalLineViewAttached = false;
        }
    }

    private void hideVerticalLine() {
        if (wmVerticalLine != null && vVerticalLine != null && isVerticalLineViewAttached) {
            wmVerticalLine.removeView(vVerticalLine);
            isVerticalLineViewAttached = false;
        }
    }

    private void hideCrossPoint() {
        if (wmCrossPoint != null && vCrossPoint != null && isCrossPointViewAttached) {
            wmCrossPoint.removeView(vCrossPoint);
            isCrossPointViewAttached = false;
        }
    }

    private void initMainHandler() {
        if (mainLooper == null) {
            mainLooper = new Handler(Looper.getMainLooper());
        }
    }

    private void releaseMainHandler() {
        if (mainLooper != null) {
            mainLooper.removeCallbacksAndMessages(null);
            mainLooper = null;
        }
    }

    /* ====================================== Eye Tracking ====================================== */
    private void initGaze() {
        Log.i(TAG, "Gaze Version: " + GazeTracker.getVersionName());
        GazeHelper.getInstance().initGaze(ContextUtils.getContext(), this);
    }

    /* ================ Calibration ================ */
    private void initHandler() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("background");
            backgroundThread.start();
            if (backgroundHandler == null) {
                backgroundHandler = new Handler(backgroundThread.getLooper());
            }
        }
    }

    private void releaseHandler() {
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
            backgroundHandler = null;
        }
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
        }
    }

    private void setCalibrationPoint(final float x, final float y) {
        mVcCalibration.changeDraw(true, null);
        mVcCalibration.setPointPosition(x, y);
        mVcCalibration.setPointAnimationPower(0);
    }

    private void setCalibrationProgress(final float progress) {
        mVcCalibration.setPointAnimationPower(progress);
    }

    // 注视坐标或校准坐标仅作为绝对坐标（即全屏屏幕）传输，但是Android视图的坐标系是相对坐标系，不考虑操作栏，状态栏和导航栏
    private void setOffsetOfView() {
        if (viewLayoutChecker == null) {
            return;
        }
        viewLayoutChecker.setOverlayView(mPvPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                mPvPoint.setOffset(x, y);
                mVcCalibration.setOffset(x, y);
                mPvPoint.setPosition(-1, -1);
            }
        });
    }

    /* ====================================== Web Socket ====================================== */
    private void initWebSocket() {
        if (!WebSocketClientManager.getInstance().isConnected()) {
            WebSocketClientManager.getInstance().connect(Config.sSocketAddress);
        }
        WebSocketClientManager.getInstance().setClientMessageCallback(this);
    }

    @Override
    public void onGlobalLayout() {
        mTvFrontCamera.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onGazeInitSuccess() {
        Log.i(TAG, "onGazeInitSuccess");
        GazeHelper.getInstance().showAvailableDevices();
        GazeDevice.Info gazeDeviceInfo = GazeHelper.getInstance().showCurrentDeviceInfo();
        mainLooper.post(new Runnable() {
            @Override
            public void run() {
                ToastUtils.showShortSafe(gazeDeviceInfo.modelName + " x: " + gazeDeviceInfo.screen_origin_x + ", y: " + gazeDeviceInfo.screen_origin_y + " " + GazeHelper.getInstance().isCurrentDeviceFound());
            }
        });
        if (mTvFrontCamera.isAvailable()) {
            GazeHelper.getInstance().setCameraPreview(mTvFrontCamera);
        }
        GazeHelper.getInstance().startTracking();
    }

    @Override
    public void onGazeInitFail(int error) {
        Log.i(TAG, "onGazeInitFail");
        mainLooper.post(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    public void onGazeCoord(long timestamp, float x, float y, int state) {

    }

    @Override
    public void onFilteredGazeCoord(long timestamp, float x, float y, int state) {

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
        hideCalibration();
        hidePoint();
        Config.setLastCalibratedTime(System.currentTimeMillis());
        Config.setIsCalibrated(true);
    }

    @Override
    public void onGazeEyeMovement(long timestamp, long duration, float x, float y, int state) {

    }

    @Override
    public void onGazeImage(long timestamp, byte[] image) {
        // Log.i(TAG, "onGazeImage");
        // FileUtils.writeYuvToDisk(640, 480, 100, image, Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow" + File.separator + "GAZE.jpg");
    }

    @Override
    public void onGazeStarted() {
        Log.i(TAG, "onGazeStarted");
        mainLooper.postDelayed(new Runnable() {
            @Override
            public void run() {
                showPoint();
                showCalibration();
                setOffsetOfView();
                GazeHelper.getInstance().startCalibration(CalibrationModeType.FIVE_POINT);
            }
        }, 3000);
    }

    @Override
    public void onGazeStopped(int error) {
        Log.i(TAG, "onGazeStopped");
        if (error != StatusErrorType.ERROR_NONE) {
            switch (error) {
                case StatusErrorType.ERROR_CAMERA_START:
                    mainLooper.post(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showShortSafe("ERROR_CAMERA_START");
                        }
                    });
                    break;
                case StatusErrorType.ERROR_CAMERA_INTERRUPT:
                    mainLooper.post(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showShortSafe("ERROR_CAMERA_INTERRUPT");
                        }
                    });
                    break;
            }
        }
    }

    @Subscribe
    public void onCalibrationEvent(CalibrationEvent event) {
        showPoint();
        showCalibration();
        setOffsetOfView();
        GazeHelper.getInstance().startCalibration(CalibrationModeType.FIVE_POINT);
    }

    // Web Socket
    @Override
    public void onMessage(String message) {
        if (isGazeMode) {
            synchronized (mWebSocketEvent) {
                mWebSocketEvent.setMessage(message);
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
                        mainLooper.post(new Runnable() {
                            @Override
                            public void run() {
                                moveCrossLines(newX, newY);
                            }
                        });
                        Coordinate coordinate = new Coordinate(newX, newY);
                        countFixation(coordinate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            EventBus.getDefault().post(new WebSocketEvent(message));
        }
    }

    private void moveCrossLines(int x, int y) {
        // 同时移动
        if (!isMoved) {
            isMoved = true;
            moveHorizontalLine(y);
            moveVerticalLine(x);
            moveCrossPoint(x, y);
            isMoved = false;
        }
    }

    private void moveHorizontalLine(float y) {
        if (wmHorizontalLine == null || vHorizontalLine == null || wmHorizontalLineLayoutParams == null) {
            return;
        }
        wmHorizontalLineLayoutParams.y = (int) y;
        wmHorizontalLine.updateViewLayout(vHorizontalLine, wmHorizontalLineLayoutParams);
    }

    private void moveVerticalLine(float x) {
        if (wmVerticalLine == null || vVerticalLine == null || wmVerticalLineLayoutParams == null) {
            return;
        }
        wmVerticalLineLayoutParams.x = (int) x;
        wmVerticalLine.updateViewLayout(vVerticalLine, wmVerticalLineLayoutParams);
    }

    private void moveCrossPoint(float x, float y) {
        if (wmCrossPoint == null || vCrossPoint == null || wmCrossPointLayoutParams == null) {
            return;
        }
        wmCrossPointLayoutParams.x = (int) x - ImageUtils.dp2px(ContextUtils.getContext(), 4);
        wmCrossPointLayoutParams.y = (int) y - ImageUtils.dp2px(ContextUtils.getContext(), 4);
        wmCrossPoint.updateViewLayout(vCrossPoint, wmCrossPointLayoutParams);
    }

    private void countFixation(Coordinate coordinate) {
        // FIXATION
        if ((Math.abs(preCoordinate.getX() - coordinate.getX()) < 80) && (Math.abs(preCoordinate.getY() - coordinate.getY()) < 80)) {
            fixationCounter++;
            if (fixationCounter <= 50) {
                mainLooper.post(new Runnable() {
                    @Override
                    public void run() {
                        pbGaze.setProgress(fixationCounter * 2);
                    }
                });
                if (fixationCounter == 50) {
                    // 模拟点击
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
    public void onGazeEvent(GazeEvent event) {
        if (event.isStart()) {
            showMain();
            showBack();
            showSlideUp();
            showSlideDown();
            showSlideLeft();
            showSlideRight();
            showProgressBar();
            showCrossLines();
            isGazeMode = true;
        } else {
            isGazeMode = false;
            hideMain();
            hideBack();
            hideSlideUp();
            hideSlideDown();
            hideSlideLeft();
            hideSlideRight();
            hideProgressBar();
            hideCrossLines();
        }
    }
}
