package com.example.enactusapp.Fragment.Dialog;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.enactusapp.Adapter.CustomViewPager;
import com.example.enactusapp.Adapter.DialogChildAdapter;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.MessageToPossibleAnswersEvent;
import com.example.enactusapp.Event.MessageEvent;
import com.example.enactusapp.Event.PossibleWordEvent;
import com.example.enactusapp.Event.RequireMessageEvent;
import com.example.enactusapp.Event.SpeakPossibleAnswersEvent;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.TTS.TTSHelper;
import com.example.enactusapp.Utils.ToastUtils;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class DialogFragment extends SupportFragment implements OnTaskCompleted {

    private static final int SEND_MESSAGE = 1;

    private Toolbar mToolbar;
    private ProgressBar mPbLoading;
    private TextView mMessageTextView;
    private TextView mPossibleAnswers;
    private EditText mInputEditText;
    private ImageView mScrollLeftBtn;
    private ImageView mScrollRightBtn;
    private ImageButton inputBackspaceBtn;

    private CustomViewPager dialogAnswerContainerViewPager;

    private User user;
    private String message;

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
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.dialog);
        mPbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
        mMessageTextView = (TextView) view.findViewById(R.id.message_tv);
        mPossibleAnswers = (TextView) view.findViewById(R.id.possible_answers);
        mInputEditText = (EditText) view.findViewById(R.id.input_et);
        dialogAnswerContainerViewPager = (CustomViewPager) view.findViewById(R.id.dialog_answer_container);
        dialogAnswerContainerViewPager.setScanScroll(false);
        mScrollLeftBtn = (ImageView) view.findViewById(R.id.scroll_left_btn);
        mScrollRightBtn = (ImageView) view.findViewById(R.id.scroll_right_btn);
        inputBackspaceBtn = (ImageButton) view.findViewById(R.id.input_backspace_btn);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {

        inputBackspaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mInputEditText.length() >= 1) {
                    String inputEditText = mInputEditText.getText().toString().substring(0, mInputEditText.getText().toString().length()-1);
                    mInputEditText.setText(inputEditText);
                    mInputEditText.setSelection(mInputEditText.length());
                }
            }
        });

        dialogAnswerContainerViewPager.setAdapter(new DialogChildAdapter(getChildFragmentManager(),
                getString(R.string.possibleAnswers),
                getString(R.string.t2keyboard),
                getString(R.string.t26keyboard)));

        mScrollLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(dialogAnswerContainerViewPager.getCurrentItem() == 0) {
                    dialogAnswerContainerViewPager.setCurrentItem(2);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    mInputEditText.requestFocus();
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
                    mInputEditText.requestFocus();
                    inputBackspaceBtn.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
            }
        });

        mScrollRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(dialogAnswerContainerViewPager.getCurrentItem() == 0) {
                    dialogAnswerContainerViewPager.setCurrentItem(1);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    mInputEditText.requestFocus();
                    inputBackspaceBtn.setVisibility(View.VISIBLE);
                    InputMethodManager imm = (InputMethodManager) _mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
                else if(dialogAnswerContainerViewPager.getCurrentItem() == 1) {
                    dialogAnswerContainerViewPager.setCurrentItem(2);
                    mPossibleAnswers.setVisibility(View.INVISIBLE);
                    mInputEditText.setVisibility(View.VISIBLE);
                    mInputEditText.requestFocus();
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
        }
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        user = event.getUser();
        message = event.getMessage();
        mMessageTextView.setText(message);
        EventBusActivityScope.getDefault(_mActivity).post(new MessageToPossibleAnswersEvent(user, message));
    }

    @Subscribe
    public void onRequireMessageEvent(RequireMessageEvent event) {
        if (!TextUtils.isEmpty(message)) {
            EventBusActivityScope.getDefault(_mActivity).post(new MessageToPossibleAnswersEvent(user, message));
        }
    }

    @Subscribe
    public void onPossibleWordEvent(PossibleWordEvent event) {
        String newText = mInputEditText.getText().toString() + event.getPossibleWord();
        mInputEditText.setText(newText);
        mInputEditText.setSelection(mInputEditText.length());
    }

    @Subscribe
    public void onSpeakPossibleAnswersEvent(SpeakPossibleAnswersEvent event) {
        if (!TextUtils.isEmpty(event.getAnswer())) {
            TTSHelper.getInstance().speak(event.getAnswer());
        } else {
            TTSHelper.getInstance().speak(mInputEditText.getText().toString());
        }
        if (user == null) {
            mInputEditText.setText("");
        } else {
            showProgress(true);
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(DialogFragment.this, SEND_MESSAGE);
            if(!TextUtils.isEmpty(event.getAnswer())) {
                task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(event.getAnswer(), user.getFirebaseToken()), Constants.SERVER_KEY);
            }
            else {
                task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(mInputEditText.getText().toString(), user.getFirebaseToken()), Constants.SERVER_KEY);
            }
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
        showProgress(false);
        if (requestId == SEND_MESSAGE) {
            retrieveFromJSONSendMessage(response);
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            mPbLoading.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
