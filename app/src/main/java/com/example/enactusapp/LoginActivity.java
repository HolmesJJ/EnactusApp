package com.example.enactusapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.core.content.ContextCompat;

import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.Markov.Listener.MarkovListener;
import com.example.enactusapp.Markov.MarkovHelper;
import com.example.enactusapp.Thread.CustomThreadPool;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.GPSUtils;
import com.example.enactusapp.Utils.PermissionsUtils;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.ToastUtils;
import com.example.enactusapp.WebSocket.WebSocketClientManager;

import org.json.JSONObject;

import pl.droidsonroids.gif.GifImageView;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;

public class LoginActivity extends BaseActivity implements OnTaskCompleted, MarkovListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final int LOGIN = 1;
    private static final int REC_PERMISSION = 100;
    private static final int START_LOCATION_ACTIVITY = 101;
    String[] PERMISSIONS = {
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.CHANGE_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.FOREGROUND_SERVICE, // Sdk Version 28
            // 自行去Settings打开SYSTEM_ALERT_WINDOW权限
            // android.Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    private EditText mUsername;
    private EditText mPassword;
    private RadioGroup mRgControlModeContainer;
    private RadioButton mRbEyeTrackingMode;
    private RadioButton mRbMuscleControlMode;
    private Button mBtnSignIn;
    private Button mBtnDefaultMode;
    private Button mBtnBluetoothMode;
    private Button mBtnSocketMode;
    private LinearLayout mLlSocketAddressContainer;
    private Button mBtnConfirm;
    private EditText mEtSocketAddress;
    private GifImageView mGivLoading;

    private static final CustomThreadPool sThreadPoolLoadDataSets = new CustomThreadPool(Thread.NORM_PRIORITY);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();

        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            mRbEyeTrackingMode.setChecked(true);
            mRbEyeTrackingMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(ContextUtils.getContext(), R.color.control_mode_radio_button_select)));
            mRbMuscleControlMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(ContextUtils.getContext(), R.color.control_mode_radio_button_unselect)));
        } else {
            mRbMuscleControlMode.setChecked(true);
            mRbMuscleControlMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(ContextUtils.getContext(), R.color.control_mode_radio_button_select)));
            mRbEyeTrackingMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(ContextUtils.getContext(), R.color.control_mode_radio_button_unselect)));
        }

        requestPermission();

        mRgControlModeContainer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_eye_tracking_mode:
                        mRbEyeTrackingMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.control_mode_radio_button_select)));
                        mRbMuscleControlMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.control_mode_radio_button_unselect)));
                        Config.setControlMode(SpUtilValueConstants.EYE_TRACKING_MODE);
                        break;
                    case R.id.rb_muscle_control_mode:
                        mRbMuscleControlMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.control_mode_radio_button_select)));
                        mRbEyeTrackingMode.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.control_mode_radio_button_unselect)));
                        Config.setControlMode(SpUtilValueConstants.MUSCLE_CONTROL_MODE);
                        break;
                    default:
                        break;
                }
            }
        });

        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasSystemAlertWindowPermission()) {
                    return;
                }
                if (!GPSUtils.isOpenGPS(ContextUtils.getContext())) {
                    startLocation();
                    return;
                }
                mGivLoading.setVisibility(View.VISIBLE);
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                HttpAsyncTaskPost task = new HttpAsyncTaskPost(LoginActivity.this, LOGIN);
                String jsonData = convertToJSONLogin(mUsername.getText().toString(), mPassword.getText().toString());
                task.execute(Constants.IP_ADDRESS + "api/Account/Login", jsonData, null);
            }
        });

        mBtnDefaultMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setUseMode(SpUtilValueConstants.DEFAULT_MODE);
                mLlSocketAddressContainer.setVisibility(View.INVISIBLE);
                ToastUtils.showShortSafe("Default Mode");
                WebSocketClientManager.getInstance().close();
            }
        });

        mBtnBluetoothMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setUseMode(SpUtilValueConstants.BLUETOOTH_MODE);
                mLlSocketAddressContainer.setVisibility(View.INVISIBLE);
            }
        });

        mBtnSocketMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setUseMode(SpUtilValueConstants.SOCKET_MODE);
                mLlSocketAddressContainer.setVisibility(View.VISIBLE);
            }
        });

        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setSocketAddress(mEtSocketAddress.getText().toString().trim());
                if (!WebSocketClientManager.getInstance().isConnected()) {
                    WebSocketClientManager.getInstance().connect(Config.sSocketAddress);
                }
                mLlSocketAddressContainer.setVisibility(View.INVISIBLE);
            }
        });

        if (!MarkovHelper.getInstance().isInitialized()) {
            MarkovHelper.getInstance().initMarkov(ContextUtils.getContext());
        }
        if (!MarkovHelper.getInstance().isDataSetsLoaded()) {
            mGivLoading.setVisibility(View.VISIBLE);
            sThreadPoolLoadDataSets.execute(() -> {
                MarkovHelper.getInstance().addMarkovListener(this);
                MarkovHelper.getInstance().loadDataSets();
            });
        }
    }

    private void initView() {
        mUsername = (EditText) findViewById(R.id.et_username);
        mPassword = (EditText) findViewById(R.id.et_password);
        mRgControlModeContainer = (RadioGroup) findViewById(R.id.rg_control_mode_container);
        mRbEyeTrackingMode = (RadioButton) findViewById(R.id.rb_eye_tracking_mode);
        mRbMuscleControlMode = (RadioButton) findViewById(R.id.rb_muscle_control_mode);
        mBtnSignIn = (Button) findViewById(R.id.btn_sign_in);
        mBtnDefaultMode = (Button) findViewById(R.id.btn_default_mode);
        mBtnBluetoothMode = (Button) findViewById(R.id.btn_bluetooth_mode);
        mBtnSocketMode = (Button) findViewById(R.id.btn_socket_mode);
        mLlSocketAddressContainer = (LinearLayout) findViewById(R.id.ll_socket_address_container);
        mBtnConfirm = (Button) findViewById(R.id.btn_confirm);
        mEtSocketAddress = (EditText) findViewById(R.id.et_socket_address);
        mEtSocketAddress.setText(Config.sSocketAddress);
        mGivLoading = (GifImageView) findViewById(R.id.giv_loading);
    }

    @AfterPermissionGranted(REC_PERMISSION)
    private void requestPermission() {
        mBtnSignIn.setEnabled(false);
        PermissionsUtils.doSomeThingWithPermission(this, () -> {
            mBtnSignIn.setEnabled(true);
            if (!hasSystemAlertWindowPermission()) {
                return;
            }
            if (!GPSUtils.isOpenGPS(ContextUtils.getContext())) {
                startLocation();
                return;
            }
            if (Config.sIsLogin) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, PERMISSIONS, REC_PERMISSION, R.string.rationale_init);
    }

    private boolean hasSystemAlertWindowPermission() {
        if(!Settings.canDrawOverlays(ContextUtils.getContext())) {
            new AppSettingsDialog.Builder(this).setTitle(R.string.need_permissions_str)
                    .setRationale(getString(R.string.permissions_denied_content_str)).build().show();
        } else {
            return true;
        }
        return false;
    }

    //开启位置权限
    private void startLocation() {
        // 不能用ContextUtils.getContext()
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    private String convertToJSONLogin(String username, String password) {
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("Username", username);
            jsonMsg.put("Password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONLogin(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int code = jsonObject.getInt("code");
            int id = jsonObject.getInt("id");
            String username = jsonObject.getString("username");
            String name = jsonObject.getString("name");
            double longitude = jsonObject.getDouble("longitude");
            double latitude = jsonObject.getDouble("latitude");
            String message = jsonObject.getString("message");
            if (code == 1) {
                Config.setIsLogin(true);
                Config.setUserId(id);
                Config.setUsername(username);
                Config.setName(name);
                Config.setLongitude(longitude);
                Config.setLatitude(latitude);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                ToastUtils.showShortSafe(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Markov
    @Override
    public void onDataSetsLoaded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGivLoading.setVisibility(View.GONE);
            }
        });
        ToastUtils.showShortSafe("Load data sets successfully...");
    }

    @Override
    public void onDataSetsError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGivLoading.setVisibility(View.GONE);
            }
        });
        ToastUtils.showShortSafe("Load data sets error...");
    }

    @Override
    public void onTaskCompleted(String response, int requestId, String... others) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == LOGIN) {
            retrieveFromJSONLogin(response);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_LOCATION_ACTIVITY) {
            if (!GPSUtils.isOpenGPS(ContextUtils.getContext())) {
                startLocation();
            }
        }
    }
}
