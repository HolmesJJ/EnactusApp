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
import com.example.enactusapp.Event.BackCameraEvent;
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
import com.example.enactusapp.Utils.ToastUtils;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.UVCCameraHelper.OnMyDevConnectListener;
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnPreViewResultListener;
import com.serenegiant.usb.widget.CameraViewInterface;

import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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

    // Which detection model to use: by default uses TensorFlow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    private Toolbar mToolbar;
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
    // Sentences
    private List<String> sentences = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private int keywordCounter = 0;

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
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.objectDetection);
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
    }

    private void initData(final String keyword) {
        sentences.clear();
        if (keyword.equals("mouse")) {
            sentences.add("That is my mouse.");
            sentences.add("Can you pass me my mouse?");
            sentences.add("How much is this mouse?");
            sentences.add("How long has this mouse been used?");
        } else if (keyword.equals("laptop")) {
            sentences.add("This is my laptop.");
            sentences.add("This laptop is very expensive.");
            sentences.add("Can I use this laptop?");
            sentences.add("How much is this laptop?");
        } else if (keyword.equals("keyboard")) {
            sentences.add("This keyboard is very beautiful.");
            sentences.add("Can I use this keyboard?");
            sentences.add("Is this keyboard comfortable for typing?");
            sentences.add("How much is this keyboard?");
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
        // if using mpeg can not record mp4,please try yuv
        // mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
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
                ORIENTATIONS.get(Surface.ROTATION_90), MAINTAIN_ASPECT);

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

        tracker.setFrameConfiguration(mPreviewW, mPreviewH, ORIENTATIONS.get(Surface.ROTATION_90));
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

    @Override
    public void onItemClick(int position) {
        TTSHelper.getInstance().speak(sentences.get(position));
    }

//    @Override
//    public void onGlobalLayout() {
//        mTvBackCamera.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//    }

//    @Override
//    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, final Size previewSize, final int displayOrientation, boolean isMirror) {
//        Log.i(TAG, "onCameraOpened: previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());
//        this.displayOrientation = displayOrientation;
//        this.isMirrorPreview = isMirror;
//        this.openedCameraId = cameraId;
//    }

//    @Override
//    public void onPreview(final byte[] y, final byte[] u, final byte[] v, final Size previewSize, final int stride) {
//
//        if (mRGBCameraTrackNv21 == null) {
//            mRGBCameraTrackNv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
//        }
//
//        if (mRGBCameraVerifyNv21 == null) {
//            mRGBCameraVerifyNv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
//        }
//
//        if (!mIsRGBCameraReady) {
//            mIsRGBCameraReady = true;
//            mPreviewW = previewSize.getWidth();
//            mPreviewH = previewSize.getHeight();
//            Log.i(TAG, "mPreviewW: " + mPreviewW + ", mPreviewH: " + mPreviewH);
//            this.y = y;
//            this.u = u;
//            this.v = v;
//            this.stride = stride;
//            initClassifier();
//        }
//
//        if (!mIsRGBCameraNv21Ready) {
//            mIsRGBCameraNv21Ready = true;
//            startTrackRGBTask();
//        }
//    }

//    @Override
//    public void onCameraClosed() {
//        Log.i(TAG, "onCameraClosed: ");
//    }

//    @Override
//    public void onCameraError(Exception e) {
//        e.printStackTrace();
//    }

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
}
