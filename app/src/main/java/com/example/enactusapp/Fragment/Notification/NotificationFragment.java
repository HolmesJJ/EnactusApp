package com.example.enactusapp.Fragment.Notification;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Entity.Selection;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.ChatEvent.StopChatEvent;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Event.NotificationEvent;
import com.example.enactusapp.Event.ChatEvent.StartChatEvent;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.R;
import com.shehuan.niv.NiceImageView;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    private static final int NOTIFICATION_FRAGMENT_ID = 99;
    private static final String TAG = "NotificationFragment";

    private TextView mTvSelection;
    private NiceImageView mNivThumbnail;
    private TextView mTvName;
    private TextView mTvMessage;
    private Button startChatBtn;
    private Button cancelBtn;

    private User user;
    private String message;

    private List<Selection> selections = new ArrayList<>();
    private int muscleControlRightCount = 0;

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
        mTvSelection = (TextView) view.findViewById(R.id.tv_selection);
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            mTvSelection.setVisibility(View.GONE);
        } else {
            mTvSelection.setVisibility(View.VISIBLE);
        }
        mNivThumbnail = (NiceImageView) view.findViewById(R.id.iv_thumbnail);
        mTvName = (TextView) view.findViewById(R.id.tv_name);
        mTvMessage = (TextView) view.findViewById(R.id.tv_message);
        startChatBtn = (Button) view.findViewById(R.id.btn_start_chat);
        cancelBtn = (Button) view.findViewById(R.id.btn_cancel);
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
                    String thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + user.getId() + ".jpg";
                    EventBusActivityScope.getDefault(_mActivity).post(new StartChatEvent(user));
                }
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBusActivityScope.getDefault(_mActivity).post(new StopChatEvent());
                ((MainFragment) getParentFragment()).hideNotificationFragment();
            }
        });
        addSelections();
    }

    private void addSelections() {
        muscleControlRightCount = 0;
        selections.clear();
        selections.add(new Selection(1, "Start"));
        selections.add(new Selection(2, "Cancel"));
        if (selections.size() > 0) {
            mTvSelection.setText(selections.get(0).getName());
        }
    }

    @Subscribe
    public void onNotificationEvent(NotificationEvent event) {
        user = event.getUser();
        message = event.getMessage();
        if (user != null && !TextUtils.isEmpty(message)) {
            String thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + user.getId() + ".jpg";
            Glide.with(this).load(thumbnail).circleCrop().into(mNivThumbnail);
            mTvName.setText(user.getName());
            mTvMessage.setText(message);
        }
    }

    @Subscribe
    public void onMuscleControlLeftEvents(MuscleControlLeftEvents event) {
        if (event != null && event.getFragmentId() == NOTIFICATION_FRAGMENT_ID) {
            if (selections.get(muscleControlRightCount).getId() == 1) {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startChatBtn.performClick();
                    }
                });
            } else {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelBtn.performClick();
                    }
                });
            }
        }
    }

    @Subscribe
    public void onMuscleControlRightEvents(MuscleControlRightEvents event) {
        if (event != null && event.getFragmentId() == NOTIFICATION_FRAGMENT_ID) {
            muscleControlRightCount++;
            if (muscleControlRightCount == selections.size()) {
                muscleControlRightCount = 0;
            }
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvSelection.setText(selections.get(muscleControlRightCount).getName());
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
