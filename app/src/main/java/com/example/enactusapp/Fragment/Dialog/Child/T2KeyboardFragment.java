package com.example.enactusapp.Fragment.Dialog.Child;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.enactusapp.Adapter.DialogPossibleWordsAdapter;
import com.example.enactusapp.Event.BlinkEvent;
import com.example.enactusapp.Event.PossibleWordEvent;
import com.example.enactusapp.Event.SpeakPossibleAnswersEvent;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Markov.MarkovHelper;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.ContextUtils;

import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
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
public class T2KeyboardFragment extends SupportFragment implements OnItemClickListener {

    private RecyclerView mDialogPossibleWordsRecyclerView;
    private DialogPossibleWordsAdapter mDialogPossibleWordsAdapter;

    private List<String> possibleWordsList = new ArrayList<>();
    private Button t2KeyboardLeftBtn;
    private Button t2KeyboardRightBtn;
    private Button t2keyboardBackBtn;
    private Button t2KeyboardSendBtn;
    private Button t2KeyboardPrevBtn;
    private Button t2keyboardNextBtn;
    private TextView inputTv;

    private String inputText = "";

    public static T2KeyboardFragment newInstance() {
        Bundle args = new Bundle();
        T2KeyboardFragment fragment = new T2KeyboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t2keyboard, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mDialogPossibleWordsRecyclerView = (RecyclerView) view.findViewById(R.id.dialog_possible_words_recycler_view);
        t2KeyboardLeftBtn = (Button) view.findViewById(R.id.t2keyboard_left_button);
        t2KeyboardRightBtn = (Button) view.findViewById(R.id.t2keyboard_right_button);
        t2KeyboardSendBtn = (Button) view.findViewById(R.id.t2keyboard_send_button);
        t2keyboardBackBtn = (Button) view.findViewById(R.id.t2keyboard_back_button);
        t2KeyboardPrevBtn = (Button) view.findViewById(R.id.t2keyboard_prev_button);
        t2keyboardNextBtn = (Button) view.findViewById(R.id.t2keyboard_next_button);
        inputTv = (TextView) view.findViewById(R.id.input_tv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mDialogPossibleWordsRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mDialogPossibleWordsRecyclerView.setLayoutManager(linearLayoutManager);
        mDialogPossibleWordsRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {

        inputTv.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (inputTv.getText() != null) {
                    possibleWords(MarkovHelper.getInstance().getByPattern(inputTv.getText().toString()));
                } else {
                    possibleWords(null);
                }
            }
        });

        t2KeyboardLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputText = inputText + "l";
                inputTv.setText(inputText);
            }
        });

        t2KeyboardRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputText = inputText + "r";
                inputTv.setText(inputText);
            }
        });

        t2KeyboardSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBusActivityScope.getDefault(_mActivity).post(new SpeakPossibleAnswersEvent(null));
            }
        });

        t2keyboardBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(inputText.length() >= 1) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                    inputTv.setText(inputText);
                }
            }
        });

        t2KeyboardPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                possibleWords(MarkovHelper.getInstance().getPrePage());
            }
        });

        t2keyboardNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                possibleWords(MarkovHelper.getInstance().getNextPage());
            }
        });

        mDialogPossibleWordsAdapter = new DialogPossibleWordsAdapter(_mActivity, possibleWordsList);
        mDialogPossibleWordsRecyclerView.setAdapter(mDialogPossibleWordsAdapter);
        mDialogPossibleWordsAdapter.setOnItemClickListener(this);
    }

    private void possibleWords(List<String> words) {
        possibleWordsList.clear();
        possibleWordsList.addAll(words);
        mDialogPossibleWordsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(int position) {
        String word = MarkovHelper.getInstance().chooseWord(position + 1);
        EventBusActivityScope.getDefault(_mActivity).post(new PossibleWordEvent(word + " "));
        inputText = "";
        inputTv.setText(inputText);
        possibleWords(MarkovHelper.getInstance().getByPreWord());
    }

    @Subscribe
    public void onBlinkEvent(BlinkEvent event) {
        if(event.isLeftEye()) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inputText = inputText + "l";
                    inputTv.setText(inputText);
                    possibleWords(MarkovHelper.getInstance().getByPattern(inputTv.getText().toString()));
                }
            });
        }
        else {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inputText = inputText + "r";
                    inputTv.setText(inputText);
                    possibleWords(MarkovHelper.getInstance().getByPattern(inputTv.getText().toString()));
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
