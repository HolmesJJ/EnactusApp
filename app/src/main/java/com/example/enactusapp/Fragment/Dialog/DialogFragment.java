package com.example.enactusapp.Fragment.Dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.enactusapp.Adapter.CustomViewPager;
import com.example.enactusapp.Adapter.DialogChildAdapter;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.ChatHistory;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Entity.Coordinate;
import com.example.enactusapp.Entity.GazePoint;
import com.example.enactusapp.Entity.Selection;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.GazeEvent.GazeCoordEvent;
import com.example.enactusapp.Event.GazeEvent.GazeEyeMovementEvent;
import com.example.enactusapp.Event.MessageEvent.SendMessageEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.ConfirmPossibleAnswerEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.GeneratePossibleAnswersEvent;
import com.example.enactusapp.Event.MessageEvent.ReceiveMessageEvent;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Event.PossibleAnswerEvent.PossibleAnswersEvent;
import com.example.enactusapp.Event.PossibleWordEvent.ConfirmPossibleWordEvent;
import com.example.enactusapp.Event.PossibleWordEvent.PossibleWordEvent;
import com.example.enactusapp.Event.PossibleWordEvent.PossibleWordsEvent;
import com.example.enactusapp.Event.PossibleWordEvent.SelectPossibleWordEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.SelectPossibleAnswerEvent;
import com.example.enactusapp.Event.T2KeyboardEvent.ConfirmT2KeyboardEvent;
import com.example.enactusapp.Event.T2KeyboardEvent.SelectT2KeyboardEvent;
import com.example.enactusapp.Event.WebSocketEvent;
import com.example.enactusapp.EyeTracker.PointView;
import com.example.enactusapp.Fragment.Base.BaseBackFragment;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.STT.STTHelper;
import com.example.enactusapp.TTS.TTSHelper;
import com.example.enactusapp.UI.BottomBar;
import com.example.enactusapp.UI.BottomBarTab;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.ScreenUtils;
import com.example.enactusapp.Utils.SimulateUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.mlkit.nl.smartreply.TextMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import pl.droidsonroids.gif.GifImageView;

public class DialogFragment extends BaseBackFragment implements OnTaskCompleted {

    private static final String TAG = DialogFragment.class.getSimpleName();

    private static final int EYE_CONTROL_ID = 1;

    private static final int SPEAK_TAB = 0;

    private static final int SEND_MESSAGE = 1;

    private static final int LEFT_BUTTON_ID = 3;
    private static final int RIGHT_BUTTON_ID = 4;
    private static final int SEND_ID = 5;
    private static final int BACK_ID = 6;
    private static final int PREV_ID = 7;
    private static final int NEXT_ID = 8;

    private static final String ID = "id";
    private static final String FIREBASE_TOKEN = "firebaseToken";
    private static final String MESSAGE = "message";
    private static final String INITIATOR = "initiator";

    private TextView mTvSelection;
    private ImageView mIvBack;
    private LinearLayout mLlMessageContainer;
    private TextView mTvMessage;
    private TextView mTvPossibleAnswers;
    private EditText mEtKeyword;
    private ImageView mIvScrollLeft;
    private ImageView mIvScrollRight;
    private ImageButton mIbBackspace;
    private ProgressBar mPbGaze;
    private BottomBar mBottomBar;
    private PointView mPvPoint;
    private GifImageView mGivLoading;

    private CustomViewPager dialogAnswerContainerViewPager;

    private int id = Constants.UNSPECIFIED_USER_ID; // -1代表通过麦克风交流，没有特定的id
    private String firebaseToken = Constants.UNSPECIFIED_FIREBASE_TOKEN;
    private String message;
    private boolean initiator = false; // 是否对话的发起者

