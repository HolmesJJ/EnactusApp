package com.example.enactusapp.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.enactusapp.CustomView.CustomToast;
import com.example.enactusapp.Entity.BackCameraEvent;
import com.example.enactusapp.Entity.MessageEvent;
import com.example.enactusapp.Entity.StartChatEvent;
import com.example.enactusapp.Fragment.Contact.ContactFragment;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.Notification.NotificationFragment;
import com.example.enactusapp.Fragment.ObjectDetection.ObjectDetectionFragment;
import com.example.enactusapp.Fragment.Profile.ProfileFragment;
import com.example.enactusapp.R;
import com.example.enactusapp.SharedPreferences.GetSetSharedPreferences;
import com.example.enactusapp.UI.BottomBar;
import com.example.enactusapp.UI.BottomBarTab;
import com.example.enactusapp.config.Config;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class MainFragment extends SupportFragment {

    private static final int FIRST = 0;
    private static final int SECOND = 1;
    private static final int THIRD = 2;
    private static final int FOURTH = 3;
    private SupportFragment[] mFragments = new SupportFragment[5];

    private BottomBar mBottomBar;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Handler handler = new Handler();
    private String fireBaseToken;

    public static MainFragment newInstance() {
        Bundle args = new Bundle();
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SupportFragment firstFragment = findFragment(ContactFragment.class);
        if (firstFragment == null) {
            mFragments[FIRST] = ContactFragment.newInstance();
            mFragments[SECOND] = DialogFragment.newInstance();
            mFragments[THIRD] = ObjectDetectionFragment.newInstance();
            mFragments[FOURTH] = ProfileFragment.newInstance();

            loadMultipleRootFragment(R.id.fl_main_container, FIRST,
                    mFragments[FIRST],
                    mFragments[SECOND],
                    mFragments[THIRD],
                    mFragments[FOURTH]
            );
        } else {
            mFragments[FIRST] = firstFragment;
            mFragments[SECOND] = findFragment(DialogFragment.class);
            mFragments[THIRD] = findFragment(ObjectDetectionFragment.class);
            mFragments[FOURTH] = findFragment(ProfileFragment.class);
        }
    }

    private void initView(View view) {

        mBottomBar = (BottomBar) view.findViewById(R.id.bottomBar);

        mBottomBar.addItem(new BottomBarTab(_mActivity, R.drawable.ic_contact, getString(R.string.contact)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_dialog, getString(R.string.dialog)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_object_detection, getString(R.string.objectDetection)))
                .addItem(new BottomBarTab(_mActivity, R.drawable.ic_profile, getString(R.string.profile)));

        mBottomBar.setOnTabSelectedListener(new BottomBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position, int prePosition) {
                if (prePosition == 2 && position != 2) {
                    EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(false));
                    // 开启摄像头
                } else if (prePosition != 2 && position == 2) {
                    // 关闭摄像头
                    EventBusActivityScope.getDefault(_mActivity).post(new BackCameraEvent(true));
                }
                showHideFragment(mFragments[position], mFragments[prePosition]);
            }

            @Override
            public void onTabUnselected(int position) {

            }

            @Override
            public void onTabReselected(int position) {

            }
        });

        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mMessageBroadcastReceiver, new IntentFilter("getMessageIntent"));
        LocalBroadcastManager.getInstance(_mActivity.getApplicationContext()).registerReceiver(mChatBroadcastReceiver, new IntentFilter("getChatIntent"));

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            CustomToast.show(_mActivity, "FireBase Token Error!");
                            return;
                        }
                        try {
                            // Get new Instance ID token
                            fireBaseToken = task.getResult().getToken();
                            System.out.println("fireBaseToken: " + fireBaseToken);
                        } catch (Exception e) {
                            CustomToast.show(_mActivity, "FireBase Token Error!");
                        }
                    }
                });

        // 10秒钟获取一次坐标
        handler.post(timedTask);
    }

    public void startBrotherFragment(SupportFragment targetFragment) {
        start(targetFragment);
    }

    private Runnable timedTask = new Runnable() {
        @Override
        public void run() {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(_mActivity);
            if (ActivityCompat.checkSelfPermission(_mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(_mActivity, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        System.out.println("Latitude: " + location.getLatitude());
                        System.out.println("getLongitude: " + location.getLongitude());
                    }
                }
            });
            handler.postDelayed(timedTask, 10000);
        }
    };

    private BroadcastReceiver mMessageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GetSetSharedPreferences.getDefaults("ChatWithDisabled", _mActivity) != null) {
                GetSetSharedPreferences.removeDefaults("ChatWithDisabled", _mActivity);
            }
            String message = intent.getStringExtra("message");
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent(message));
            intent.removeExtra("message");
            if (mBottomBar.getCurrentItemPosition() == 0) {
                showHideFragment(mFragments[1], mFragments[0]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 2) {
                showHideFragment(mFragments[1], mFragments[2]);
                mBottomBar.setCurrentItem(1);
            } else if (mBottomBar.getCurrentItemPosition() == 3) {
                showHideFragment(mFragments[1], mFragments[3]);
                mBottomBar.setCurrentItem(1);
            }
        }
    };

    private BroadcastReceiver mChatBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GetSetSharedPreferences.setDefaults("ChatWithDisabled", "true", _mActivity);
            startBrotherFragment(NotificationFragment.newInstance());
        }
    };

    @Subscribe
    public void onStartChatEvent(StartChatEvent event) {
        if (Config.sIsLogin && Config.sUserId.equals("A1234567B")) {
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent("Hi, Mr.Wong, How are you?"));
        } else {
            EventBusActivityScope.getDefault(_mActivity).post(new MessageEvent("Hi, Mr.Chai, How are you?"));
        }
        if (mBottomBar.getCurrentItemPosition() == 0) {
            showHideFragment(mFragments[1], mFragments[0]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 2) {
            showHideFragment(mFragments[1], mFragments[2]);
            mBottomBar.setCurrentItem(1);
        } else if (mBottomBar.getCurrentItemPosition() == 3) {
            showHideFragment(mFragments[1], mFragments[3]);
            mBottomBar.setCurrentItem(1);
        }
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
