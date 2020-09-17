package com.example.enactusapp.Fragment.Bluetooth;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.enactusapp.Adapter.BluetoothAdapter;
import com.example.enactusapp.Bluetooth.BluetoothHelper;
import com.example.enactusapp.Bluetooth.BluetoothHelper.UpdateList;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.GPSUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.hc.bluetoothlibrary.DeviceModule;

import java.util.ArrayList;
import java.util.List;

import me.yokeyword.fragmentation.SupportFragment;

public class BluetoothFragment extends SupportFragment implements OnItemClickListener, UpdateList {

    private static final String TAG = "BluetoothFragment";

    private static final int START_LOCATION_ACTIVITY = 99;
    private static final byte[] STATE_DATA = new byte[] {0x00};;

    private BluetoothHelper mBluetoothHelper;

    private Toolbar mToolbar;
    private TextView mTvConnectedDevice;
    private SwipeRefreshLayout mSrlRefresh;
    private RecyclerView mRvBluetooth;
    private BluetoothAdapter mBluetoothAdapter;

    private List<DeviceModule> deviceModules = new ArrayList<>();

    public static BluetoothFragment newInstance() {
        BluetoothFragment fragment = new BluetoothFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.bluetooth);
        mTvConnectedDevice = (TextView) view.findViewById(R.id.tv_connected_device);
        mSrlRefresh = (SwipeRefreshLayout) view.findViewById(R.id.srl_refresh);
        mRvBluetooth = (RecyclerView) view.findViewById(R.id.rv_bluetooth);
        mBluetoothAdapter = new BluetoothAdapter(_mActivity, deviceModules);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRvBluetooth.getContext(), linearLayoutManager.getOrientation());
        mRvBluetooth.setLayoutManager(linearLayoutManager);
        mRvBluetooth.addItemDecoration(dividerItemDecoration);
        mRvBluetooth.setAdapter(mBluetoothAdapter);
        mBluetoothAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        mSrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSrlRefresh.setRefreshing(false);
                refresh();
            }
        });
        initBluetooth();
    }

    private void initBluetooth() {
        mBluetoothHelper = BluetoothHelper.getInstance();
        mBluetoothHelper.setOnUpdateListListener(this);
        refresh();
    }

    //开启位置权限
    private void startLocation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(_mActivity);
        builder.setTitle("Tips")
                .setMessage("Please turn on your GPS")
                .setCancelable(false)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, START_LOCATION_ACTIVITY);
                    }
                }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == START_LOCATION_ACTIVITY) {
            if (!GPSUtils.isOpenGPS(_mActivity)) {
                startLocation();
            }
        }
    }

    //刷新的具体实现
    private void refresh() {
        if (mBluetoothHelper.getConnectedDeviceModules().size() > 0) {
            mBluetoothHelper.sendData(mBluetoothHelper.getConnectedDeviceModules().get(0), STATE_DATA);
        }
        if (mBluetoothHelper.scan(false)) {
            deviceModules.clear();
        }
    }

    @Override
    public void onItemClick(int position) {
        if (mBluetoothHelper.getConnectedDeviceModules().size() > 0 && deviceModules.get(position).getMac().equals(mBluetoothHelper.getConnectedDeviceModules().get(0).getMac())) {
            mBluetoothHelper.disconnectAll();
            mTvConnectedDevice.setText("");
            ToastUtils.showShortSafe("Disconnected: " + deviceModules.get(position).getName());
        } else {
            mBluetoothHelper.disconnectAll();
            mBluetoothHelper.connect(deviceModules.get(position));
            ToastUtils.showShortSafe("Connected: " + deviceModules.get(position).getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 打开蓝牙
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothHelper.bluetoothState()) {
                    if (GPSUtils.isOpenGPS(_mActivity)) {
                        refresh();
                    } else {
                        startLocation();
                    }
                }
            }
        }, 1000);
    }

    @Override
    public void onPause() {
        if (mBluetoothHelper != null) {
            mBluetoothHelper.stopScan();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mBluetoothHelper != null) {
            mBluetoothHelper.stopScan();
            mBluetoothHelper.setOnUpdateListListener(null);
            mBluetoothHelper = null;
        }
        super.onDestroyView();
    }

    @Override
    public void update(boolean isStart, DeviceModule deviceModule) {
        if (isStart) {
            deviceModule.isCollectName(_mActivity);
            deviceModules.add(deviceModule);
            mBluetoothAdapter.notifyDataSetChanged();
        } else {
            Log.i(TAG, "Done..");
        }
    }

    @Override
    public void updateMessyCode(boolean isStart, DeviceModule deviceModule) {
        for (int i = 0; i < deviceModules.size(); i++) {
            if (deviceModules.get(i).getMac().equals(deviceModule.getMac())) {
                deviceModules.remove(deviceModules.get(i));
                deviceModules.add(i, deviceModule);
                mBluetoothAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void connectSucceed() {
        if (mBluetoothHelper.getConnectedDeviceModules().size() > 0) {
            mTvConnectedDevice.setText(mBluetoothHelper.getConnectedDeviceModules().get(0).getName() + " " + mBluetoothHelper.getConnectedDeviceModules().get(0).getMac());
        }
    }

    @Override
    public void errorDisconnect(DeviceModule deviceModule) {
        mTvConnectedDevice.setText("");
    }
}
