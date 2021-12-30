package com.example.enactusapp.Fragment.Notification;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Entity.Coordinate;
import com.example.enactusapp.Entity.GazePoint;
import com.example.enactusapp.Entity.Selection;
import com.example.enactusapp.Event.GazeEvent.GazeCoordEvent;
import com.example.enactusapp.Event.GazeEvent.GazeEyeMovementEvent;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Event.WebSocketEvent;
import com.example.enactusapp.EyeTracker.PointView;
import com.example.enactusapp.Fragment.Base.BaseBackFragment;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.ScreenUtils;
import com.example.enactusapp.Utils.SimulateUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.shehuan.niv.NiceImageView;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;

public class NotificationFragment extends BaseBackFragment {

    private static final String TAG = NotificationFragment.class.getSimpleName();

    private static final int EYE_CONTROL_ID = 1;

    private static final int UNSPECIFIED_USER = -1;

    private static final String ID = "id";
    private static final String FIREBASE_TOKEN = "firebaseToken";
    private static final String NAME = "name";
    private static final String MESSAGE = "message";

    private TextView mTvSelection;
    private ImageView mIvBack;
    private NiceImageView mNivThumbnail;
    private TextView mTvName;
    private TextView mTvMessage;
    private ProgressBar mPbGaze;
    private Button startChatBtn;
    private Button cancelBtn;
    private PointView mPvPoint;

    private int id = UNSPECIFIED_USER;
    private String firebaseToken;
    private String message;

    private final List<Selection> selections = new ArrayList<>();
    private int muscleControlRightCount = 0;

    // 凝视点
    private final GazeCoordEvent mGazeCoordEvent = new GazeCoordEvent(new GazePoint());
    private final GazeEyeMovementEvent mGazeEyeMovementEvent = new GazeEyeMovementEvent(new GazePoint());
    // 眼睛是在凝视或在移动
    private int fixationCounter = 0;
    private int preEyeMovementState = EyeMovementState.FIXATION;

    // Web Socket数据
    private final WebSocketEvent mWebSocketEvent = new WebSocketEvent();
    private Coordinate preCoordinate = new Coordinate();

    public static NotificationFragment newInstance(int id, String firebaseToken, String name, String message) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ID, id);
        bundle.putString(FIREBASE_TOKEN, firebaseToken);
        bundle.putString(NAME, name);
        bundle.putString(MESSAGE, message);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        EventBus.getDefault().register(this);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        initData();
        return view;
    }

    private void initView(View view) {
        mTvSelection = (TextView) view.findViewById(R.id.tv_selection);
        mIvBack = (ImageView) view.findViewById(R.id.iv_back);
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            mTvSelection.setVisibility(View.GONE);
        } else {
            mTvSelection.setVisibility(View.VISIBLE);
        }
        mNivThumbnail = (NiceImageView) view.findViewById(R.id.iv_thumbnail);
        mTvName = (TextView) view.findViewById(R.id.tv_name);
        mTvMessage = (TextView) view.findViewById(R.id.tv_message);
        mPbGaze = (ProgressBar) view.findViewById(R.id.pb_gaze);
        startChatBtn = (Button) view.findViewById(R.id.btn_start_chat);
        cancelBtn = (Button) view.findViewById(R.id.btn_cancel);
        mPvPoint = (PointView) view.findViewById(R.id.pv_point);
        mPvPoint.setPosition(-1, -1);
    }

    private void initData() {
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            id = bundle.getInt(ID, UNSPECIFIED_USER);
            firebaseToken = bundle.getString(FIREBASE_TOKEN);
            String name = bundle.getString(NAME);
            message = bundle.getString(MESSAGE);
            String thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + id + ".jpg";
            Glide.with(this).load(thumbnail).circleCrop().into(mNivThumbnail);
            if (!TextUtils.isEmpty(name)) {
                mTvName.setText(name);
            }
            if (!TextUtils.isEmpty(message)) {
                mTvMessage.setText(message);
            }
        }
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popTo(MainFragment.class, false);
            }
        });
        startChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWithPopTo(DialogFragment.newInstance(id, firebaseToken, message, false), MainFragment.class, false);
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popTo(MainFragment.class, false);
            }
        });
        addSelections();
    }

    private void addSelections() {
        muscleControlRightCount = 0;
        selections.clear();
        selections.add(new Selection(1, "Start"));
        selections.add(new Selection(2, "Cancel"));
        if (selections.size() > 0) {
            mTvSelection.setText(selections.get(0).getName());
        }
    }

    @Subscribe
    public void onGazeCoordEvent(GazeCoordEvent gazeCoordEvent) {
        if (!(getTopFragment() instanceof NotificationFragment)) {
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
        if (!(getTopFragment() instanceof NotificationFragment)) {
            return;
        }
        synchronized (mGazeEyeMovementEvent) {
            mGazeEyeMovementEvent.setGazePoint(gazeEyeMovementEvent.getGazePoint());
            countFixation(mGazeEyeMovementEvent);
        }
    }

    @Subscribe
    public void onWebSocketEvent(WebSocketEvent webSocketEvent) {
        if (!(getTopFragment() instanceof NotificationFragment)) {
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
                }
            } catch (Exception e) {
                e.printStackTrace();
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
    public void onMuscleControlLeftEvents(MuscleControlLeftEvents event) {
        if (event != null && event.getFragmentId() == Constants.NOTIFICATION_FRAGMENT_ID) {
            if (selections.get(muscleControlRightCount).getId() == 1) {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startChatBtn.performClick();
                    }
                });
            } else {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelBtn.performClick();
                    }
                });
            }
        }
    }

    @Subscribe
    public void onMuscleControlRightEvents(MuscleControlRightEvents event) {
        if (event != null && event.getFragmentId() == Constants.NOTIFICATION_FRAGMENT_ID) {
            muscleControlRightCount++;
            if (muscleControlRightCount == selections.size()) {
                muscleControlRightCount = 0;
            }
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvSelection.setText(selections.get(muscleControlRightCount).getName());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }
}
