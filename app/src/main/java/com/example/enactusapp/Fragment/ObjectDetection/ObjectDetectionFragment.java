package com.example.enactusapp.Fragment.ObjectDetection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.os.Environment;
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
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.enactusapp.Camera2.Camera2Helper;
import com.example.enactusapp.Camera2.Camera2Listener;
import com.example.enactusapp.CustomView.OverlayView;
import com.example.enactusapp.CustomView.OverlayView.DrawCallback;
import com.example.enactusapp.Entity.CameraEvent;
import com.example.enactusapp.Entity.ObjectDetectionEvent;
import com.example.enactusapp.R;
import com.example.enactusapp.TensorFlow.Classifier;
import com.example.enactusapp.TensorFlow.MultiBoxTracker;
import com.example.enactusapp.TensorFlow.TFLiteObjectDetectionAPIModel;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.Utils.FileUtils;
import com.example.enactusapp.Utils.ImageUtils;

import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.IOException;
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
public class ObjectDetectionFragment extends SupportFragment implements ViewTreeObserver.OnGlobalLayoutListener, Camera2Listener {

    private static final String TAG = "ObjectDetectionFragment";

    // 默认打开的CAMERA
    private static final String CAMERA_ID = Camera2Helper.CAMERA_ID_BACK;

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
    private TextureView mTvCamera;
    private OverlayView trackingOverlay;
    private Camera2Helper camera2Helper;
    private ImageView mIvPreview;
    private TextView mTvInferenceTimeView;

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
        View view = inflater.inflate(R.layout.fragment_object_detection,container,false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.objectDetection);
        mTvCamera = (TextureView) view.findViewById(R.id.tv_camera);
        mTvCamera.getViewTreeObserver().addOnGlobalLayoutListener(this);
        trackingOverlay = (OverlayView) view.findViewById(R.id.tracking_overlay);
        mIvPreview = (ImageView) view.findViewById(R.id.iv_preview);
        mTvInferenceTimeView = (TextView) view.findViewById(R.id.tv_inference_time);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {

    }

    private void initCamera() {
        camera2Helper = new Camera2Helper.Builder()
                .cameraListener(this)
                .maxPreviewSize(new Point(640, 480))
                .minPreviewSize(new Point(640, 480))
                .specificCameraId(CAMERA_ID)
                .context(_mActivity)
                .previewOn(mTvCamera)
                .previewViewSize(new Point(mTvCamera.getWidth(), mTvCamera.getHeight()))
                .rotation(_mActivity.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera2Helper.start();
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

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= minimumConfidence) {
                    canvas.drawRect(location, paint);

                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);
                    mappedRecognitions.add(result);
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
    public void onGlobalLayout() {
        mTvCamera.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, final Size previewSize, final int displayOrientation, boolean isMirror) {
        Log.i(TAG, "onCameraOpened:  previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());
        this.displayOrientation = displayOrientation;
        this.isMirrorPreview = isMirror;
        this.openedCameraId = cameraId;
    }

    @Override
    public void onPreview(final byte[] y, final byte[] u, final byte[] v, final Size previewSize, final int stride) {

        if (mRGBCameraTrackNv21 == null) {
            mRGBCameraTrackNv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
        }

        if (mRGBCameraVerifyNv21 == null) {
            mRGBCameraVerifyNv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
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
            initClassifier();
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

    @Subscribe
    public void onCameraEvent(CameraEvent event) {
        if(event.isEnabled()) {
            initCamera();
            mIsRGBCameraReady = false;
        }
        else {
            if (camera2Helper != null) {
                camera2Helper.stop();
                camera2Helper.release();
                camera2Helper = null;
            }
        }
    }

    @Subscribe
    public void onObjectDetectionEvent(ObjectDetectionEvent event) {
        if(!event.isShowed()) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (camera2Helper != null) {
            camera2Helper.start();
        }
        mIsRGBCameraReady = false;
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
