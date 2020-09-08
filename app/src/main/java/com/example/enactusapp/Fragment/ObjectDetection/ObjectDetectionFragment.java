package com.example.enactusapp.Fragment.ObjectDetection;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.enactusapp.Adapter.ObjectDetectionSentencesAdapter;
import com.example.enactusapp.CustomView.CustomToast;
import com.example.enactusapp.Entity.CameraEvent;
import com.example.enactusapp.Entity.ObjectDetectionEvent;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;
import com.example.enactusapp.SharedPreferences.GetSetSharedPreferences;

import org.greenrobot.eventbus.Subscribe;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class ObjectDetectionFragment extends SupportFragment implements CameraBridgeViewBase.CvCameraViewListener2, OnItemClickListener {

    private Toolbar mToolbar;
    private RecyclerView mObjectDetectionSentencesRecyclerView;
    private ObjectDetectionSentencesAdapter mObjectDetectionSentencesAdapter;
    private JavaCameraView mJavaCameraView;
    private TextView objectDetectionPen;
    private TextView objectDetectionWallet;
    private TextView objectDetectionGlasses;
    private ImageButton objectDetectionStartBtn;
    private View objectDetectionLine1;
    private View objectDetectionLine2;

    private List<String> objectDetectionSentencesList = new ArrayList<>();

    private Mat frame;
    private int countFrame = 0;

    private TextToSpeech mTextToSpeech;

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
        mJavaCameraView = (JavaCameraView) view.findViewById(R.id.object_detection_cv_camera);
        objectDetectionPen = (TextView) view.findViewById(R.id.object_detection_pen);
        objectDetectionWallet = (TextView) view.findViewById(R.id.object_detection_wallet);
        objectDetectionGlasses = (TextView) view.findViewById(R.id.object_detection_glasses);
        objectDetectionStartBtn = (ImageButton) view.findViewById(R.id.object_detection_start);
        objectDetectionLine1 = (View) view.findViewById(R.id.object_detection_line1);
        objectDetectionLine2 = (View) view.findViewById(R.id.object_detection_line2);
        mObjectDetectionSentencesRecyclerView = (RecyclerView) view.findViewById(R.id.object_detection_sentences_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mObjectDetectionSentencesRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mObjectDetectionSentencesRecyclerView.setLayoutManager(linearLayoutManager);
        mObjectDetectionSentencesRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        if(GetSetSharedPreferences.getDefaults("isEnableEyeTrackingCamera", _mActivity) != null) {
            GetSetSharedPreferences.removeDefaults("isEnableEyeTrackingCamera", _mActivity);
            startCamera();
        }

        mTextToSpeech = new TextToSpeech(_mActivity, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i == TextToSpeech.SUCCESS) {
                    int result = mTextToSpeech.setLanguage(Locale.UK);
                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        CustomToast.show(_mActivity, "Text to Speech Error!");
                    }
                }
                else {
                    CustomToast.show(_mActivity, "Text to Speech Error!");
                }
            }
        });

        objectDetectionStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                objectDetectionPen.setVisibility(View.VISIBLE);
                objectDetectionWallet.setVisibility(View.VISIBLE);
                objectDetectionGlasses.setVisibility(View.VISIBLE);
                objectDetectionLine1.setVisibility(View.VISIBLE);
                objectDetectionLine2.setVisibility(View.VISIBLE);
            }
        });

        objectDetectionPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                objectDetectionSentencesList.clear();
                objectDetectionSentencesList.add("That is a ink pen.");
                objectDetectionSentencesList.add("This is a very beautiful pen.");
                objectDetectionSentencesList.add("My pen is out of ink.");
                objectDetectionSentencesList.add("Shall I buy a pen as a gift for someone?");
                refreshList();
            }
        });

        objectDetectionWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                objectDetectionSentencesList.clear();
                objectDetectionSentencesList.add("I forgot my wallet.");
                objectDetectionSentencesList.add("This wallet looks beautiful.");
                objectDetectionSentencesList.add("I wish i can have this wallet.");
                objectDetectionSentencesList.add("The wallet was a gift from a friend.");
                refreshList();
            }
        });

        objectDetectionGlasses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                objectDetectionSentencesList.clear();
                objectDetectionSentencesList.add("I need my glasses.");
                objectDetectionSentencesList.add("Pick up my glasses for me please?");
                objectDetectionSentencesList.add("My glasses are damaged.");
                objectDetectionSentencesList.add("My glasses look unfashionable.");
                refreshList();
            }
        });
    }

    private void refreshList() {
        mObjectDetectionSentencesAdapter = new ObjectDetectionSentencesAdapter(_mActivity, objectDetectionSentencesList);
        mObjectDetectionSentencesRecyclerView.setAdapter(mObjectDetectionSentencesAdapter);
        mObjectDetectionSentencesAdapter.setOnItemClickListener(this);
    }

    private void startCamera() {
        mJavaCameraView.setCvCameraViewListener(this);
        // 前置摄像头
        mJavaCameraView.setCameraIndex(0);
        mJavaCameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();
        Core.rotate(frame, frame, Core.ROTATE_90_CLOCKWISE);
        return frame;
    }

    private void speak(String speakText) {
        mTextToSpeech.setPitch(0.5f);
        mTextToSpeech.setPitch(0.5f);
        mTextToSpeech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Subscribe
    public void onCameraEvent(CameraEvent event) {
        if(event.isEnabled()) {
            if(mJavaCameraView != null) {
                mJavaCameraView.setCameraIndex(0);
                mJavaCameraView.enableView();
            }
        }
        else {
            if(mJavaCameraView != null) {
                mJavaCameraView.disableView();
            }
        }
    }

    @Subscribe
    public void onObjectDetectionEvent(ObjectDetectionEvent event) {
        if(!event.isShowed()) {
            objectDetectionPen.setVisibility(View.GONE);
            objectDetectionWallet.setVisibility(View.GONE);
            objectDetectionGlasses.setVisibility(View.GONE);
            objectDetectionLine1.setVisibility(View.GONE);
            objectDetectionLine2.setVisibility(View.GONE);
            objectDetectionSentencesList.clear();
            refreshList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
    }

    @Override
    public void onItemClick(int position) {
        speak(objectDetectionSentencesList.get(position));
    }
}
