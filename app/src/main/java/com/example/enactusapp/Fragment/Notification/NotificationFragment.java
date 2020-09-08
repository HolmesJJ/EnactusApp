package com.example.enactusapp.Fragment.Notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.enactusapp.Entity.StartChatEvent;
import com.example.enactusapp.R;
import com.example.enactusapp.SharedPreferences.GetSetSharedPreferences;

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
public class NotificationFragment extends SupportFragment {

    private Toolbar mToolbar;
    private ImageView notificationIv;
    private TextView notificationTv;
    private Button cancelBtn;
    private Button startChattingBtn;

    public static NotificationFragment newInstance(){
        NotificationFragment fragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification,container,false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.notification);
        notificationIv = (ImageView) view.findViewById(R.id.notification_iv);
        notificationTv = (TextView) view.findViewById(R.id.notification_tv);
        cancelBtn = (Button) view.findViewById(R.id.cancel_btn);
        startChattingBtn = (Button) view.findViewById(R.id.start_chatting_btn);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _mActivity.pop();
            }
        });
        startChattingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBusActivityScope.getDefault(_mActivity).post(new StartChatEvent(true));
                _mActivity.pop();
            }
        });

        if(GetSetSharedPreferences.getDefaults("nric", _mActivity).equals("A1234567B")) {
            notificationIv.setImageResource(_mActivity.getResources().getIdentifier("user2", "drawable", _mActivity.getPackageName()));
            notificationTv.setText("Mr.Chai");
        }
        else {
            notificationIv.setImageResource(_mActivity.getResources().getIdentifier("user1", "drawable", _mActivity.getPackageName()));
            notificationTv.setText("Mr.Wong");
        }
    }
}
