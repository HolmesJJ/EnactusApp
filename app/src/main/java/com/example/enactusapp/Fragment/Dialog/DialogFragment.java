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
import android.widget.TextView;

import com.example.enactusapp.Adapter.CustomViewPager;
import com.example.enactusapp.Adapter.DialogChildAdapter;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Entity.Selection;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.ClearChatHistoryEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.ConfirmPossibleAnswerEvent;
import com.example.enactusapp.Event.GreetingEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.MessageToPossibleAnswersEvent;
import com.example.enactusapp.Event.MessageEvent;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Event.PossibleAnswerEvent.PossibleAnswersEvent;
import com.example.enactusapp.Event.PossibleWordEvent.ConfirmPossibleWordEvent;
import com.example.enactusapp.Event.PossibleWordEvent.PossibleWordEvent;
import com.example.enactusapp.Event.PossibleWordEvent.PossibleWordsEvent;
import com.example.enactusapp.Event.PossibleWordEvent.SelectPossibleWordEvent;
import com.example.enactusapp.Event.RequireMessageEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.SelectPossibleAnswerEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.SpeakPossibleAnswerEvent;
import com.example.enactusapp.Event.T2KeyboardEvent.ConfirmT2KeyboardEvent;
import com.example.enactusapp.Event.T2KeyboardEvent.SelectT2KeyboardEvent;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.TTS.TTSHelper;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;
import pl.droidsonroids.gif.GifImageView;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class DialogFragment extends SupportFragment implements OnTaskCompleted {

    private static final int DIALOG_FRAGMENT_ID = 1;
    private static final String TAG = "DialogFragment";
    private static final int SEND_MESSAGE = 1;

    private static final int LEFT_BUTTON_ID = 3;
    private static final int RIGHT_BUTTON_ID = 4;
    private static final int SEND_ID = 5;
    private static final int BACK_ID = 6;
    private static final int PREV_ID = 7;
    private static final int NEXT_ID = 8;

    private TextView mTvSelection;
    private LinearLayout mLlMessageContainer;
    private TextView mMessageTextView;
    private TextView mPossibleAnswers;
    private EditText mInputEditText;
    private ImageView mScrollLeftBtn;
    private ImageView mScrollRightBtn;
    private ImageButton inputBackspaceBtn;
    private GifImageView mGivLoading;

    private CustomViewPager dialogAnswerContainerViewPager;

    private User user;
    private String message;
    private List<FirebaseTextMessage> chatHistory = new ArrayList<>();

    private List<Selection> selections = new ArrayList<>();
    private List<Selection> paSelections = new ArrayList<>(); // PossibleAnswersFragment
    private List<Selection> t2Selections = new ArrayList<>(); // T2KeyboardFragment
    private List<Selection> t26Selections = new ArrayList<>(); // T26KeyboardFragment
    private int muscleControlRightCount = 0;
    private int muscleControlRightPaCount = 0; // PossibleAnswersFragment
    private int muscleControlRightT2Count = 0; // T2KeyboardFragment
    private int muscleControlRightT26Count = 0; // T26KeyboardFragment
    private int currentPage = 1;

    public static DialogFragment newInstance() {
        DialogFragment fragment = new DialogFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog,container,false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mTvSelection = (TextView) view.findViewById(R.id.tv_selection);
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            mTvSelection.setVisibility(View.GONE);
        } else {
            mTvSelection.setVisibility(View.VISIBLE);
        }
        mLlMessageContainer = (LinearLayout) view.findViewById(R.id.ll_message_container);
        mMessageTextView = (TextView) view.findViewById(R.id.tv_message);
        mPossibleAnswers = (TextView) view.findViewById(R.id.possible_answers);
        mInputEditText = (EditText) view.findViewById(R.id.et_keyword);
        dialogAnswerContainerViewPager = (CustomViewPager) view.findViewById(R.id.dialog_answer_container);
        dialogAnswerContainerViewPager.setScanScroll(false);
        mScrollLeftBtn = (ImageView) view.findViewById(R.id.scroll_left_btn);
        mScrollRightBtn = (ImageView) view.findViewById(R.id.scroll_right_btn);
        inputBackspaceBtn = (ImageButton) view.findViewById(R.id.ibtn_input_backspace);
        mGivLoading = (GifImageView) view.findViewById(R.id.giv_loading);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {

        inputBackspaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentInputText = mInputEditText.getText().toString().trim();
                if(currentInputText.length() > 0) {
                    String[] inputTexts = currentInputText.trim().split("\\s+");
                    String newInputText = "";
                    for (int i = 0; i < inputTexts.length - 1; i++) {
                        newInputText = String.format("%s%s ", newInputText, inputTexts[i]);
                    }
                    mInputEditText.setText(newInputText);
                    mInputEditText.requestFocus();
                    mInputEditText.setSelection(mInputEditText.length());
                } else {
                    mInputEditText.setText("");
                    mInputEditText.requestFocus();
                }
            }
        });

        dialogAnswerContainerViewPager.setAdapter(new DialogChildAdapter(getChildFragmentManager(),
                getString(R.string.possible_answer),
                getString(R.string.t2keyboard),
                getString(R.string.t26keyboard)));

        mScrollLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPage--;
                if (currentPage < 1) {
                    currentPage = 3;
                }
                if(dialogAnswerContainerViewPager.getCurrentItem() == 0) {
                    dialogAnswerContainerViewPager.setCurrentItem(2);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    String currentInputText = mInputEditText.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mInputEditText.setText(String.format("%s ", currentInputText));
                    }
                    mInputEditText.requestFocus();
                    mInputEditText.setSelection(mInputEditText.getText().length());
                    inputBackspaceBtn.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 1) {
                    dialogAnswerContainerViewPager.setCurrentItem(0);
                    mPossibleAnswers.setVisibility(View.VISIBLE);
                    mInputEditText.setVisibility(View.INVISIBLE);
                    mInputEditText.clearFocus();
                    inputBackspaceBtn.setVisibility(View.INVISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 2) {
                    dialogAnswerContainerViewPager.setCurrentItem(1);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    String currentInputText = mInputEditText.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mInputEditText.setText(String.format("%s ", currentInputText));
                    }
                    mInputEditText.requestFocus();
                    mInputEditText.setSelection(mInputEditText.getText().length());
                    inputBackspaceBtn.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
            }
        });

        mScrollRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPage++;
                if (currentPage > 3) {
                    currentPage = 1;
                }
                if(dialogAnswerContainerViewPager.getCurrentItem() == 0) {
                    dialogAnswerContainerViewPager.setCurrentItem(1);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    String currentInputText = mInputEditText.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mInputEditText.setText(String.format("%s ", currentInputText));
                    }
                    mInputEditText.requestFocus();
                    mInputEditText.setSelection(mInputEditText.getText().length());
                    inputBackspaceBtn.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 1) {
                    dialogAnswerContainerViewPager.setCurrentItem(2);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    String currentInputText = mInputEditText.getText().toString();
                    if (currentInputText.length() > 0 && !currentInputText.substring(currentInputText.length() - 1).equals(" ")) {
                        mInputEditText.setText(String.format("%s ", currentInputText));
                    }
                    mInputEditText.requestFocus();
                    mInputEditText.setSelection(mInputEditText.getText().length());
                    inputBackspaceBtn.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 2) {
                    dialogAnswerContainerViewPager.setCurrentItem(0);
                    mPossibleAnswers.setVisibility(View.VISIBLE);
                    mInputEditText.setVisibility(View.INVISIBLE);
                    mInputEditText.clearFocus();
                    inputBackspaceBtn.setVisibility(View.INVISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
            }
        });

        if (user != null && !TextUtils.isEmpty(message)) {
            mMessageTextView.setText(message);
            mLlMessageContainer.setVisibility(View.VISIBLE);
        }

        addSelections();
        addPaSelections(null);
        addT2Selections(null);
        addT26Selections();
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
    public void onGreetingEvent(GreetingEvent event) {
        chatHistory.clear();
        chatHistory.add(FirebaseTextMessage.createForLocalUser("Hi, How are you?", System.currentTimeMillis()));
    }

    @Subscribe
    public void onClearChatHistoryEvent(ClearChatHistoryEvent event) {
        chatHistory.clear();
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        if (user == null) {
            chatHistory.clear();
        }
        if (user != null && event.getUser() != null && user.getId() != event.getUser().getId()) {
            chatHistory.clear();
        }
        if (event.getUser() != null) {
            user = event.getUser();
        }
        message = event.getMessage();
        mMessageTextView.setText(message);
        mLlMessageContainer.setVisibility(View.VISIBLE);
        // 不是麦克风的信息
        if (user != null) {
            if (chatHistory.size() == 0) {
                chatHistory.add(FirebaseTextMessage.createForRemoteUser("Hi, How are you?", System.currentTimeMillis(), String.valueOf(user.getId())));
            } else {
                chatHistory.add(FirebaseTextMessage.createForRemoteUser(message, System.currentTimeMillis(), String.valueOf(user.getId())));
            }
        } else {
            chatHistory.add(FirebaseTextMessage.createForRemoteUser(message, System.currentTimeMillis(), String.valueOf(-1)));
        }
        EventBusActivityScope.getDefault(_mActivity).post(new MessageToPossibleAnswersEvent(user, message, chatHistory));
    }

    @Subscribe
    public void onRequireMessageEvent(RequireMessageEvent event) {
        if (!TextUtils.isEmpty(message)) {
            EventBusActivityScope.getDefault(_mActivity).post(new MessageToPossibleAnswersEvent(user, message, chatHistory));
        }
    }

    @Subscribe
    public void onPossibleWordEvent(PossibleWordEvent event) {
        String newText = mInputEditText.getText().toString() + event.getPossibleWord();
        mInputEditText.setText(newText);
        mInputEditText.setSelection(mInputEditText.length());
    }

    @Subscribe
    public void onSpeakPossibleAnswerEvent(SpeakPossibleAnswerEvent event) {
        if (!TextUtils.isEmpty(event.getAnswer())) {
            chatHistory.add(FirebaseTextMessage.createForLocalUser(event.getAnswer(), System.currentTimeMillis()));
            TTSHelper.getInstance().speak(event.getAnswer());
        } else {
            chatHistory.add(FirebaseTextMessage.createForLocalUser(mInputEditText.getText().toString(), System.currentTimeMillis()));
            TTSHelper.getInstance().speak(mInputEditText.getText().toString());
        }
        if (user == null || user.getFirebaseToken() == null) {
            mInputEditText.setText("");
        } else {
            mGivLoading.setVisibility(View.VISIBLE);
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(DialogFragment.this, SEND_MESSAGE);
            if(!TextUtils.isEmpty(event.getAnswer())) {
                task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(event.getAnswer(), user.getFirebaseToken()), Constants.SERVER_KEY);
            }
            else {
                task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(mInputEditText.getText().toString(), user.getFirebaseToken()), Constants.SERVER_KEY);
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
            int id = jsonObject.getInt("success");
            if (id == 1) {
                ToastUtils.showShortSafe("Sent");
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mInputEditText.setText("");
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
    public void onTaskCompleted(String response, int requestId) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == SEND_MESSAGE) {
            retrieveFromJSONSendMessage(response);
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }

    @Subscribe
    public void onMuscleControlLeftEvents(MuscleControlLeftEvents event) {
        if (event != null && event.getFragmentId() == DIALOG_FRAGMENT_ID) {
            if (currentPage == 1) {
                if (paSelections.get(muscleControlRightPaCount).getId() == 1) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScrollLeftBtn.performClick();
                        }
                    });
                } else if (paSelections.get(muscleControlRightPaCount).getId() == 2) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScrollRightBtn.performClick();
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
                            mScrollLeftBtn.performClick();
                        }
                    });
                } else if (t2Selections.get(muscleControlRightT2Count).getId() == 2) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScrollRightBtn.performClick();
                        }
                    });
                } else if (t2Selections.get(muscleControlRightT2Count).getId() >= 3 && t2Selections.get(muscleControlRightT2Count).getId() <= 8) {
                    EventBusActivityScope.getDefault(_mActivity).post(new ConfirmT2KeyboardEvent(t2Selections.get(muscleControlRightT2Count).getId()));
                } else if (t2Selections.get(muscleControlRightT2Count).getId() == 9) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            inputBackspaceBtn.performClick();
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
                            mScrollLeftBtn.performClick();
                        }
                    });
                } else if (t26Selections.get(muscleControlRightT26Count).getId() == 2) {
                    _mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScrollRightBtn.performClick();
                        }
                    });
                }
            }
        }
    }

    @Subscribe
    public void onMuscleControlRightEvents(MuscleControlRightEvents event) {
        if (event != null && event.getFragmentId() == DIALOG_FRAGMENT_ID) {
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
