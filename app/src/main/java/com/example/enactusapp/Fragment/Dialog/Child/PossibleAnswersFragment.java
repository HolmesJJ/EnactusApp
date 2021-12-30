package com.example.enactusapp.Fragment.Dialog.Child;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.enactusapp.Adapter.DialogPossibleAnswersAdapter;
import com.example.enactusapp.Constants.ChatHistory;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Event.MessageEvent.SendMessageEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.ConfirmPossibleAnswerEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.GeneratePossibleAnswersEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.PossibleAnswersEvent;
import com.example.enactusapp.Event.PossibleAnswerEvent.SelectPossibleAnswerEvent;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;

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

public class PossibleAnswersFragment extends SupportFragment implements OnItemClickListener, OnTaskCompleted {

    private static final String TAG = PossibleAnswersFragment.class.getSimpleName();

    private static final int GET_SMART_ANSWERS = 1;

    private RecyclerView mDialogPossibleAnswersRecyclerView;
    private GifImageView mGivLoading;
    private DialogPossibleAnswersAdapter mDialogPossibleAnswersAdapter;

    private final List<String> possibleAnswersList = new ArrayList<>();
    private final SmartReplyGenerator smartReply = SmartReply.getClient();

    // 上一次选中的位置
    private int lastSelectedPosition = -1;
    // 上一次选中的答案
    private String lastSelectedAnswer = "";