    private final List<Selection> selections = new ArrayList<>();
    private final List<Selection> paSelections = new ArrayList<>(); // PossibleAnswersFragment
    private final List<Selection> t2Selections = new ArrayList<>(); // T2KeyboardFragment
    private final List<Selection> t26Selections = new ArrayList<>(); // T26KeyboardFragment
    private int muscleControlRightCount = 0;
    private int muscleControlRightPaCount = 0; // PossibleAnswersFragment
    private int muscleControlRightT2Count = 0; // T2KeyboardFragment
    private int muscleControlRightT26Count = 0; // T26KeyboardFragment
    private int currentPage = 1;

    // 凝视点
    private final GazeCoordEvent mGazeCoordEvent = new GazeCoordEvent(new GazePoint());
    private final GazeEyeMovementEvent mGazeEyeMovementEvent = new GazeEyeMovementEvent(new GazePoint());
    // 眼睛是在凝视或在移动
    private int fixationCounter = 0;
    private int preEyeMovementState = EyeMovementState.FIXATION;

    // Web Socket数据
    private final WebSocketEvent mWebSocketEvent = new WebSocketEvent();
    private Coordinate preCoordinate = new Coordinate();

    public static DialogFragment newInstance(int id) {
        DialogFragment fragment = new DialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ID, id);
        bundle.putBoolean(INITIATOR, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static DialogFragment newInstance(int id, String firebaseToken, String message, boolean initiator) {
        DialogFragment fragment = new DialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ID, id);
        bundle.putString(FIREBASE_TOKEN, firebaseToken);
        bundle.putString(MESSAGE, message);
        bundle.putBoolean(INITIATOR, initiator);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog,container,false);
        EventBus.getDefault().register(this);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        initData();
        return view;
    }

    private void initView(View view) {
        mTvSelection = (TextView) view.findViewById(R.id.tv_selection);
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            mTvSelection.setVisibility(View.GONE);
        } else {
            mTvSelection.setVisibility(View.VISIBLE);
        }
        mIvBack = (ImageView) view.findViewById(R.id.iv_back);
        mLlMessageContainer = (LinearLayout) view.findViewById(R.id.ll_message_container);
        mTvMessage = (TextView) view.findViewById(R.id.tv_message);
        mTvPossibleAnswers = (TextView) view.findViewById(R.id.tv_possible_answers);
        mEtKeyword = (EditText) view.findViewById(R.id.et_keyword);
        dialogAnswerContainerViewPager = (CustomViewPager) view.findViewById(R.id.dialog_answer_container);
        dialogAnswerContainerViewPager.setScanScroll(false);
        mIvScrollLeft = (ImageView) view.findViewById(R.id.iv_scroll_left);
        mIvScrollRight = (ImageView) view.findViewById(R.id.iv_scroll_right);
        mIbBackspace = (ImageButton) view.findViewById(R.id.ib_backspace);
        mPbGaze = (ProgressBar) view.findViewById(R.id.pb_gaze);
        mBottomBar = (BottomBar) view.findViewById(R.id.bottomBar);
        mBottomBar.addItem(new BottomBarTab(ContextUtils.getContext(), R.drawable.ic_mic, getString(R.string.speak)));
        mBottomBar.setSpeakTabPositions(SPEAK_TAB);
        mBottomBar.setOnTabSelectedListener(new BottomBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position, int prePosition) {
                // STTHelper是单例模式，可以直接获取实例
                ToastUtils.showShortSafe("Start Speaking");
                STTHelper.getInstance().setSpeaking(true);
                STTHelper.getInstance().start();
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {
                // STTHelper是单例模式，可以直接获取实例
                ToastUtils.showShortSafe("Stop Speaking");
                STTHelper.getInstance().stop();
                STTHelper.getInstance().setSpeaking(false);
            }
        });
        mPvPoint = (PointView) view.findViewById(R.id.pv_point);
        mPvPoint.setPosition(-1, -1);
        mGivLoading = (GifImageView) view.findViewById(R.id.giv_loading);
    }

    private void initData() {
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            id = bundle.getInt(ID, Constants.UNSPECIFIED_USER_ID);
            firebaseToken = bundle.getString(FIREBASE_TOKEN);
            message = bundle.getString(MESSAGE);
            initiator = bundle.getBoolean(INITIATOR, false);
            ChatHistory.CONVERSATIONS.clear();
            if (initiator) {
                ChatHistory.CONVERSATIONS.add(TextMessage.createForLocalUser("Hi, How are you?".toLowerCase(), System.currentTimeMillis()));
            } else {
                handleReceivedMessageEvent(id, message);
            }
            if (!TextUtils.isEmpty(message)) {
                mTvMessage.setText(message);
                mLlMessageContainer.setVisibility(View.VISIBLE);
            } else {
                mLlMessageContainer.setVisibility(View.INVISIBLE);
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

        mIbBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentInputText = mEtKeyword.getText().toString().trim();
                if(currentInputText.length() > 0) {
                    String[] inputTexts = currentInputText.trim().split("\\s+");
                    String newInputText = "";
                    for (int i = 0; i < inputTexts.length - 1; i++) {
                        newInputText = String.format("%s%s ", newInputText, inputTexts[i]);
                    }
                    mEtKeyword.setText(newInputText);
                    mEtKeyword.requestFocus();
                    mEtKeyword.setSelection(mEtKeyword.length());
                } else {
                    mEtKeyword.setText("");
                    mEtKeyword.requestFocus();
                }
            }
        });

        dialogAnswerContainerViewPager.setAdapter(new DialogChildAdapter(getChildFragmentManager(),
                getString(R.string.possible_answer),
                getString(R.string.t2keyboard),
                getString(R.string.t26keyboard)));

        mIvScrollLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPage--;
                if (currentPage < 1) {
                    currentPage = 3;
                }
                if(dialogAnswerContainerViewPager.getCurrentItem() == 0) {
                    dialogAnswerContainerViewPager.setCurrentItem(2);
                    mTvPossibleAnswers.setVisibility(View.INVISIBLE);
                    mEtKeyword.setVisibility(View.VISIBLE);
                    String currentInputText = mEtKeyword.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mEtKeyword.setText(String.format("%s ", currentInputText));
                    }
                    mEtKeyword.requestFocus();
                    mEtKeyword.setSelection(mEtKeyword.getText().length());
                    mIbBackspace.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) ContextUtils.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 1) {
                    dialogAnswerContainerViewPager.setCurrentItem(0);
                    mTvPossibleAnswers.setVisibility(View.VISIBLE);
                    mEtKeyword.setVisibility(View.INVISIBLE);
                    mEtKeyword.clearFocus();
                    mIbBackspace.setVisibility(View.INVISIBLE);
                    InputMethodManager imm = (InputMethodManager) ContextUtils.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 2) {
                    dialogAnswerContainerViewPager.setCurrentItem(1);
                    mTvPossibleAnswers.setVisibility(View.INVISIBLE);
                    mEtKeyword.setVisibility(View.VISIBLE);
                    String currentInputText = mEtKeyword.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mEtKeyword.setText(String.format("%s ", currentInputText));
                    }
                    mEtKeyword.requestFocus();
                    mEtKeyword.setSelection(mEtKeyword.getText().length());
                    mIbBackspace.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) ContextUtils.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
            }
        });

        mIvScrollRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPage++;
                if (currentPage > 3) {
                    currentPage = 1;
                }
                if(dialogAnswerContainerViewPager.getCurrentItem() == 0) {
                    dialogAnswerContainerViewPager.setCurrentItem(1);
                    mTvPossibleAnswers.setVisibility(View.INVISIBLE);
                    mEtKeyword.setVisibility(View.VISIBLE);
                    String currentInputText = mEtKeyword.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mEtKeyword.setText(String.format("%s ", currentInputText));
                    }
                    mEtKeyword.requestFocus();
                    mEtKeyword.setSelection(mEtKeyword.getText().length());
                    mIbBackspace.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) ContextUtils.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 1) {
                    dialogAnswerContainerViewPager.setCurrentItem(2);
                    mTvPossibleAnswers.setVisibility(View.INVISIBLE);
                    mEtKeyword.setVisibility(View.VISIBLE);
                    String currentInputText = mEtKeyword.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mEtKeyword.setText(String.format("%s ", currentInputText));
                    }
                    mEtKeyword.requestFocus();
                    mEtKeyword.setSelection(mEtKeyword.getText().length());
                    mIbBackspace.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) ContextUtils.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 2) {
                    dialogAnswerContainerViewPager.setCurrentItem(0);
                    mTvPossibleAnswers.setVisibility(View.VISIBLE);
                    mEtKeyword.setVisibility(View.INVISIBLE);
                    mEtKeyword.clearFocus();
                    mIbBackspace.setVisibility(View.INVISIBLE);
                    InputMethodManager imm = (InputMethodManager) ContextUtils.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
            }
        });

        addSelections();
        addPaSelections(null);
        addT2Selections(null);
        addT26Selections();
    }

    public int getSpeakTabPosition() {
        return SPEAK_TAB;
    }

    public BottomBar getBottomBar() {
        return mBottomBar;
    }

    private void addSelections() {
        muscleControlRightCount = 0;
        selections.clear();
        selections.add(new Selection(1, "Left"));
        selections.add(new Selection(2, "Right"));
        if (selections.size() > 0) {
            mTvSelection.setText(selections.get(0).getName());
        }
    }

    private void addPaSelections(List<String> pa) {
        addSelections();
        muscleControlRightPaCount = 0;
        paSelections.clear();
        paSelections.addAll(selections);
        if (pa != null) {
            for (int i = 0; i < pa.size(); i++) {
                paSelections.add(new Selection(i + 3, "Option " + (i + 1)));
            }
        }
        if (paSelections.size() > 0) {
            mTvSelection.setText(paSelections.get(0).getName());
        }
    }

    private void addT2Selections(List<String> t2) {
        addSelections();
        muscleControlRightT2Count = 0;
        t2Selections.clear();
        t2Selections.addAll(selections);
        t2Selections.add(new Selection(LEFT_BUTTON_ID, "LeftBtn"));
        t2Selections.add(new Selection(RIGHT_BUTTON_ID, "RightBtn"));
        t2Selections.add(new Selection(SEND_ID, "Send"));
        t2Selections.add(new Selection(BACK_ID, "Back"));
        t2Selections.add(new Selection(PREV_ID, "Prev"));
        t2Selections.add(new Selection(NEXT_ID, "Next"));
        t2Selections.add(new Selection(9, "Backspace"));
        if (t2 != null) {
            for (int i = 0; i < t2.size(); i++) {
                t2Selections.add(new Selection(i + 10, t2.get(i)));
            }
        }
        if (t2Selections.size() > 0) {
            mTvSelection.setText(t2Selections.get(0).getName());
        }
    }

    private void addT26Selections() {
        addSelections();
        muscleControlRightT26Count = 0;
        t26Selections.clear();
        t26Selections.addAll(selections);
        if (t26Selections.size() > 0) {
            mTvSelection.setText(t26Selections.get(0).getName());
        }
    }

    @Subscribe
    public void onGazeCoordEvent(GazeCoordEvent gazeCoordEvent) {
        if (!(getTopFragment() instanceof DialogFragment)) {
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
        if (!(getTopFragment() instanceof DialogFragment)) {
            return;
        }
        synchronized (mGazeEyeMovementEvent) {
            mGazeEyeMovementEvent.setGazePoint(gazeEyeMovementEvent.getGazePoint());
            countFixation(mGazeEyeMovementEvent);
        }
    }

    @Subscribe
    public void onWebSocketEvent(WebSocketEvent webSocketEvent) {
        if (!(getTopFragment() instanceof DialogFragment)) {
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
    public void onReceiveMessageEvent(ReceiveMessageEvent event) {
        handleReceivedMessageEvent(event);
    }

    private void handleReceivedMessageEvent(ReceiveMessageEvent event) {
        // 非特定的交流用户
        if (event.getUser() == null) {
            id = Constants.UNSPECIFIED_USER_ID;
            firebaseToken = Constants.UNSPECIFIED_FIREBASE_TOKEN;
        }
        User user = event.getUser();
        // 新的交流用户需要清空记录
        if (user != null && user.getId() != id) {
            ChatHistory.CONVERSATIONS.clear();
            id = user.getId();
            firebaseToken = user.getFirebaseToken();
        }
        message = event.getMessage();
        handleReceivedMessageEvent(id, message);
    }

    private void handleReceivedMessageEvent(int id, String message) {
        if (!TextUtils.isEmpty(message)) {
            mTvMessage.setText(message);
            mLlMessageContainer.setVisibility(View.VISIBLE);
        } else {
            mLlMessageContainer.setVisibility(View.INVISIBLE);
        }
        ChatHistory.CONVERSATIONS.add(TextMessage.createForRemoteUser(message.toLowerCase(), System.currentTimeMillis(), String.valueOf(id)));
        EventBusActivityScope.getDefault(_mActivity).post(new GeneratePossibleAnswersEvent(message.toLowerCase()));
    }

    @Subscribe
    public void onPossibleWordEvent(PossibleWordEvent event) {
        String newText = mEtKeyword.getText().toString() + event.getPossibleWord();
        mEtKeyword.setText(newText);
        mEtKeyword.setSelection(mEtKeyword.length());
    }

    @Subscribe
    public void onSendMessageEvent(SendMessageEvent event) {
        // PossibleAnswers
        if (!TextUtils.isEmpty(event.getMessage())) {
            String message = event.getMessage();
            ChatHistory.CONVERSATIONS.add(TextMessage.createForLocalUser(message.toLowerCase(), System.currentTimeMillis()));
            TTSHelper.getInstance().speak(message);
            // 非特定的交流用户
            if (id == Constants.UNSPECIFIED_USER_ID) {
                mEtKeyword.setText("");
            }
            // 特定的交流用户
            else {
                mGivLoading.setVisibility(View.VISIBLE);
                HttpAsyncTaskPost task = new HttpAsyncTaskPost(DialogFragment.this, SEND_MESSAGE);
                task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(message, firebaseToken), Constants.SERVER_KEY);
            }
        }
        // T2Keyboard
        else {
            String message = mEtKeyword.getText().toString();
            if (!TextUtils.isEmpty(message)) {
                ChatHistory.CONVERSATIONS.add(TextMessage.createForLocalUser(message.toLowerCase(), System.currentTimeMillis()));
                TTSHelper.getInstance().speak(message);
            }
        }
    }

    @Subscribe
    public void onPossibleAnswersEvent(PossibleAnswersEvent event) {
        if (event != null && event.getPossibleAnswers().size() > 0) {
            addPaSelections(event.getPossibleAnswers());
        }
    }

    @Subscribe
    public void onPossibleWordsEvent(PossibleWordsEvent event) {
        if (event != null && event.getPossibleWords().size() > 0) {
            addT2Selections(event.getPossibleWords());
        }
    }

    private String convertToJSONSendMessage(String message, String firebaseToken) {
        JSONObject jsonMsg = new JSONObject();
        JSONObject content = new JSONObject();
        JSONObject body = new JSONObject();
        JSONObject from = new JSONObject();
        try {
            from.put("id", Config.sUserId);
            from.put("username", Config.sUsername);
            from.put("name", Config.sName);
            from.put("firebaseToken", Config.sFirebaseToken);
            from.put("longitude", Config.sLongitude);
            from.put("latitude", Config.sLatitude);
            body.put("from", from);
            body.put("message", message);
            content.put("title", MessageType.NORMAL.getValue());
            content.put("body", body);
            jsonMsg.put("to", firebaseToken);
            jsonMsg.put("notification", content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONSendMessage(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int code = jsonObject.getInt("success");
            if (code == 1) {
                ToastUtils.showShortSafe("Sent");
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mEtKeyword.setText("");
                    }
                });
            } else {
                String results = jsonObject.getString("results");
                JSONArray jsonArray = new JSONArray(results);
                JSONObject result = new JSONObject(jsonArray.getString(0));
                ToastUtils.showShortSafe(result.getString("error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShortSafe("System error");
        }
    }

    @Override
    public void onTaskCompleted(String response, int requestId, String... others) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == SEND_MESSAGE) {
            retrieveFromJSONSendMessage(response);
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe
    public void onMuscleControlLeftEvents(MuscleControlLeftEvents event) {
        if (event != null && event.getFragmentId() == Constants.DIALOG_FRAGMENT_ID) {
            if (currentPage == 1) {
                if (paSelections.get(muscleControlRightPaCount).getId() == 1) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvScrollLeft.performClick();
                        }
                    });
                } else if (paSelections.get(muscleControlRightPaCount).getId() == 2) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvScrollRight.performClick();
                        }
                    });
                } else {
                    EventBusActivityScope.getDefault(_mActivity).post(new ConfirmPossibleAnswerEvent(muscleControlRightPaCount - 2));
                }
            } else if (currentPage == 2) {
                if (t2Selections.get(muscleControlRightT2Count).getId() == 1) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvScrollLeft.performClick();
                        }
                    });
                } else if (t2Selections.get(muscleControlRightT2Count).getId() == 2) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvScrollRight.performClick();
                        }
                    });
                } else if (t2Selections.get(muscleControlRightT2Count).getId() >= 3 && t2Selections.get(muscleControlRightT2Count).getId() <= 8) {
                    EventBusActivityScope.getDefault(_mActivity).post(new ConfirmT2KeyboardEvent(t2Selections.get(muscleControlRightT2Count).getId()));
                } else if (t2Selections.get(muscleControlRightT2Count).getId() == 9) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIbBackspace.performClick();
                        }
                    });
                } else {
                    EventBusActivityScope.getDefault(_mActivity).post(new ConfirmPossibleWordEvent(muscleControlRightT2Count - 9));
                }
            } else if (currentPage == 3) {
                if (t26Selections.get(muscleControlRightT26Count).getId() == 1) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvScrollLeft.performClick();
                        }
                    });
                } else if (t26Selections.get(muscleControlRightT26Count).getId() == 2) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvScrollRight.performClick();
                        }
                    });
                }
            }
        }
    }

    @Subscribe
    public void onMuscleControlRightEvents(MuscleControlRightEvents event) {
        if (event != null && event.getFragmentId() == Constants.DIALOG_FRAGMENT_ID) {
            if (currentPage == 1) {
                muscleControlRightPaCount++;
                if (muscleControlRightPaCount == paSelections.size()) {
                    muscleControlRightPaCount = 0;
                }
                if (muscleControlRightPaCount >= 2) {
                    EventBusActivityScope.getDefault(_mActivity).post(new SelectPossibleAnswerEvent(muscleControlRightPaCount - 2));
                }
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvSelection.setText(paSelections.get(muscleControlRightPaCount).getName());
                    }
                });
            } else if (currentPage == 2) {
                muscleControlRightT2Count++;
                if (muscleControlRightT2Count == t2Selections.size()) {
                    muscleControlRightT2Count = 0;
                }
                else if (muscleControlRightT2Count >= 2 && muscleControlRightT2Count <= 7) {
                    EventBusActivityScope.getDefault(_mActivity).post(new SelectT2KeyboardEvent(t2Selections.get(muscleControlRightPaCount).getId()));
                } else if (muscleControlRightT2Count == 8) {

                } else {
                    EventBusActivityScope.getDefault(_mActivity).post(new SelectPossibleWordEvent(muscleControlRightT2Count - 9));
                }
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvSelection.setText(t2Selections.get(muscleControlRightT2Count).getName());
                    }
                });
            } else if (currentPage == 3) {
                muscleControlRightT26Count++;
                if (muscleControlRightT26Count == t26Selections.size()) {
                    muscleControlRightT26Count = 0;
                }
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvSelection.setText(t26Selections.get(muscleControlRightT26Count).getName());
                    }
                });
            }
        }
    }
}
