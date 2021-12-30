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
import com.example.enactusapp.Event.MessageEvent.SendMessageEvent;
import com.example.enactusapp.Event.PossibleWordEvent.ConfirmPossibleWordEvent;
import com.example.enactusapp.Event.PossibleWordEvent.PossibleWordEvent;
import com.example.enactusapp.Event.PossibleWordEvent.PossibleWordsEvent;
import com.example.enactusapp.Event.PossibleWordEvent.SelectPossibleWordEvent;
import com.example.enactusapp.Event.T2KeyboardEvent.ConfirmT2KeyboardEvent;
import com.example.enactusapp.Event.T2KeyboardEvent.SelectT2KeyboardEvent;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Markov.MarkovHelper;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.ContextUtils;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

public class T2KeyboardFragment extends SupportFragment implements OnItemClickListener {

    private static final int LEFT_BUTTON_ID = 3;
    private static final int RIGHT_BUTTON_ID = 4;
    private static final int SEND_ID = 5;
    private static final int BACK_ID = 6;
    private static final int PREV_ID = 7;
    private static final int NEXT_ID = 8;

    private RecyclerView mDialogPossibleWordsRecyclerView;
    private DialogPossibleWordsAdapter mDialogPossibleWordsAdapter;

    private final List<String> possibleWordsList = new ArrayList<>();
    private Button t2KeyboardLeftBtn;
    private Button t2KeyboardRightBtn;
    private Button t2keyboardBackBtn;
    private Button t2KeyboardSendBtn;
    private Button t2KeyboardPrevBtn;
    private Button t2keyboardNextBtn;
    private TextView inputTv;

    private String inputText = "";

    // 上一次选中的位置
    private int lastSelectedPosition = -1;
    // 上一次选中的单词
    private String lastSelectedWord = "";

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
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ContextUtils.getContext());
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
                EventBusActivityScope.getDefault(_mActivity).post(new SendMessageEvent());
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
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialogPossibleWordsAdapter.notifyDataSetChanged();
            }
        });
        lastSelectedPosition = -1;
        lastSelectedWord = "";
        EventBusActivityScope.getDefault(_mActivity).post(new PossibleWordsEvent(possibleWordsList));
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

    @Subscribe
    public void onSelectT2KeyboardEvent(SelectT2KeyboardEvent event) {

    }

    @Subscribe
    public void onConfirmT2KeyboardEvent(ConfirmT2KeyboardEvent event) {
        if (event.getKeyId() == LEFT_BUTTON_ID) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t2KeyboardLeftBtn.performClick();
                }
            });
        } else if (event.getKeyId() == RIGHT_BUTTON_ID) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t2KeyboardRightBtn.performClick();
                }
            });
        } else if (event.getKeyId() == SEND_ID) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t2KeyboardSendBtn.performClick();
                }
            });
        } else if (event.getKeyId() == BACK_ID) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t2keyboardBackBtn.performClick();
                }
            });
        } else if (event.getKeyId() == PREV_ID) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t2KeyboardPrevBtn.performClick();
                }
            });
        } else if (event.getKeyId() == NEXT_ID) {
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t2keyboardNextBtn.performClick();
                }
            });
        }
    }

    @Subscribe
    public void onSelectPossibleWordEvent(SelectPossibleWordEvent event) {
        if (lastSelectedPosition != -1) {
            possibleWordsList.set(lastSelectedPosition, lastSelectedWord);
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDialogPossibleWordsAdapter.notifyItemChanged(lastSelectedPosition);
                }
            });
        }
        lastSelectedPosition = event.getPosition();
        lastSelectedWord = possibleWordsList.get(event.getPosition());
        possibleWordsList.set(event.getPosition(), possibleWordsList.get(event.getPosition()) + " *");
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialogPossibleWordsAdapter.notifyItemChanged(event.getPosition());
            }
        });
    }

    @Subscribe
    public void onConfirmPossibleWordEvent(ConfirmPossibleWordEvent event) {
        String word = MarkovHelper.getInstance().chooseWord(event.getPosition() + 1);
        EventBusActivityScope.getDefault(_mActivity).post(new PossibleWordEvent(word + " "));
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                inputText = "";
                inputTv.setText(inputText);
                possibleWords(MarkovHelper.getInstance().getByPreWord());
            }
        });
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
