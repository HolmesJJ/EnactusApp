package com.example.enactusapp;

import android.content.Intent;
import android.os.Bundle;

import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.Service.LocationService;
import com.example.enactusapp.Utils.ContextUtils;

import me.yokeyword.fragmentation.SupportActivity;
import me.yokeyword.fragmentation.anim.DefaultHorizontalAnimator;
import me.yokeyword.fragmentation.anim.FragmentAnimator;

public class MainActivity extends SupportActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findFragment(MainFragment.class) == null) {
            loadRootFragment(R.id.fl_container, MainFragment.newInstance());
        }

        startLocationService();
    }

    @Override
    public FragmentAnimator onCreateFragmentAnimator() {
        // 设置横向(和安卓4.x动画相同)
        return new DefaultHorizontalAnimator();
    }

    @Override
    protected void onDestroy() {
        stopLocationService();
        super.onDestroy();
    }

    private void startLocationService() {
        Intent intent = new Intent(ContextUtils.getContext(), LocationService.class);
        ContextUtils.getContext().startService(intent);
    }

    private void stopLocationService() {
        Intent intent = new Intent(ContextUtils.getContext(), LocationService.class);
        ContextUtils.getContext().stopService(intent);
    }
}