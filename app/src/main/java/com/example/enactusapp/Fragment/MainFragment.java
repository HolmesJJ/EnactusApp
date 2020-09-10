package com.example.enactusapp.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.example.enactusapp.Camera2.Camera2Helper;
import com.example.enactusapp.Camera2.Camera2Listener;
import com.example.enactusapp.CustomView.CustomToast;
import com.example.enactusapp.Entity.BackCameraEvent;
import com.example.enactusapp.Entity.MessageEvent;
import com.example.enactusapp.Entity.StartChatEvent;
import com.example.enactusapp.Fragment.Contact.ContactFragment;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.Notification.NotificationFragment;
import com.example.enactusapp.Fragment.ObjectDetection.ObjectDetectionFragment;
import com.example.enactusapp.Fragment.Profile.ProfileFragment;
import com.example.enactusapp.R;
import com.example.enactusapp.SharedPreferences.GetSetSharedPreferences;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.UI.BottomBar;
import com.example.enactusapp.UI.BottomBarTab;
import com.example.enactusapp.Utils.ImageUtils;
import com.example.enactusapp.Config.Config;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Comparator;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class MainFragment extends SupportFragment implements ViewTreeObserver.OnGlobalLayoutListener, Camera2Listener {

    private static final String TAG = "MainFragment";

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;

    // 默认打开的CAMERA
    private static final String FRONT_CAMERA_ID = Camera2Helper.CAMERA_ID_FRONT;

    // RGBCamera是否就绪
    private boolean mIsRGBCameraReady = false;
    // 预览宽度
    private int mPreviewW = -1;
    // 预览高度
    private int mPreviewH = -1;
    // 颜色通道
    private byte[] y;
    private byte[] u;
    private byte[] v;
    // 步长
    private int stride;
    // 显示的旋转角度
    private int displayOrientation;
    // 是否手动镜像预览
    private boolean isMirrorPreview;
    // 实际打开的cameraId
    private String openedCameraId;
    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] mRGBCameraTrackNv21;
    // 帧处理
    private volatile boolean mIsRGBCameraNv21Ready;

    private SupportFragment[] mFragments = new SupportFragment[5];

    private TextureView mTvFrontCamera;
    private Camera2Helper camera2Helper;
    private BottomBar mBottomBar;

    private Handler handler = new Handler();
    private String fireBaseToken;

    // 线程池
    private static CustomThreadPool sThreadPoolRGBTrack = new CustomThreadPool(Thread.NORM_PRIORITY);

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

            loadMultipleRootFragment(R.id.fl_main_container, FIRST,
                    mFragments[FIRST],
                    mFragments[SECOND],
                    mFragments[THIRD],
                    mFragments[FOURTH]
            );
        } else {
            mFragments[FIRST] = firstFragment;
            mFragments[SECOND] = findFragment(DialogFragment.class);
            mFragments[THIRD] = findFragment(ObjectDetectionFragment.class);
            mFragments[FOURTH] = findFragment(ProfileFragment.class);
        }
    }

    private void initView(View view) {

        mTvFrontCamera = (TextureView) view.findViewById(R.id.tv_front_camera);
        mTvFrontCamera.getViewTreeObserver().addOnGlobalLayoutListener(this);

        mBottomBar = (BottomBar) view.findViewById(R.id.bottomBar);

        mBottomBar.addItem(new BottomBarTab(_mActivity, R.drawable.ic_contact, getString(R.string.contact)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_dialog, getString(R.string.dialog)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_object_detection, getString(R.string.objectDetection)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_profile, getString(R.string.profile)));

        mBottomBar.setOnTabSelectedListener(new BottomBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position, int prePosition) {
                if (prePosition == 2 && position != 2) {
                    EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(false));
                    // 开启摄像头
                    initCamera();
                } else if (prePosition != 2 && position == 2) {
                    // 关闭摄像头
                    releaseCamera();
                    EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(true));
                }
                showHideFragment(mFragments[position], mFragments[prePosition]);
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {

            }
        });

        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mMessageBroadcastReceiver, new IntentFilter("getMessageIntent"));
        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mChatBroadcastReceiver, new IntentFilter("getChatIntent"));

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            CustomToast.show(_mActivity, "FireBase Token Error!");
                            return;
                        }
                        try {
                            // Get new Instance ID token
                            fireBaseToken = task.getResult().getToken();
                            System.out.println("fireBaseToken: " + fireBaseToken);
                        } catch (Exception e) {
                            CustomToast.show(_mActivity, "FireBase Token Error!");
                        }
                    }
                });
    }

    private void initCamera() {
        camera2Helper = new Camera2Helper.Builder()
                .cameraListener(this)
                .maxPreviewSize(new Point(640, 480))
                .minPreviewSize(new Point(640, 480))
                .specificCameraId(FRONT_CAMERA_ID)
                .context(_mActivity)
                .previewOn(mTvFrontCamera)
                .previewViewSize(new Point(mTvFrontCamera.getWidth(), mTvFrontCamera.getHeight()))
                .rotation(_mActivity.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera2Helper.start();
    }

    private void releaseCamera() {
        if (camera2Helper != null) {
            camera2Helper.stop();
            camera2Helper.release();
            camera2Helper = null;
        }
    }

    private void startTrackRGBTask() {
        sThreadPoolRGBTrack.execute(() -> {
            if (mIsRGBCameraNv21Ready) {

                // 回传数据是YUV422
                if (y.length / u.length == 2) {
                    ImageUtils.yuv422ToYuv420sp(y, u, v, mRGBCameraTrackNv21, stride, mPreviewH);
                }
                // 回传数据是YUV420
                else if (y.length / u.length == 4) {
                    ImageUtils.yuv420ToYuv420sp(y, u, v, mRGBCameraTrackNv21, stride, mPreviewH);
                }

                mIsRGBCameraNv21Ready = false;
            }
        });
    }

    @Override
    public void onGlobalLayout() {
        mTvFrontCamera.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initCamera();
    }

    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, final Size previewSize, final int displayOrientation, boolean isMirror) {
        Log.i(TAG, "onCameraOpened: previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());
        this.displayOrientation = displayOrientation;
        this.isMirrorPreview = isMirror;
        this.openedCameraId = cameraId;
    }

    @Override
    public void onPreview(final byte[] y, final byte[] u, final byte[] v, final Size previewSize, final int stride) {
        if (mRGBCameraTrackNv21 == null) {
            mRGBCameraTrackNv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
        }

        if (!mIsRGBCameraReady) {
            mIsRGBCameraReady = true;
            mPreviewW = previewSize.getWidth();
            mPreviewH = previewSize.getHeight();
            Log.i(TAG, "mPreviewW: " + mPreviewW + ", mPreviewH: " + mPreviewH);
            this.y = y;
            this.u = u;
            this.v = v;
            this.stride = stride;
        }

        if (!mIsRGBCameraNv21Ready) {
            mIsRGBCameraNv21Ready = true;
            startTrackRGBTask();
        }
    }

    @Override
    public void onCameraClosed() {
        Log.i(TAG, "onCameraClosed: ");
    }

    @Override
    public void onCameraError(Exception e) {
        e.printStackTrace();
    }

    public void startBrotherFragment(SupportFragment targetFragment) {
        start(targetFragment);
    }

    private BroadcastReceiver mMessageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GetSetSharedPreferences.getDefaults("ChatWithDisabled", _mActivity) != null) {
                GetSetSharedPreferences.removeDefaults("ChatWithDisabled", _mActivity);
            }
            String message = intent.getStringExtra("message");
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent(message));
            intent.removeExtra("message");
            if (mBottomBar.getCurrentItemPosition() == 0) {
                showHideFragment(mFragments[1], mFragments[0]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 2) {
                showHideFragment(mFragments[1], mFragments[2]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 3) {
                showHideFragment(mFragments[1], mFragments[3]);
                mBottomBar.setCurrentItem(1);
            }
        }
    };

    private BroadcastReceiver mChatBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GetSetSharedPreferences.setDefaults("ChatWithDisabled", "true", _mActivity);
            startBrotherFragment(NotificationFragment.newInstance());
        }
    };

    @Subscribe
    public void onStartChatEvent(StartChatEvent event) {
        if (Config.sIsLogin && Config.sUserId.equals("A1234567B")) {
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent("Hi, Mr.Wong, How are you?"));
        } else {
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent("Hi, Mr.Chai, How are you?"));
        }
        if (mBottomBar.getCurrentItemPosition() == 0) {
            showHideFragment(mFragments[1], mFragments[0]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 2) {
            showHideFragment(mFragments[1], mFragments[2]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 3) {
            showHideFragment(mFragments[1], mFragments[3]);
            mBottomBar.setCurrentItem(1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (camera2Helper != null) {
            camera2Helper.start();
        }
    }

    @Override
    public void onPause() {
        if (camera2Helper != null) {
            camera2Helper.stop();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (camera2Helper != null) {
            camera2Helper.release();
            camera2Helper = null;
        }
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