    private boolean isGeneratingAnswersFromGoogle = false;
    private boolean isGeneratingAnswersFromMicrosoft = false;

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
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ContextUtils.getContext());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mDialogPossibleAnswersRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mDialogPossibleAnswersRecyclerView.setLayoutManager(linearLayoutManager);
        mDialogPossibleAnswersRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
        // 无论用户是否已经发送过回答，都会自动生成一个回答
        mGivLoading.setVisibility(View.VISIBLE);
        if (!isGeneratingAnswersFromGoogle && !isGeneratingAnswersFromMicrosoft) {
            qnaAnswers();
        }
    }

    private void initDelayView() {
        mDialogPossibleAnswersAdapter = new DialogPossibleAnswersAdapter(ContextUtils.getContext(), possibleAnswersList);
        mDialogPossibleAnswersRecyclerView.setAdapter(mDialogPossibleAnswersAdapter);
        mDialogPossibleAnswersAdapter.setOnItemClickListener(this);
    }

    private void qnaAnswers() {
        Log.i(TAG, "ChatHistory Size: " + ChatHistory.CONVERSATIONS.size());
        if (ChatHistory.CONVERSATIONS.size() == 0) {
            mGivLoading.setVisibility(View.VISIBLE);
            return;
        }
        String lastMessage = ChatHistory.CONVERSATIONS.get(ChatHistory.CONVERSATIONS.size() - 1).zzb();
        Log.i(TAG, "Last Message: " + lastMessage);
        if (TextUtils.isEmpty(lastMessage)) {
            mGivLoading.setVisibility(View.VISIBLE);
            return;
        }
        possibleAnswersList.clear();
        // Google
        Log.i(TAG, "Q&A from Google");
        if (!isGeneratingAnswersFromGoogle) {
            isGeneratingAnswersFromGoogle = true;
            smartReply.suggestReplies(ChatHistory.CONVERSATIONS)
                    .addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                        @Override
                        public void onSuccess(SmartReplySuggestionResult result) {
                            Log.i(TAG, "Q&A from Google: " + result.getStatus());
                            if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                                for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                                    Log.i(TAG, "Q&A from Google: " + suggestion.getText());
                                    possibleAnswersList.add(suggestion.getText());
                                    _mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mDialogPossibleAnswersAdapter.notifyItemInserted(possibleAnswersList.size() - 1);
                                        }
                                    });
                                }
                                lastSelectedPosition = -1;
                                lastSelectedAnswer = "";
                                EventBusActivityScope.getDefault(_mActivity).post(new PossibleAnswersEvent(possibleAnswersList));
                                if (result.getSuggestions().size() == 0) {
                                    // 刷新历史记录
                                    ChatHistory.CONVERSATIONS.clear();
                                    ToastUtils.showShortSafe("No Reply");
                                }
                            } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                                // 刷新历史记录
                                ChatHistory.CONVERSATIONS.clear();
                                ToastUtils.showShortSafe("Not Supported Language");
                            } else {
                                // 刷新历史记录
                                ChatHistory.CONVERSATIONS.clear();
                                ToastUtils.showShortSafe("No Reply");
                            }
                            isGeneratingAnswersFromGoogle = false;
                            if (!isGeneratingAnswersFromMicrosoft) {
                                mGivLoading.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            ToastUtils.showShortSafe("Answers Generation Failed!");
                            isGeneratingAnswersFromGoogle = false;
                            if (!isGeneratingAnswersFromMicrosoft) {
                                mGivLoading.setVisibility(View.GONE);
                            }
                        }
                    });
        }

        // Microsoft
        Log.i(TAG, "Q&A from Microsoft");
        String jsonData = convertToJSONGetSmartAnswers(lastMessage);
        if (!isGeneratingAnswersFromMicrosoft) {
            isGeneratingAnswersFromMicrosoft = true;
            HttpAsyncTaskPost task1 = new HttpAsyncTaskPost(PossibleAnswersFragment.this, GET_SMART_ANSWERS);
            task1.execute(Constants.SMART_ANSWERING_IP_ADDRESS, jsonData, Constants.SMART_ANSWERING_TOKEN);
        }
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
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDialogPossibleAnswersAdapter.notifyItemInserted(possibleAnswersList.size() - 1);
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void onGeneratePossibleAnswersEvent(GeneratePossibleAnswersEvent event) {
        mGivLoading.setVisibility(View.VISIBLE);
        if (!isGeneratingAnswersFromGoogle && !isGeneratingAnswersFromMicrosoft) {
            qnaAnswers();
        }
    }

    @Subscribe
    public void onSelectPossibleAnswerEvent(SelectPossibleAnswerEvent event) {
        if (lastSelectedPosition != -1) {
            possibleAnswersList.set(lastSelectedPosition, lastSelectedAnswer);
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDialogPossibleAnswersAdapter.notifyItemChanged(lastSelectedPosition);
                }
            });
        }
        lastSelectedPosition = event.getPosition();
        lastSelectedAnswer = possibleAnswersList.get(event.getPosition());
        possibleAnswersList.set(event.getPosition(), possibleAnswersList.get(event.getPosition()) + " *");
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialogPossibleAnswersAdapter.notifyItemChanged(event.getPosition());
            }
        });
    }

    @Subscribe
    public void onConfirmPossibleAnswerEvent(ConfirmPossibleAnswerEvent event) {
        EventBusActivityScope.getDefault(_mActivity).post(new SendMessageEvent(possibleAnswersList.get(event.getPosition())));
    }

    @Override
    public void onItemClick(int position) {
        if (lastSelectedPosition != -1) {
            possibleAnswersList.set(lastSelectedPosition, lastSelectedAnswer);
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDialogPossibleAnswersAdapter.notifyItemChanged(lastSelectedPosition);
                }
            });
        }
        lastSelectedPosition = position;
        lastSelectedAnswer = possibleAnswersList.get(position);
        possibleAnswersList.set(position, possibleAnswersList.get(position) + " *");
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialogPossibleAnswersAdapter.notifyItemChanged(position);
            }
        });
        EventBusActivityScope.getDefault(_mActivity).post(new SendMessageEvent(possibleAnswersList.get(position)));
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onTaskCompleted(String response, int requestId, String... others) {
        if (requestId == GET_SMART_ANSWERS) {
            retrieveFromJSONGetSmartAnswers(response);
            isGeneratingAnswersFromMicrosoft = false;
        }
        lastSelectedPosition = -1;
        lastSelectedAnswer = "";
        EventBusActivityScope.getDefault(_mActivity).post(new PossibleAnswersEvent(possibleAnswersList));
        if (!isGeneratingAnswersFromGoogle && !isGeneratingAnswersFromMicrosoft) {
            mGivLoading.setVisibility(View.GONE);
        }
    }
}
