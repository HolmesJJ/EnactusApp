package com.example.enactusapp.Fragment.Dialog.Child;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private static final String DICTIONARY_1 = "dict1.txt";
    private static final String DICTIONARY_2 = "dict2.txt";
    private static final String DICTIONARY_3 = "dict3.txt";
    private static final String DICTIONARY_4 = "dict4.txt";
    private static final String DICTIONARY_5 = "dict5.txt";

    private RecyclerView mDialogPossibleWordsRecyclerView;
    private DialogPossibleWordsAdapter mDialogPossibleWordsAdapter;

    private List<String> possibleWordsList = new ArrayList<>();
    private Button t2KeyboardLeftBtn;
    private Button t2KeyboardRightBtn;
    private Button t2keyboardBackBtn;
    private Button t2KeyboardSendBtn;
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
                possibleWords(inputTv.getText().toString());
            }
        });

        t2KeyboardLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputText = inputText + "L";
                inputTv.setText(inputText);
            }
        });

        t2KeyboardRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputText = inputText + "R";
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
                    inputText = inputText.substring(0, inputText.length()-1);
                    inputTv.setText(inputText);
                }
            }
        });
    }

    private void possibleWords(String keys) {
        possibleWordsList.clear();
        possibleWordsList.addAll(readDictionary(DICTIONARY_5, keys, 20));
        if (possibleWordsList.size() <= 20) {
            possibleWordsList.addAll(readDictionary(DICTIONARY_4, keys, 20 - possibleWordsList.size()));
        }
        if (possibleWordsList.size() <= 20) {
            possibleWordsList.addAll(readDictionary(DICTIONARY_3, keys, 20 - possibleWordsList.size()));
        }
        if (possibleWordsList.size() <= 20) {
            possibleWordsList.addAll(readDictionary(DICTIONARY_2, keys, 20 - possibleWordsList.size()));
        }
        if (possibleWordsList.size() <= 20) {
            possibleWordsList.addAll(readDictionary(DICTIONARY_1, keys, 10 - possibleWordsList.size()));
        }
        mDialogPossibleWordsAdapter = new DialogPossibleWordsAdapter(_mActivity, possibleWordsList);
        mDialogPossibleWordsRecyclerView.setAdapter(mDialogPossibleWordsAdapter);
        mDialogPossibleWordsAdapter.setOnItemClickListener(this);
    }

    private List<String> readDictionary(String fileName, String keys, int number) {
        List<String> wordsList = new ArrayList<>();
        BufferedReader reader = null;
        String L = "abcdefghijklm";
        String R = "nopqrstuvwxyz";

        try {
            reader = new BufferedReader(new InputStreamReader(ContextUtils.getContext().getAssets().open(fileName)));
            String mLine;
            while ((mLine = reader.readLine()) != null && wordsList.size() <= number) {

                // 符合长度
                if (mLine.length() == keys.length()) {

                    // 判断word的每个字母是否符合每一个key
                    char[] keyArr = keys.toCharArray();
                    char[] letterArr = mLine.toCharArray();

                    int count = 0;
                    for (int i = 0; i < keys.length(); i++) {
                        String key = Character.toString(keyArr[i]).toUpperCase();
                        String letter = Character.toString(letterArr[i]).toLowerCase();

                        if ((key.equals("L") && L.contains(letter)) || key.equals("R") && R.contains(letter)) {
                            count++;
                        } else {
                            break;
                        }
                    }

                    // 全部字母都符合要求
                    if (count == keys.length()) {
                        wordsList.add(mLine);
                    }
                }
            }
        } catch (IOException e) {
            e.fillInStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.fillInStackTrace();
                }
            }
        }
        return wordsList;
    }

    @Override
    public void onItemClick(int position) {
        EventBusActivityScope.getDefault(_mActivity).post(new PossibleWordEvent(possibleWordsList.get(position) + " "));
        inputText = "";
        inputTv.setText(inputText);
    }

    @Subscribe
    public void onBlinkEvent(BlinkEvent event) {
        if(event.isLeftEye()) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inputText = inputText + "R";
                    inputTv.setText(inputText);
                    possibleWords(inputTv.getText().toString());
                }
            });
        }
        else {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inputText = inputText + "L";
                    inputTv.setText(inputText);
                    possibleWords(inputTv.getText().toString());
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
