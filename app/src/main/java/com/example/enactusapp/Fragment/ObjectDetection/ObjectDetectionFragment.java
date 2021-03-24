package com.example.enactusapp.Fragment.ObjectDetection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.enactusapp.Adapter.SentencesAdapter;
import com.example.enactusapp.CustomView.OverlayView;
import com.example.enactusapp.CustomView.OverlayView.DrawCallback;
import com.example.enactusapp.Entity.GazePoint;
import com.example.enactusapp.Entity.Selection;
import com.example.enactusapp.Event.BackCameraEvent;
import com.example.enactusapp.Event.BluetoothEvent;
import com.example.enactusapp.Event.GazePointEvent;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;
import com.example.enactusapp.TTS.TTSHelper;
import com.example.enactusapp.TensorFlow.Classifier;
import com.example.enactusapp.TensorFlow.MultiBoxTracker;
import com.example.enactusapp.TensorFlow.TFLiteObjectDetectionAPIModel;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.Utils.FileUtils;
import com.example.enactusapp.Utils.ImageUtils;
import com.example.enactusapp.Utils.ScreenUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.UVCCameraHelper.OnMyDevConnectListener;
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnPreViewResultListener;
import com.serenegiant.usb.widget.CameraViewInterface;

import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
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
public class ObjectDetectionFragment extends SupportFragment implements OnItemClickListener, CameraViewInterface.Callback, OnMyDevConnectListener, OnPreViewResultListener {

    private static final int OBJECT_DETECTION_FRAGMENT_ID = 2;
    private static final String TAG = "ObjectDetectionFragment";

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;

    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    private List<Selection> selections = new ArrayList<>();
    private int muscleControlRightCount = 0;

    // Which detection model to use: by default uses TensorFlow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    private TextView mTvSelection;
    private TextureView mTvBackCamera;
    private CameraViewInterface mCviBackCamera;
    private SeekBar mSbBrightness;
    private SeekBar mSbContrast;
    private OverlayView trackingOverlay;
    private ImageView mIvPreview;
    private RecyclerView mRvSentences;
    private SentencesAdapter mSentencesAdapter;
    private TextView mTvInferenceTimeView;
    private TextView mTvCurrentKeyword;
    private Button btnNext;

    private int screenHeight;
    private int screenWidth;

    // UVCCamera
    private UVCCameraHelper mUVCCameraHelper;
    // 是否连接USB
    private boolean isAttached;
    // 是否预览
    private boolean isPreview;

