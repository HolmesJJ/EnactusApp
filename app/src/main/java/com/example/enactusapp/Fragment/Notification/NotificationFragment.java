package com.example.enactusapp.Fragment.Notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.StartChatEvent;
import com.example.enactusapp.R;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

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

    public static final String ID = "id";
    public static final String USERNAME = "username";
    public static final String NAME = "name";
    public static final String FIREBASE_TOKEN = "firebaseToken";
    public static final String MESSAGE = "message";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";

    private Toolbar mToolbar;
    private ImageView mIvThumbnail;
    private TextView mTvName;
    private TextView mTvMessage;
    private Button cancelBtn;
    private Button startChattingBtn;

    private int id;
    private String username;
    private String name;
    private String firebaseToken;
    private String message;
    private double longitude;
    private double latitude;

    public static NotificationFragment newInstance(int id, String username, String name, String firebaseToken, String message, double longitude, double latitude) {
        NotificationFragment fragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ID, id);
        bundle.putString(USERNAME, username);
        bundle.putString(NAME, name);
        bundle.putString(FIREBASE_TOKEN, firebaseToken);
        bundle.putString(MESSAGE, message);
        bundle.putDouble(LONGITUDE, longitude);
        bundle.putDouble(LATITUDE, latitude);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        initView(view);
        initData();
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.notification);
        mIvThumbnail = (ImageView) view.findViewById(R.id.iv_thumbnail);
        mTvName = (TextView) view.findViewById(R.id.tv_name);
        mTvMessage = (TextView) view.findViewById(R.id.tv_message);
        cancelBtn = (Button) view.findViewById(R.id.cancel_btn);
        startChattingBtn = (Button) view.findViewById(R.id.start_chatting_btn);
    }

    private void initData() {
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            id = bundle.getInt(ID, -1);
            username = bundle.getString(USERNAME);
            name = bundle.getString(NAME);
            firebaseToken = bundle.getString(FIREBASE_TOKEN);
            message = bundle.getString(MESSAGE);
            longitude = bundle.getDouble(LONGITUDE);
            latitude = bundle.getDouble(LATITUDE);
        }
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
                String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + id + ".jpg";
                EventBusActivityScope.getDefault(_mActivity).post(new StartChatEvent(new User(id, username, name, thumbnail, firebaseToken, longitude, latitude)));
                _mActivity.pop();
            }
        });
        if (id > 0) {
            String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + id + ".jpg";
            Glide.with(this).load(thumbnail).into(mIvThumbnail);
            mTvName.setText(name);
            mTvMessage.setText(message);
        }
    }
}
