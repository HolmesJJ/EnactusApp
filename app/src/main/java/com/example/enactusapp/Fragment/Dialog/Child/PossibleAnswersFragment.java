package com.example.enactusapp.Fragment.Dialog.Child;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.enactusapp.Adapter.DialogPossibleAnswersAdapter;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.ClearChatHistoryEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.ConfirmPossibleAnswerEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.MessageToPossibleAnswersEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.PossibleAnswersEvent;
import com.example.enactusapp.Event.RequireMessageEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.SelectPossibleAnswerEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.SpeakPossibleAnswerEvent;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

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
public class PossibleAnswersFragment extends SupportFragment implements OnItemClickListener, OnTaskCompleted {

    private static final int GET_SMART_ANSWERS_1 = 1;
    private static final int GET_SMART_ANSWERS_2 = 2;

    private RecyclerView mDialogPossibleAnswersRecyclerView;
    private GifImageView mGivLoading;
    private DialogPossibleAnswersAdapter mDialogPossibleAnswersAdapter;

    private User user;
    private String message;

    private List<String> possibleAnswersList = new ArrayList<>();
    private List<FirebaseTextMessage> chatHistory = new ArrayList<>();
    private FirebaseSmartReply smartReply = FirebaseNaturalLanguage.getInstance().getSmartReply();

    // 上一次选中的位置
    private int lastSelectedPosition = -1;
    // 上一次选中的答案
    private String lastSelectedAnswer = "";

    public static PossibleAnswersFragment newInstance() {
        Bundle args = new Bundle();
        PossibleAnswersFragment fragment = new PossibleAnswersFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_possible_answers, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mDialogPossibleAnswersRecyclerView = (RecyclerView) view.findViewById(R.id.dialog_possible_answers_recycler_view);
        mGivLoading = (GifImageView) view.findViewById(R.id.giv_loading);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mDialogPossibleAnswersRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mDialogPossibleAnswersRecyclerView.setLayoutManager(linearLayoutManager);
        mDialogPossibleAnswersRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        mDialogPossibleAnswersAdapter = new DialogPossibleAnswersAdapter(_mActivity, possibleAnswersList);
        mDialogPossibleAnswersRecyclerView.setAdapter(mDialogPossibleAnswersAdapter);
        mDialogPossibleAnswersAdapter.setOnItemClickListener(this);
        EventBusActivityScope.getDefault(_mActivity).post(new RequireMessageEvent());
    }

    private void qnaAnswers() {
        possibleAnswersList.clear();
        // Google
        smartReply.suggestReplies(chatHistory)
                .addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                    @Override
                    public void onSuccess(SmartReplySuggestionResult smartReplySuggestionResult) {
                        if (smartReplySuggestionResult.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                            for (SmartReplySuggestion suggestion : smartReplySuggestionResult.getSuggestions()) {
                                possibleAnswersList.add(suggestion.getText());
                            }
                            lastSelectedPosition = -1;
                            lastSelectedAnswer = "";
                            EventBusActivityScope.getDefault(_mActivity).post(new PossibleAnswersEvent(possibleAnswersList));
                            if (smartReplySuggestionResult.getSuggestions().size() == 0) {
                                chatHistory.clear();
                                EventBusActivityScope.getDefault(_mActivity).post(new ClearChatHistoryEvent());
                            } else {
                                mDialogPossibleAnswersAdapter.notifyDataSetChanged();
                                mGivLoading.setVisibility(View.GONE);
                            }
                        } else {
                            ToastUtils.showShortSafe("Answer generation fail!");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ToastUtils.showShortSafe("Answer generation fail!");
                    }
                });

        // Microsoft
        String jsonData = convertToJSONGetSmartAnswers(message);
        HttpAsyncTaskPost task1 = new HttpAsyncTaskPost(PossibleAnswersFragment.this, GET_SMART_ANSWERS_1);
        task1.execute(Constants.SMART_ANSWERING_IP_ADDRESS_1, jsonData, Constants.SMART_ANSWERING_TOKEN_1);
        HttpAsyncTaskPost task2 = new HttpAsyncTaskPost(PossibleAnswersFragment.this, GET_SMART_ANSWERS_2);
        task2.execute(Constants.SMART_ANSWERING_IP_ADDRESS_2, jsonData, Constants.SMART_ANSWERING_TOKEN_2);
    }

    private String convertToJSONGetSmartAnswers(String question) {
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("question", question);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONGetSmartAnswers(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            String answers = jsonObject.getString("answers");
            JSONArray jsonArrayAnswers = new JSONArray(answers);
            JSONObject jsonObjectAnswer = new JSONObject(jsonArrayAnswers.getString(0));
            int answerId = jsonObjectAnswer.getInt("id");
            String answer = jsonObjectAnswer.getString("answer");
            if (answerId != -1) {
                possibleAnswersList.add(answer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void onMessageToPossibleAnswersEvent(MessageToPossibleAnswersEvent event) {
        user = event.getUser();
        message = event.getMessage().toLowerCase();
        chatHistory.clear();
        chatHistory.addAll(event.getChatHistory());
        mGivLoading.setVisibility(View.VISIBLE);
        qnaAnswers();
    }

    @Subscribe
    public void onSelectPossibleAnswerEvent(SelectPossibleAnswerEvent event) {
        if (lastSelectedPosition != -1) {
            possibleAnswersList.set(lastSelectedPosition, lastSelectedAnswer);
        }
        lastSelectedPosition = event.getPosition();
        lastSelectedAnswer = possibleAnswersList.get(event.getPosition());
        possibleAnswersList.set(event.getPosition(), possibleAnswersList.get(event.getPosition()) + " *");
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialogPossibleAnswersAdapter.notifyDataSetChanged();
            }
        });
    }

    @Subscribe
    public void onConfirmPossibleAnswerEvent(ConfirmPossibleAnswerEvent event) {
        EventBusActivityScope.getDefault(_mActivity).post(new SpeakPossibleAnswerEvent(possibleAnswersList.get(event.getPosition())));
    }

    @Override
    public void onItemClick(int position) {
        if (lastSelectedPosition != -1) {
            possibleAnswersList.set(lastSelectedPosition, lastSelectedAnswer);
        }
        lastSelectedPosition = position;
        lastSelectedAnswer = possibleAnswersList.get(position);
        possibleAnswersList.set(position, possibleAnswersList.get(position) + " *");
        mDialogPossibleAnswersAdapter.notifyDataSetChanged();
        EventBusActivityScope.getDefault(_mActivity).post(new SpeakPossibleAnswerEvent(possibleAnswersList.get(position)));
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onTaskCompleted(String response, int requestId) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == GET_SMART_ANSWERS_1) {
            retrieveFromJSONGetSmartAnswers(response);
            mDialogPossibleAnswersAdapter.notifyDataSetChanged();
        }
        if (requestId == GET_SMART_ANSWERS_2) {
            retrieveFromJSONGetSmartAnswers(response);
            mDialogPossibleAnswersAdapter.notifyDataSetChanged();
        }
        lastSelectedPosition = -1;
        lastSelectedAnswer = "";
        EventBusActivityScope.getDefault(_mActivity).post(new PossibleAnswersEvent(possibleAnswersList));
    }
}
