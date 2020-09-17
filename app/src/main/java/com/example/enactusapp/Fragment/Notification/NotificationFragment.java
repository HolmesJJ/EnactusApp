package com.example.enactusapp.Fragment.Notification;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.MessageEvent;
import com.example.enactusapp.Event.MessageToPossibleAnswersEvent;
import com.example.enactusapp.Event.StartChatEvent;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.R;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;

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
    private ImageView mIvThumbnail;
    private TextView mTvName;
    private TextView mTvMessage;
    private Button startChatBtn;
    private Button cancelBtn;

    private User user;
    private String message;

    public static NotificationFragment newInstance() {
        NotificationFragment fragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.notification);
        mIvThumbnail = (ImageView) view.findViewById(R.id.iv_thumbnail);
        mTvName = (TextView) view.findViewById(R.id.tv_name);
        mTvMessage = (TextView) view.findViewById(R.id.tv_message);
        startChatBtn = (Button) view.findViewById(R.id.start_chat_btn);
        cancelBtn = (Button) view.findViewById(R.id.cancel_btn);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        startChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user != null && !TextUtils.isEmpty(message)) {
                    String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + user.getId() + ".jpg";
                    EventBusActivityScope.getDefault(_mActivity).post(new StartChatEvent(user));
                }
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainFragment) getParentFragment()).hideNotificationFragment();
            }
        });
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        user = event.getUser();
        message = event.getMessage();
        if (user != null && !TextUtils.isEmpty(message)) {
            String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + user.getId() + ".jpg";
            Glide.with(this).load(thumbnail).into(mIvThumbnail);
            mTvName.setText(user.getName());
            mTvMessage.setText(message);
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }
}
