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
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.enactusapp.Adapter.BluetoothAdapter;
import com.example.enactusapp.Bluetooth.BluetoothHelper;
import com.example.enactusapp.Bluetooth.BluetoothHelper.UpdateList;
import com.example.enactusapp.Fragment.Base.BaseBackFragment;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.R;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.GPSUtils;
import com.example.enactusapp.Utils.ToastUtils;
import com.hc.bluetoothlibrary.DeviceModule;

import java.util.ArrayList;
import java.util.List;

public class BluetoothFragment extends BaseBackFragment implements UpdateList {

    private static final String TAG = BluetoothFragment.class.getSimpleName();

    private static final int START_LOCATION_ACTIVITY = 99;

    private BluetoothHelper mBluetoothHelper;

    private ImageView mIvBack;
    private SwipeRefreshLayout mSrlRefresh;
    private BluetoothAdapter mPairedBluetoothAdapter;
    private BluetoothAdapter mUnpairedBluetoothAdapter;

    private final List<DeviceModule> pairedDeviceModules = new ArrayList<>();
    private final List<DeviceModule> unpairedDeviceModules = new ArrayList<>();

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
        mIvBack = (ImageView) view.findViewById(R.id.iv_back);
        mSrlRefresh = (SwipeRefreshLayout) view.findViewById(R.id.srl_refresh);
        RecyclerView mRvPairedBluetooth = (RecyclerView) view.findViewById(R.id.rv_paired_bluetooth);
        RecyclerView mRvUnpairedBluetooth = (RecyclerView) view.findViewById(R.id.rv_unpaired_bluetooth);
        mPairedBluetoothAdapter = new BluetoothAdapter(ContextUtils.getContext(), pairedDeviceModules);
        LinearLayoutManager pairedBluetoothLinearLayoutManager = new LinearLayoutManager(ContextUtils.getContext());
        DividerItemDecoration pairedBluetoothDividerItemDecoration = new DividerItemDecoration(mRvPairedBluetooth.getContext(), pairedBluetoothLinearLayoutManager.getOrientation());
        mRvPairedBluetooth.setLayoutManager(pairedBluetoothLinearLayoutManager);
        mRvPairedBluetooth.addItemDecoration(pairedBluetoothDividerItemDecoration);
        mRvPairedBluetooth.setAdapter(mPairedBluetoothAdapter);
        mUnpairedBluetoothAdapter = new BluetoothAdapter(ContextUtils.getContext(), unpairedDeviceModules);
        LinearLayoutManager unpairedBluetoothLinearLayoutManager = new LinearLayoutManager(ContextUtils.getContext());
        DividerItemDecoration unpairedBluetoothDividerItemDecoration = new DividerItemDecoration(mRvUnpairedBluetooth.getContext(), unpairedBluetoothLinearLayoutManager.getOrientation());
        mRvUnpairedBluetooth.setLayoutManager(unpairedBluetoothLinearLayoutManager);
        mRvUnpairedBluetooth.addItemDecoration(unpairedBluetoothDividerItemDecoration);
        mRvUnpairedBluetooth.setAdapter(mUnpairedBluetoothAdapter);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
        initBluetooth();
    }

    private void initDelayView() {
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popTo(MainFragment.class, false);
            }
        });

        mPairedBluetoothAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ToastUtils.showShortSafe("Connecting " + pairedDeviceModules.get(position).getName());
                mBluetoothHelper.connect(pairedDeviceModules.get(position));
            }
        });

        mUnpairedBluetoothAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ToastUtils.showShortSafe("Pairing " + unpairedDeviceModules.get(position).getName());
                mBluetoothHelper.connect(unpairedDeviceModules.get(position));
            }
        });

        mSrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSrlRefresh.setRefreshing(false);
                refresh();
            }
        });
    }

    private void initBluetooth() {
        mBluetoothHelper = BluetoothHelper.getInstance();
        mBluetoothHelper.setOnUpdateListListener(this);
        refresh();
    }

    // 开启位置权限
    private void startLocation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ContextUtils.getContext());
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
            if (!GPSUtils.isOpenGPS(ContextUtils.getContext())) {
                startLocation();
            }
        }
    }

    //刷新的具体实现
    private void refresh() {
        if (mBluetoothHelper.scan(false)) {
            pairedDeviceModules.clear();
            unpairedDeviceModules.clear();
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
                    if (GPSUtils.isOpenGPS(ContextUtils.getContext())) {
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
            deviceModule.isCollectName(ContextUtils.getContext());
            if (deviceModule.isBeenConnected()) {
                pairedDeviceModules.add(deviceModule);
            } else {
                unpairedDeviceModules.add(deviceModule);
            }
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPairedBluetoothAdapter.notifyItemInserted(pairedDeviceModules.size() - 1);
                    mUnpairedBluetoothAdapter.notifyItemInserted(unpairedDeviceModules.size() - 1);
                }
            });
        } else {
            Log.i(TAG, "Done..");
        }
    }

    @Override
    public void updateMessyCode(boolean isStart, DeviceModule deviceModule) {
        for (int i = 0; i < pairedDeviceModules.size(); i++) {
            if (pairedDeviceModules.get(i).getMac().equals(deviceModule.getMac())) {
                pairedDeviceModules.remove(i);
                int pos = i;
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPairedBluetoothAdapter.notifyItemRemoved(pos);
                    }
                });
                pairedDeviceModules.add(i, deviceModule);
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPairedBluetoothAdapter.notifyItemInserted(pos);
                    }
                });
                break;
            }
        }
        for (int i = 0; i < unpairedDeviceModules.size(); i++) {
            if (unpairedDeviceModules.get(i).getMac().equals(deviceModule.getMac())) {
                unpairedDeviceModules.remove(i);
                int pos = i;
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUnpairedBluetoothAdapter.notifyItemRemoved(pos);
                    }
                });
                unpairedDeviceModules.add(i, deviceModule);
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mUnpairedBluetoothAdapter.notifyItemInserted(pos);
                    }
                });
                break;
            }
        }
    }

    @Override
    public void connectSucceed() {

    }

    @Override
    public void errorDisconnect(DeviceModule deviceModule) {

    }
}