    // RGBCamera是否就绪
    private boolean mIsRGBCameraReady = false;
    // 预览宽度
    private int mPreviewW = -1;
    // 预览高度
    private int mPreviewH = -1;
    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] mRGBCameraTrackNv21, mRGBCameraVerifyNv21;
    // 帧处理
    private volatile boolean mIsRGBCameraNv21Ready;
    // 物体识别处理
    private volatile boolean mIsRGBCameraObjectReady;
    // 物体识别所需要的时间
    private long lastProcessingTimeMs = 0;
    // 时间戳
    private long timestamp = 0;
    // 当前选中的对象
    private String currentObject;
    // Detected Objects
    private List<String> sentences = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private List<Classifier.Recognition> detectedObjects = new LinkedList<>();
    private GazePoint mGazePoint;
    // 是否正在搜索物体
    private boolean isSearchingObject = false;
    // 是否正在等待搜索物体
    private boolean isWaitingSearch = false;
    // 是否正在更新识别物体
    private boolean isUpdatingRecognitionObjects = false;
    private int keywordCounter = 0;

    // 上一次选中的位置
    private int lastSelectedPosition = -1;
    // 上一次选中的句子
    private String lastSelectedSentence = "";

    // YuvToRGB
    private ScriptIntrinsicYuvToRGB mScriptIntrinsicYuvToRGB;
    private Allocation mInAllocation, mOutAllocation;
    private Bitmap mSourceBitmap;

    // 分类器
    private Classifier detector;
    // 物体跟踪框
    private MultiBoxTracker tracker;
    // 相机的帧和TensorFlow要求的帧转换
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    // 相机的帧
    private Bitmap rgbFrameBitmap = null;
    // TensorFlow要求的帧
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    // 线程池
    private static CustomThreadPool sThreadPoolRGBTrack = new CustomThreadPool(Thread.NORM_PRIORITY);
    private static CustomThreadPool sThreadPoolRGBVerify = new CustomThreadPool(Thread.MAX_PRIORITY);

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
            return Long.signum( (long)(lhs.getWidth() * lhs.getHeight()) -
                    (long)(rhs.getWidth() * rhs.getHeight()));
        }
    }

    public static ObjectDetectionFragment newInstance(){
        ObjectDetectionFragment fragment = new ObjectDetectionFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_object_detection, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        initCamera();
        return view;
    }

    private void initView(View view) {
        mTvSelection = (TextView) view.findViewById(R.id.tv_selection);
        mTvBackCamera = (TextureView) view.findViewById(R.id.tv_back_camera);
        mCviBackCamera = (CameraViewInterface) mTvBackCamera;
        mCviBackCamera.setCallback(this);
        mSbBrightness = (SeekBar) view.findViewById(R.id.sb_brightness);
        mSbContrast = (SeekBar) view.findViewById(R.id.sb_contrast);
        trackingOverlay = (OverlayView) view.findViewById(R.id.tracking_overlay);
        mIvPreview = (ImageView) view.findViewById(R.id.iv_preview);
        mRvSentences = (RecyclerView) view.findViewById(R.id.rv_sentences);
        mTvInferenceTimeView = (TextView) view.findViewById(R.id.tv_inference_time);
        mTvCurrentKeyword = (TextView) view.findViewById(R.id.tv_current_keyword);
        btnNext = (Button) view.findViewById(R.id.btn_next);
        mSentencesAdapter = new SentencesAdapter(_mActivity, sentences);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRvSentences.getContext(), linearLayoutManager.getOrientation());
        mRvSentences.setLayoutManager(linearLayoutManager);
        mRvSentences.addItemDecoration(dividerItemDecoration);
        mRvSentences.setAdapter(mSentencesAdapter);
        mSentencesAdapter.setOnItemClickListener(this);
        getScreenSize();
    }

    private void getScreenSize() {
        screenHeight = ScreenUtils.getScreenRealHeight(_mActivity);
        screenWidth = ScreenUtils.getScreenRealWidth(_mActivity);
        Log.i(TAG, "getScreenSize: screenHeight " + screenHeight + ", screenWidth " + screenWidth);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        mSbBrightness.setMax(100);
        mSbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
                    mUVCCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSbContrast.setMax(100);
        mSbContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
                    mUVCCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastSelectedPosition = -1;
                lastSelectedSentence = "";
                muscleControlRightCount = 0;
                try {
                    if (keywordCounter < keywords.size()) {
                        final String currentKeyword = keywords.get(keywordCounter);
                        mTvCurrentKeyword.setText(currentKeyword);
                        initData(currentKeyword);
                        keywordCounter++;
                    } else {
                        keywordCounter = 0;
                    }
                } catch (Exception e) {
                    e.fillInStackTrace();
                    mTvCurrentKeyword.setText("");
                    initData("");
                }
                mSentencesAdapter.notifyDataSetChanged();
            }
        });

        addSelections();
    }

    private void addSelections() {
        muscleControlRightCount = 0;
        selections.clear();
        selections.add(new Selection(1, "Next"));
        selections.add(new Selection(2, "Option 1"));
        selections.add(new Selection(3, "Option 2"));
        selections.add(new Selection(4, "Option 3"));
        selections.add(new Selection(5, "Option 4"));
        selections.add(new Selection(6, "Option 5"));
        if (selections.size() > 0) {
            mTvSelection.setText(selections.get(0).getName());
        }
    }

    private void initData(final String keyword) {
        sentences.clear();
        lastSelectedPosition = -1;
        lastSelectedSentence = "";
        if (keyword.equals("mouse")) {
            sentences.add("Can you pass me my mouse?");
            sentences.add("Is this mouse yours?");
            sentences.add("Who owns this mouse?");
            sentences.add("Can I use this mouse?");
            sentences.add("How much does this mouse?");
        } else if (keyword.equals("laptop")) {
            sentences.add("Can you pass me my laptop?");
            sentences.add("Is this laptop yours?");
            sentences.add("Who owns this laptop?");
            sentences.add("Can I use this laptop?");
            sentences.add("How much does this laptop?");
        } else if (keyword.equals("keyboard")) {
            sentences.add("Can I use this keyboard?");
            sentences.add("Is this keyboard yours?");
            sentences.add("Who owns this keyboard?");
            sentences.add("Is this keyboard comfortable for typing?");
            sentences.add("How much is this keyboard?");
        } else if (keyword.equals("tv")) {
            sentences.add("Can I change the tv channel?");
            sentences.add("Can I turn on the tv?");
            sentences.add("Can I turn off the tv?");
            sentences.add("How much does this tv?");
            sentences.add("Can you watch tv with me?");
        } else if (keyword.equals("cell phone")) {
            sentences.add("Can you pass me my cell phone?");
            sentences.add("Is this cell phone yours?");
            sentences.add("Who owns this cell phone?");
            sentences.add("How much does this cell phone?");
            sentences.add("What brand of cell phone is this?");
        } else if (keyword.equals("book")) {
            sentences.add("Can you pass me my book?");
            sentences.add("Is this book yours?");
            sentences.add("Who is the author of this book?");
            sentences.add("What is this book about?");
            sentences.add("How much does this book?");
        } else if (keyword.equals("chair")) {
            sentences.add("Can I use this chair?");
            sentences.add("Can you push me to the side of the chair?");
            sentences.add("Can I take a break?");
            sentences.add("How much is this chair?");
            sentences.add("This chair looks very comfortable.");
        } else if (keyword.equals("cup")) {
            sentences.add("Can you pass me my cup?");
            sentences.add("Is this cup yours?");
            sentences.add("Who owns this cup?");
            sentences.add("How much does this cup?");
            sentences.add("Can you fill me with a glass of water?");
        } else if (keyword.equals("pencil")) {
            sentences.add("Can you pass me my pencil?");
            sentences.add("Is this pencil yours?");
            sentences.add("Who owns this pencil?");
            sentences.add("Can I use this pencil?");
            sentences.add("How much does this pencil?");
        } else if (keyword.equals("pen")) {
            sentences.add("Can you pass me my pen?");
            sentences.add("Is this pen yours?");
            sentences.add("Who owns this pen?");
            sentences.add("Can I use this pen?");
            sentences.add("How much does this pen?");
        } else if (keyword.equals("bed")) {
            sentences.add("Can you push me to the side of the bed?");
            sentences.add("Can you help me lie down?");
            sentences.add("Can I take a break?");
            sentences.add("How much is this bed?");
            sentences.add("This bed looks very comfortable.");
        } else if (keyword.equals("apple")) {
            sentences.add("Can you pass me my apple?");
            sentences.add("Can I eat this apple?");
            sentences.add("Is this apple yours?");
            sentences.add("Who owns this apple?");
            sentences.add("How much does this apple?");
        } else if (keyword.equals("banana")) {
            sentences.add("Can you pass me my banana?");
            sentences.add("Can I eat this banana?");
            sentences.add("Is this banana yours?");
            sentences.add("Who owns this banana?");
            sentences.add("How much does this banana?");
        }
    }

    private void initCamera() {
        // step.1 initialize UVCCameraHelper
        if (mUVCCameraHelper != null) {
            return;
        }
        mUVCCameraHelper = UVCCameraHelper.getInstance();
        // set default preview size
        mUVCCameraHelper.setDefaultPreviewSize(640, 480);
        // set default frame format，defalut is UVCCameraHelper.Frame_FORMAT_MPEG
        // if using mpeg can not record mp4, please try yuv
        mUVCCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
        mUVCCameraHelper.initUSBMonitor(_mActivity, mCviBackCamera, this);
        mUVCCameraHelper.setOnPreviewFrameListener(this);
    }

    private void releaseCamera() {
        // step.4 release uvc camera resources
        if (mUVCCameraHelper != null) {
            mUVCCameraHelper.release();
            mUVCCameraHelper = null;
        }
    }

    private void initClassifier() {
        tracker = new MultiBoxTracker(_mActivity);
        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                    _mActivity.getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
            Log.i(TAG,  "Classifier Initialized");
            ToastUtils.showShortSafe("Classifier Initialized");
        } catch (final IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Exception initializing classifier! " + e.getMessage());
        }

        croppedBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(
                mPreviewW, mPreviewH,
                TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                ORIENTATIONS.get(Surface.ROTATION_0), MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        tracker.drawDebug(canvas);
                    }
                });

        tracker.setFrameConfiguration(mPreviewW, mPreviewH, ORIENTATIONS.get(Surface.ROTATION_0));
    }

    private void startTrackRGBTask() {
        sThreadPoolRGBTrack.execute(() -> {
            if (mIsRGBCameraNv21Ready) {

                rgbFrameBitmap = getSceneBtm(mRGBCameraTrackNv21, mPreviewW, mPreviewH);

                trackingOverlay.postInvalidate();
                final Canvas canvas = new Canvas(croppedBitmap);
                canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

                // For examining the actual TF input.
                if (SAVE_PREVIEW_BITMAP) {
                    FileUtils.writeBitmapToDisk(croppedBitmap, Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow" + File.separator + "preview.png");
                }

                 // _mActivity.runOnUiThread(new Runnable() {
                 //     @Override
                 //     public void run() {
                 //         mIvPreview.setImageBitmap(rgbFrameBitmap);
                 //     }
                 // });

                if (!mIsRGBCameraObjectReady) {
                    System.arraycopy(mRGBCameraTrackNv21, 0, mRGBCameraVerifyNv21, 0, mRGBCameraTrackNv21.length);
                    mIsRGBCameraObjectReady = true;

                    startVerifyRGBTask();
                }
                mIsRGBCameraNv21Ready = false;
            }
        });
    }

    private void startVerifyRGBTask() {
        sThreadPoolRGBVerify.execute(() -> {

            ++timestamp;
            final long currTimestamp = timestamp;
            Log.i(TAG, "Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
                case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
            }

            final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

            if (!isSearchingObject) {
                isUpdatingRecognitionObjects = true;
                detectedObjects.clear();
                for (final Classifier.Recognition result : results) {
                    final RectF location = result.getLocation();
                    if (location != null && result.getConfidence() >= minimumConfidence) {
                        detectedObjects.add(result);
                    }
                }
                isUpdatingRecognitionObjects = false;
                if (isWaitingSearch && mGazePoint != null) {
                    Classifier.Recognition searchObject = searchObject(this.mGazePoint);
                    if (searchObject != null) {
                        mTvCurrentKeyword.setText(searchObject.getTitle());
                        initData(searchObject.getTitle());
                    }
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSentencesAdapter.notifyDataSetChanged();
                        }
                    });
                    isWaitingSearch = false;
                }
            }
            keywords.clear();
            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= minimumConfidence) {
                    canvas.drawRect(location, paint);

                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);
                    mappedRecognitions.add(result);

                    Log.i(TAG, "Object Detection result title: " + result.getTitle() + ", top: " + result.getLocation().top + ", bottom: " + result.getLocation().bottom + ", left: " + result.getLocation().left + ", right: " + result.getLocation().right);
                    keywords.add(result.getTitle());
                }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvInferenceTimeView.setText("Inference Time: " + lastProcessingTimeMs + "ms");
                }
            });

            mIsRGBCameraObjectReady = false;
        });
    }

    private Classifier.Recognition searchObject(GazePoint gazePoint) {
        Log.i(TAG, "searchObject: " + gazePoint.getGazePointX() + " " + gazePoint.getGazePointY());
        double convertX = (mPreviewW * 1.0) / screenWidth * gazePoint.getGazePointX();
        double convertY = (mPreviewH * 1.0) / (screenWidth / (mPreviewW * 1.0) * (mPreviewH * 1.0)) * gazePoint.getGazePointY();
        Log.i(TAG, "convertXY: " + convertX + " " + convertY);
        for (int i = 0; i < detectedObjects.size(); i++) {
            Log.i(TAG, "detectedObjects: " + i + " " + detectedObjects.get(i).getLocation().left + " " + detectedObjects.get(i).getLocation().right
                    + " " + detectedObjects.get(i).getLocation().top + " " + detectedObjects.get(i).getLocation().bottom);
            if (detectedObjects.get(i).getLocation().left <= convertX &&
                    detectedObjects.get(i).getLocation().right >= convertX &&
                    detectedObjects.get(i).getLocation().top <= convertY &&
                    detectedObjects.get(i).getLocation().bottom >= convertY) {
                return detectedObjects.get(i);
            }
        }
        return null;
    }

    @Override
    public void onItemClick(int position) {
        if (lastSelectedPosition != -1) {
            sentences.set(lastSelectedPosition, lastSelectedSentence);
        }
        lastSelectedPosition = position;
        lastSelectedSentence = sentences.get(position);
        sentences.set(position, sentences.get(position) + " *");
        mSentencesAdapter.notifyDataSetChanged();
        TTSHelper.getInstance().speak(sentences.get(position));
    }

    // OnMyDevConnectListener
    @Override
    public void onAttachDev(UsbDevice device) {
        // request open permission
        if (!isAttached) {
            isAttached = true;
            ToastUtils.showShortSafe(device.getDeviceName() + " is attached");
            if (((MainFragment) getParentFragment()).getCurrentItemPosition() == 3 && mUVCCameraHelper != null) {
                mUVCCameraHelper.requestPermission(0);
            }
        }
    }

    @Override
    public void onDettachDev(UsbDevice device) {
        // close camera
        if (isAttached) {
            isAttached = false;
            if (mUVCCameraHelper != null) {
                mUVCCameraHelper.stopPreview();
                mUVCCameraHelper.closeCamera();
            }
            ToastUtils.showShortSafe(device.getDeviceName() + " is detached");
        }
    }

    @Override
    public void onConnectDev(UsbDevice device, boolean isConnected) {
        ToastUtils.showShortSafe(device.getDeviceName() + " is connected " + isConnected);
        if (isConnected) {
            // Start preview
            // need to wait UVCCamera initialize over
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Looper.prepare();
                    if (mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
                        mUVCCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS, 30);
                        mUVCCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST, 30);
                        _mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSbBrightness.setProgress(30);
                                mSbContrast.setProgress(30);
                            }
                        });
                        isPreview = true;
                    }
                    Looper.loop();
                }
            }).start();
        } else {
            isPreview = false;
        }
    }

    @Override
    public void onDisConnectDev(UsbDevice device) {
        ToastUtils.showShortSafe(device.getDeviceName() + " is disconnected");
    }

    // CameraViewInterface
    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        // if (mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
        //     mUVCCameraHelper.startPreview(mCviBackCamera);
        //     isPreview = true;
        // }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
            mUVCCameraHelper.stopPreview();
            isPreview = false;
        }
    }

    @Override
    public void onPreviewResult(byte[] data) {
        if (mRGBCameraTrackNv21 == null) {
            mRGBCameraTrackNv21 = new byte[data.length];
        }
        if (mRGBCameraVerifyNv21 == null) {
            mRGBCameraVerifyNv21 = new byte[data.length];
        }
        if (!mIsRGBCameraReady) {
            mIsRGBCameraReady = true;
            mPreviewW = 640;
            mPreviewH = 480;
            Log.i(TAG, "mPreviewW: " + mPreviewW + ", mPreviewH: " + mPreviewH);
            initClassifier();
        }
        if (!mIsRGBCameraNv21Ready) {
            System.arraycopy(data, 0, mRGBCameraTrackNv21, 0, data.length);
            mIsRGBCameraNv21Ready = true;
            startTrackRGBTask();
        }
    }

    @Subscribe
    public void onBackCameraEvent(BackCameraEvent event) {
        if(event.isEnabled()) {
            if (mUVCCameraHelper != null && isAttached) {
                mUVCCameraHelper.requestPermission(0);
            }
            mIsRGBCameraReady = false;
            mTvCurrentKeyword.setText("");
            initData("");
        }
        else {
            if (mUVCCameraHelper != null && isAttached) {
                if (mUVCCameraHelper.isCameraOpened()) {
                    mUVCCameraHelper.stopPreview();
                    isPreview = false;
                }
                mUVCCameraHelper.closeCamera();
            }
        }
    }

    @Subscribe
    public void onGazePointEvent(GazePointEvent gazePointEvent) {
        this.mGazePoint = gazePointEvent.getGazePoint();
        if (isUpdatingRecognitionObjects) {
            isWaitingSearch = true;
        } else {
            Classifier.Recognition searchObject = searchObject(this.mGazePoint);
            if (searchObject != null) {
                mTvCurrentKeyword.setText(searchObject.getTitle());
                initData(searchObject.getTitle());
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentencesAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    @Subscribe
    public void onBluetoothEvent(BluetoothEvent bluetoothEvent) {
        if (bluetoothEvent.getCurrentPosition() == 3) {
            if (bluetoothEvent.getChannel1().equals("A") && bluetoothEvent.getChannel2().equals("B")) {
                if (selections.get(muscleControlRightCount).getId() == 1) {
                    btnNext.performClick();
                } else {
                    int position = selections.get(muscleControlRightCount).getId() - 2;
                    TTSHelper.getInstance().speak(sentences.get(position));
                }
            } else if (bluetoothEvent.getChannel1().equals("B") && bluetoothEvent.getChannel2().equals("A")) {
                if (muscleControlRightCount < selections.size() && muscleControlRightCount < sentences.size() + 1) {
                    muscleControlRightCount++;
                    if (selections.get(muscleControlRightCount).getId() == 1) {
                        if (lastSelectedPosition != -1) {
                            sentences.set(lastSelectedPosition, lastSelectedSentence);
                        }
                        mSentencesAdapter.notifyDataSetChanged();
                    } else {
                        int position = selections.get(muscleControlRightCount).getId() - 2;
                        if (lastSelectedPosition != -1) {
                            sentences.set(lastSelectedPosition, lastSelectedSentence);
                        }
                        lastSelectedPosition = position;
                        lastSelectedSentence = sentences.get(position);
                        sentences.set(position, sentences.get(position) + " *");
                        mSentencesAdapter.notifyDataSetChanged();
                    }
                } else {
                    muscleControlRightCount = 0;
                }
                mTvSelection.setText(selections.get(muscleControlRightCount).getName());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mUVCCameraHelper != null) {
            mUVCCameraHelper.registerUSB();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
            mUVCCameraHelper.startPreview(mCviBackCamera);
            isPreview = true;
        }
        mIsRGBCameraReady = false;
    }

    @Override
    public void onPause() {
        if (mUVCCameraHelper != null && mUVCCameraHelper.isCameraOpened()) {
            mUVCCameraHelper.stopPreview();
            isPreview = false;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mUVCCameraHelper != null) {
            mUVCCameraHelper.unregisterUSB();
        }
    }

    @Override
    public void onDestroyView() {
        releaseCamera();
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }

    /**
     * 根据nv21数据生成bitmap
     */
    private Bitmap getSceneBtm(byte[] nv21Bytes, int width, int height) { //8ms左右

        if (nv21Bytes == null) {
            return null;
        }

        if (mInAllocation == null) {
            initRenderScript(width, height);
        }
        long s = SystemClock.uptimeMillis();
        mInAllocation.copyFrom(nv21Bytes);
        mScriptIntrinsicYuvToRGB.setInput(mInAllocation);
        mScriptIntrinsicYuvToRGB.forEach(mOutAllocation);
        if (mSourceBitmap == null || mSourceBitmap.getWidth() * mSourceBitmap.getHeight() != width * height) {
            mSourceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        mOutAllocation.copyTo(mSourceBitmap);
        // Log.i(TAG, "getSceneBtm = " + Math.abs(SystemClock.uptimeMillis() - s));
        return mSourceBitmap;
    }

    private void initRenderScript(int width, int height) {

        RenderScript mRenderScript = RenderScript.create(_mActivity);
        mScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(mRenderScript,
                Element.U8_4(mRenderScript));

        Type.Builder yuvType = new Type.Builder(mRenderScript, Element.U8(mRenderScript))
                .setX(width * height * 3 / 2);
        mInAllocation = Allocation.createTyped(mRenderScript,
                yuvType.create(),
                Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(mRenderScript, Element.RGBA_8888(mRenderScript))
                .setX(width).setY(height);
        mOutAllocation = Allocation.createTyped(mRenderScript,
                rgbaType.create(),
                Allocation.USAGE_SCRIPT);
    }

    @Subscribe
    public void onMuscleControlLeftEvents(MuscleControlLeftEvents event) {
        if (event != null && event.getFragmentId() == OBJECT_DETECTION_FRAGMENT_ID) {
            if (selections.get(muscleControlRightCount).getId() == 1) {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnNext.performClick();
                    }
                });
            } else {
                int position = selections.get(muscleControlRightCount).getId() - 2;
                TTSHelper.getInstance().speak(sentences.get(position));
            }
        }
    }

    @Subscribe
    public void onMuscleControlRightEvents(MuscleControlRightEvents event) {
        if (event != null && event.getFragmentId() == OBJECT_DETECTION_FRAGMENT_ID) {
            muscleControlRightCount++;
            if (muscleControlRightCount < selections.size() && muscleControlRightCount < sentences.size() + 1) {
                if (selections.get(muscleControlRightCount).getId() == 1) {
                    if (lastSelectedPosition != -1) {
                        sentences.set(lastSelectedPosition, lastSelectedSentence);
                    }
                } else {
                    int position = selections.get(muscleControlRightCount).getId() - 2;
                    if (lastSelectedPosition != -1) {
                        sentences.set(lastSelectedPosition, lastSelectedSentence);
                    }
                    lastSelectedPosition = position;
                    lastSelectedSentence = sentences.get(position);
                    sentences.set(position, sentences.get(position) + " *");
                }
            } else {
                muscleControlRightCount = 0;
                if (lastSelectedPosition != -1) {
                    sentences.set(lastSelectedPosition, lastSelectedSentence);
                }
            }
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSentencesAdapter.notifyDataSetChanged();
                    mTvSelection.setText(selections.get(muscleControlRightCount).getName());
                }
            });
        }
    }
}
