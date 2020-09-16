package com.example.enactusapp.Fragment.Dialog.Child;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.enactusapp.Adapter.DialogPossibleAnswersAdapter;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.MessageToPossibleAnswersEvent;
import com.example.enactusapp.Event.RequireMessageEvent;
import com.example.enactusapp.Event.SpeakPossibleAnswersEvent;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.Subscribe;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class PossibleAnswersFragment extends SupportFragment implements OnItemClickListener {

    private RecyclerView mDialogPossibleAnswersRecyclerView;
    private DialogPossibleAnswersAdapter mDialogPossibleAnswersAdapter;

    private User user;
    private String message;
    private List<String> possibleAnswersList = new ArrayList<>();

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
        if (!TextUtils.isEmpty(message)) {
            qnaAnswers(message);
        }
        EventBusActivityScope.getDefault(_mActivity).post(new RequireMessageEvent());
    }

    private void qnaAnswers(String message) {
        possibleAnswersList.clear();
        if(message.contains("how are you")) {
            possibleAnswersList.add("I am fine! And you?");
            possibleAnswersList.add("Very well, thanks!");
            possibleAnswersList.add("I am hanging here.");
            possibleAnswersList.add("Not bad.");
            possibleAnswersList.add("I have been better.");
        }
        if(message.contains("i am fine")) {
            possibleAnswersList.add("I am ok! Thanks!");
            possibleAnswersList.add("Me too! Thanks!");
            possibleAnswersList.add("Same! Thanks!");
        }
        if(message.contains("bad")) {
            possibleAnswersList.add("Why?");
            possibleAnswersList.add("What happen?");
            possibleAnswersList.add("Are you not well?");
        }
        if(message.contains("what happen")) {
            possibleAnswersList.add("Yes, I am sick.");
            possibleAnswersList.add("Someone make feel uncomfortable.");
            possibleAnswersList.add("I have some family problem.");
        }
        mDialogPossibleAnswersAdapter = new DialogPossibleAnswersAdapter(_mActivity, possibleAnswersList);
        mDialogPossibleAnswersRecyclerView.setAdapter(mDialogPossibleAnswersAdapter);
        mDialogPossibleAnswersAdapter.setOnItemClickListener(this);
    }

    @Subscribe
    public void onMessageToPossibleAnswersEvent(MessageToPossibleAnswersEvent event) {
        user = event.getUser();
        message = event.getMessage().toLowerCase();
        qnaAnswers(message);
    }

    @Override
    public void onItemClick(int position) {
        EventBusActivityScope.getDefault(_mActivity).post(new SpeakPossibleAnswersEvent(possibleAnswersList.get(position)));
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
