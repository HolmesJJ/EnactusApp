package com.example.enactusapp.Fragment.Dialog.Child;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;

import androidx.annotation.NonNull;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class T26KeyboardFragment extends SupportFragment implements OnTaskCompleted {

    public static T26KeyboardFragment newInstance() {
        Bundle args = new Bundle();
        T26KeyboardFragment fragment = new T26KeyboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t26keyboard, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {

    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {

    }

    @Override
    public void onTaskCompleted(String response, int requestId) {

    }
}
