package com.example.enactusapp.Fragment.Dialog;


import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.enactusapp.Adapter.CustomViewPager;
import com.example.enactusapp.Adapter.DialogChildAdapter;
import com.example.enactusapp.CustomView.CustomToast;
import com.example.enactusapp.Entity.MessageEvent;
import com.example.enactusapp.Entity.PossibleWordEvent;
import com.example.enactusapp.Entity.SpeakPossibleAnswersEvent;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.SharedPreferences.GetSetSharedPreferences;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.Locale;

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

    private Toolbar mToolbar;
    private TextView mMessageTextView;
    private TextView mPossibleAnswers;
    private EditText mInputEditText;
    private ImageView mScrollLeftBtn;
    private ImageView mScrollRightBtn;
    private ImageButton inputBackspaceBtn;

    private CustomViewPager dialogAnswerContainerViewPager;

    private TextToSpeech mTextToSpeech;

    private int isSucceeded = 0;
    private static final String FIREBASE_TOKEN = "e1tXvYDdr9M:APA91bE8x-VWP0QzInyLJ92_pD4KO96csJbnh5QaiQ1pxe2uiOBwaj8NQgtRs9ogdqdhgZrT6_ydEWe-VTQKvhpZLOeiUB8BMfZpe9gD2SD90hBdzJ569NLF9ClhXvM2aYPkluYe8i9T";

    public static DialogFragment newInstance(){
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
    }

    private void speak(String speakText) {
        mTextToSpeech.setPitch(0.5f);
        mTextToSpeech.setPitch(0.5f);
        mTextToSpeech.speak(speakText, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        mMessageTextView.setText(event.getMessage());
        GetSetSharedPreferences.setDefaults("message", event.getMessage(), _mActivity);
    }

    @Subscribe
    public void onPossibleWordEvent(PossibleWordEvent event) {
        String newText = mInputEditText.getText().toString() + event.getPossibleWord();
        mInputEditText.setText(newText);
        mInputEditText.setSelection(mInputEditText.length());
    }

    @Subscribe
    public void onSpeakPossibleAnswersEvent(SpeakPossibleAnswersEvent event) {
        if(GetSetSharedPreferences.getDefaults("ChatWithDisabled", _mActivity) != null) {
            GetSetSharedPreferences.removeDefaults("ChatWithDisabled", _mActivity);
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(DialogFragment.this);
            if(event.getMessage() != null) {
                task.execute("https://fcm.googleapis.com/fcm/send", convertToJSON(event.getMessage()));
            }
            else {
                task.execute("https://fcm.googleapis.com/fcm/send", convertToJSON(mInputEditText.getText().toString()));
            }
        }
        else {
            if(event.getMessage() != null) {
                speak(event.getMessage());
            }
            else {
                speak(mInputEditText.getText().toString());
            }
            mInputEditText.setText("");
        }
    }

    public String convertToJSON(String message) {
        JSONObject jsonMsg = new JSONObject();
        JSONObject content = new JSONObject();
        try {
            content.put("title", "message");
            content.put("body", message);
            jsonMsg.put("to", FIREBASE_TOKEN);
            jsonMsg.put("notification", content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    public void retrieveFromJSON(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            isSucceeded = jsonObject.getInt("success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskCompleted(String response) {
        retrieveFromJSON(response);

        // if response is from upload request
        if (isSucceeded == 1){
            CustomToast.show(_mActivity, "Sent!");
        }
        else {
            CustomToast.show(_mActivity, "Failed to send!");
        }

        isSucceeded = 0;
    }

    @Override
    public void onDestroyView() {
        if(mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
