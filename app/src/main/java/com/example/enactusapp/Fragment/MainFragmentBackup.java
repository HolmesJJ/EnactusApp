package com.example.enactusapp.Fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.BlinkEvent;
import com.example.enactusapp.Event.BackCameraEvent;
import com.example.enactusapp.Event.MessageEvent;
import com.example.enactusapp.Event.StartChatEvent;
import com.example.enactusapp.Fragment.Contact.ContactFragment;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.Notification.NotificationFragment;
import com.example.enactusapp.Fragment.ObjectDetection.ObjectDetectionFragment;
import com.example.enactusapp.Fragment.Profile.ProfileFragment;
import com.example.enactusapp.R;
import com.example.enactusapp.UI.BottomBar;
import com.example.enactusapp.UI.BottomBarTab;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.GPSUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.greenrobot.eventbus.Subscribe;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.facedetect.DetectionBasedTracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class MainFragmentBackup extends SupportFragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int START_LOCATION_ACTIVITY = 99;

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;
    private SupportFragment[] mFragments = new SupportFragment[5];

    private BottomBar mBottomBar;
    private JavaCameraView mJavaCameraView;

    private Handler handler = new Handler();
    private String fireBaseToken;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsolutionFaceSize = 0;
    private Mat frame;
    private Mat mGray;
    private Mat leftEye_tpl;
    private Mat rightEye_tpl;

    private DetectionBasedTracker mNativeDetector;
    private CascadeClassifier eyeDetector;
    private Scalar FACE_COLOR;
    private Scalar EYE_COLOR;
    private Scalar EYEBALL_BORDER_COLOR;
    private Scalar EYEBALL_COLOR;

    private int countLeftEyeBall = 0;
    private int countRightEyeBall = 0;
    private int countFrame = 0;
    private boolean isStartCountFrame = false;

    private static final int PERMISSION_ALL = 100;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static MainFragmentBackup newInstance() {
        Bundle args = new Bundle();
        MainFragmentBackup fragment = new MainFragmentBackup();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_backup, container, false);
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
                    if (mJavaCameraView != null) {
                        mJavaCameraView.setCameraIndex(1);
                        mJavaCameraView.enableView();
                        mJavaCameraView.setVisibility(View.VISIBLE);
                    }
                } else if (prePosition != 2 && position == 2) {
                    if (mJavaCameraView != null) {
                        mJavaCameraView.disableView();
                        mJavaCameraView.setVisibility(View.INVISIBLE);
                    }
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

        mJavaCameraView = (JavaCameraView) view.findViewById(R.id.eye_blinking_cv_camera);

        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mGreetingBroadcastReceiver, new IntentFilter(MessageType.GREETING.getValue()));
        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mNormalBroadcastReceiver, new IntentFilter(MessageType.NORMAL.getValue()));

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
                            fireBaseToken = task.getResult().getToken();
                            System.out.println("fireBaseToken: " + fireBaseToken);
                        } catch (Exception e) {
                            ToastUtils.showShortSafe("FireBase Token Error!");
                        }
                    }
                });

        if (!hasPermissions(_mActivity, PERMISSIONS)) {
            MainFragmentBackup.this.requestPermissions(PERMISSIONS, PERMISSION_ALL);
        }

        staticLoadCVLibraries();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                startCamera();
            }
        }, 1000);

        if (!GPSUtils.isOpenGPS(_mActivity)) {
            startLocation();
        }
    }

    public void startBrotherFragment(SupportFragment targetFragment) {
        start(targetFragment);
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_ALL) {
            if (grantResults.length > 0) {
                int j = 0;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        j++;
                        break;
                    }
                }
                if (j != 0) {
                    _mActivity.finish();
                    System.exit(0);
                }
            } else {
                _mActivity.finish();
                System.exit(0);
            }
            return;
        }
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
            // startBrotherFragment(NotificationFragment.newInstance(id, username, name, firebaseToken, message, longitude, latitude));
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
            } else if (mBottomBar.getCurrentItemPosition() == 2) {
                showHideFragment(mFragments[1], mFragments[2]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 3) {
                showHideFragment(mFragments[1], mFragments[3]);
                mBottomBar.setCurrentItem(1);
            }
        }
    };

    // OpenCV库静态加载并初始化
    private void staticLoadCVLibraries() {
        boolean load = OpenCVLoader.initDebug();
        if (load) {
            System.out.println("OpenCV Libraries loaded...");
        }
        System.loadLibrary("face_detection");
    }

    private void startCamera() {
        mJavaCameraView.setCvCameraViewListener(this);
        // 前置摄像头
        mJavaCameraView.setCameraIndex(1);
        mJavaCameraView.enableView();

        initFaceDetector();
        initEyeDetector();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        leftEye_tpl = new Mat();
        rightEye_tpl = new Mat();
        FACE_COLOR = new Scalar(255, 0, 0);
        EYE_COLOR = new Scalar(0, 0, 255);
        EYEBALL_BORDER_COLOR = new Scalar(0, 255, 255);
        EYEBALL_COLOR = new Scalar(0, 255, 0);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();
        Core.rotate(frame, frame, Core.ROTATE_90_COUNTERCLOCKWISE);
        Core.flip(frame, frame, 1);
        process(frame);

        System.out.println("Left: " + countLeftEyeBall + "; Right: " + countRightEyeBall);
        if (isStartCountFrame && countFrame <= 30) {
            countFrame++;
        } else {
            isStartCountFrame = false;
            countFrame = 0;
        }

        if (!isStartCountFrame) {
            if ((countLeftEyeBall >= 3) && (countRightEyeBall == 0)) {
                EventBusActivityScope.getDefault(_mActivity).post(new BlinkEvent(true));
                isStartCountFrame = true;
            }
            if ((countLeftEyeBall == 0) && (countRightEyeBall >= 3)) {
                EventBusActivityScope.getDefault(_mActivity).post(new BlinkEvent(false));
                isStartCountFrame = true;
            }
        }

        return frame;
    }

    private void process(Mat getFrame) {
//        faceDetect(getFrame.getNativeObjAddr());
        // 第一帧过来的时候
        if (mAbsolutionFaceSize == 0) {
            int height = frame.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsolutionFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsolutionFaceSize);
            mNativeDetector.start();
        }
        Imgproc.cvtColor(getFrame, mGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.equalizeHist(mGray, mGray);
        MatOfRect faces = new MatOfRect();
        mNativeDetector.detect(mGray, faces);
        Rect[] faceList = faces.toArray();
        System.out.println("faceList.length: " + faceList.length);
        if (faceList.length > 0) {
            for (int i = 0; i < faceList.length; i++) {
                Imgproc.rectangle(getFrame, faceList[i].tl(), faceList[i].br(), FACE_COLOR, 2, 8, 0);
                findEyeArea(faceList[i], getFrame);
            }
        }
        faces.release();
    }

    private void initFaceDetector() {
        File file = new File(_mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/haarcascade_frontalface.xml");
        readAndWriteXML(file, R.raw.haarcascade_frontalface_alt_tree);
        mNativeDetector = new DetectionBasedTracker(file.getAbsolutePath(), 0);
        // initLoad(file.getAbsolutePath());
    }

    private void initEyeDetector() {
        File file = new File(_mActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/haarcascade_eye_tree_eyeglasses.xml");
        readAndWriteXML(file, R.raw.haarcascade_eye_tree_eyeglasses);
        eyeDetector = new CascadeClassifier(file.getAbsolutePath());
    }

    private void findEyeArea(Rect faceROI, Mat getFrame) {
        // Step One
        int offy = (int) (faceROI.height * 0.33f);
        int offx = (int) (faceROI.width * 0.15f);
        int sh = (int) (faceROI.height * 0.17f);
        int sw = (int) (faceROI.width * 0.32f);
        int gap = (int) (faceROI.width * 0.025f);
        Point lp_eye = new Point(faceROI.tl().x + offx, faceROI.tl().y + offy);
        Point lp_end = new Point(lp_eye.x + sw - gap, lp_eye.y + sh);

        int right_offx = (int) (faceROI.width * 0.095f);
        int rew = (int) (sw * 0.81f);
        Point rp_eye = new Point(faceROI.tl().x + faceROI.width / 2 + right_offx, faceROI.tl().y + offy);
        Point rp_end = new Point(rp_eye.x + rew, rp_eye.y + sh);

        Imgproc.rectangle(getFrame, lp_eye, lp_end, EYE_COLOR, 2);
        Imgproc.rectangle(getFrame, rp_eye, rp_end, EYE_COLOR, 2);

        // Step Two
        MatOfRect eyes = new MatOfRect();

        Rect left_eye_roi = new Rect();
        left_eye_roi.x = (int) lp_eye.x;
        left_eye_roi.y = (int) lp_eye.y;
        left_eye_roi.width = (int) (lp_end.x - lp_eye.x);
        left_eye_roi.height = (int) (lp_end.y - lp_eye.y);

        Rect right_eye_roi = new Rect();
        right_eye_roi.x = (int) rp_eye.x;
        right_eye_roi.y = (int) rp_eye.y;
        right_eye_roi.width = (int) (rp_end.x - rp_eye.x);
        right_eye_roi.height = (int) (rp_end.y - rp_eye.y);

        // 级联分类器
        Mat leftEye = getFrame.submat(left_eye_roi);
        Mat rightEye = getFrame.submat(right_eye_roi);

        eyeDetector.detectMultiScale(mGray.submat(left_eye_roi), eyes, 1.15, 2, 0, new Size(30, 30), new Size());
        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; i++) {
            leftEye.submat(eyesArray[i]).copyTo(leftEye_tpl);
            detectBall(leftEye.submat(eyesArray[i]));
            Imgproc.rectangle(leftEye, eyesArray[i].tl(), eyesArray[i].br(), EYEBALL_BORDER_COLOR, 2);
            countLeftEyeBall++;
        }
        if (eyesArray.length == 0) {
            countLeftEyeBall = 0;
//            Rect left_roi = matchWithTemplate(leftEye, true);
//            if(left_roi != null) {
//                detectBall(leftEye.submat(left_roi));
//                Imgproc.rectangle(leftEye, left_roi.tl(), left_roi.br(), EYEBALL_BORDER_COLOR, 2);
//            }
//            else {
//                detectBall(leftEye);
//            }
        }
        eyes.release();

        eyes = new MatOfRect();
        eyeDetector.detectMultiScale(mGray.submat(right_eye_roi), eyes, 1.15, 2, 0, new Size(30, 30), new Size());
        eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; i++) {
            rightEye.submat(eyesArray[i]).copyTo(rightEye_tpl);
            detectBall(rightEye.submat(eyesArray[i]));
            Imgproc.rectangle(rightEye, eyesArray[i].tl(), eyesArray[i].br(), EYEBALL_BORDER_COLOR, 2);
            countRightEyeBall++;
        }
        if (eyesArray.length == 0) {
            countRightEyeBall = 0;
//            Rect right_roi = matchWithTemplate(rightEye, false);
//            if(right_roi != null) {
//                detectBall(rightEye.submat(right_roi));
//                Imgproc.rectangle(rightEye, right_roi.tl(), right_roi.br(), EYEBALL_BORDER_COLOR, 2);
//            }
//            else {
//                detectBall(rightEye);
//            }
        }
        eyes.release();
    }

    private void detectBall(Mat eyeImage) {
        Mat gray = new Mat();
        Mat binary = new Mat();
        Imgproc.cvtColor(eyeImage, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);

        Mat k1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1), new Point(-1, -1));
        Mat k2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(20, 20), new Point(-1, -1));

        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, k1);
        Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_OPEN, k2);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierachy = new Mat();
        Imgproc.findContours(binary, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(eyeImage, contours, i, EYEBALL_COLOR, -1);
        }

        hierachy.release();
        contours.clear();
        gray.release();
        binary.release();
    }

    private Rect matchWithTemplate(Mat src, boolean left) {
        Mat tpl = left ? leftEye_tpl : rightEye_tpl;
        if (tpl.cols() == 0 || tpl.rows() == 0) {
            return null;
        }
        int height = src.rows() - tpl.rows() + 1;
        int width = src.cols() - tpl.cols() + 1;
        if (height < 1 || width < 1) {
            return null;
        }
        Mat result = new Mat(height, width, CvType.CV_32FC1);

        // 模板匹配
        int method = Imgproc.TM_CCOEFF_NORMED;
        Imgproc.matchTemplate(src, tpl, result, method);
        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result);
        Point maxloc = minMaxLocResult.maxLoc;

        // ROI
        Rect rect = new Rect();
        rect.x = (int) maxloc.x;
        rect.y = (int) maxloc.y;
        rect.width = tpl.cols();
        rect.height = tpl.rows();
        result.release();

        return rect;
    }

    public void readAndWriteXML(File file, @RawRes int id) {
        InputStream ip = getResources().openRawResource(id);
        InputStreamReader ir = null;
        FileOutputStream op = null;
        BufferedReader br = null;
        OutputStreamWriter ow = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
        String line = null;
        try {
            ir = new InputStreamReader(ip);
            br = new BufferedReader(ir);
            op = new FileOutputStream(file);
            ow = new OutputStreamWriter(op);
            bw = new BufferedWriter(ow);
            pw = new PrintWriter(bw);
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (ir != null) {
                    ir.close();
                }
                if (ip != null) {
                    ip.close();
                }
                if (pw != null) {
                    pw.close();
                }
                if (bw != null) {
                    bw.close();
                }
                if (ow != null) {
                    ow.close();
                }
                if (op != null) {
                    op.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    // 一个Native Method就是一个Java调用非Java代码的接口
    public native void initLoad(String haarFilePath);

    public native void faceDetect(long address);
}
