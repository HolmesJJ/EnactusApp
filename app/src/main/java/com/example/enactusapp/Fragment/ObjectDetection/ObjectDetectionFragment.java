package com.example.enactusapp.Fragment.ObjectDetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
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
import com.example.enactusapp.Entity.CameraEvent;
import com.example.enactusapp.Entity.ObjectDetectionEvent;
import com.example.enactusapp.R;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.UI.AutoFitTextureView;
import com.example.enactusapp.Utils.ImageUtils;

import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import java.io.ByteArrayOutputStream;
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
public class ObjectDetectionFragment extends SupportFragment implements ViewTreeObserver.OnGlobalLayoutListener, Camera2Listener {

    private static final String TAG = "ObjectDetectionFragment";

    // 默认打开的CAMERA
    private static final String CAMERA_ID = Camera2Helper.CAMERA_ID_BACK;

    private Toolbar mToolbar;
    private TextureView mTvCamera;
    private Camera2Helper camera2Helper;

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
        mTvCamera = (AutoFitTextureView) view.findViewById(R.id.tv_camera);
        mTvCamera.getViewTreeObserver().addOnGlobalLayoutListener(this);
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
                .maxPreviewSize(new Point(1920, 1080))
                .minPreviewSize(new Point(1280, 720))
                .specificCameraId(CAMERA_ID)
                .context(_mActivity)
                .previewOn(mTvCamera)
                .previewViewSize(new Point(mTvCamera.getWidth(), mTvCamera.getHeight()))
                .rotation(_mActivity.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera2Helper.start();
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
                YuvImage yuvImage = new YuvImage(mRGBCameraTrackNv21, ImageFormat.NV21, stride, mPreviewH, null);

                // ByteArrayOutputStream的close中其实没做任何操作，可不执行
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                // 由于某些stride和previewWidth差距大的分辨率，[0,previewWidth)是有数据的，而[previewWidth,stride)补上的U、V均为0，因此在这种情况下运行会看到明显的绿边
                // yuvImage.compressToJpeg(new Rect(0, 0, stride, previewSize.getHeight()), 100, byteArrayOutputStream);

                // 由于U和V一般都有缺损，因此若使用方式，可能会有个宽度为1像素的绿边
                yuvImage.compressToJpeg(new Rect(0, 0, mPreviewW, mPreviewH), 100, byteArrayOutputStream);

                // 为了删除绿边，抛弃一行像素
                // yuvImage.compressToJpeg(new Rect(0, 0, previewSize.getWidth() - 1, previewSize.getHeight()), 100, byteArrayOutputStream);

                byte[] jpgBytes = byteArrayOutputStream.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                // 原始预览数据生成的bitmap
                final Bitmap originalBitmap = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.length, options);
                Matrix matrix = new Matrix();
                // 预览相对于原数据可能有旋转
                matrix.postRotate(Camera2Helper.CAMERA_ID_BACK.equals(openedCameraId) ? displayOrientation : -displayOrientation);

                // 对于前置数据，镜像处理；若手动设置镜像预览，则镜像处理；若都有，则不需要镜像处理
                if (Camera2Helper.CAMERA_ID_FRONT.equals(openedCameraId) ^ isMirrorPreview) {
                    matrix.postScale(-1, 1);
                }

                // 和预览画面相同的bitmap
                final Bitmap previewBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // mIvPreview.setImageBitmap(previewBitmap);
                    }
                });
                mIsRGBCameraNv21Ready = false;
            }
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

        if (!mIsRGBCameraNv21Ready) {
            mIsRGBCameraNv21Ready = true;
            mPreviewW = previewSize.getWidth();
            mPreviewH = previewSize.getHeight();
            this.y = y;
            this.u = u;
            this.v = v;
            this.stride = stride;
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
