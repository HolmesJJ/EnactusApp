package com.example.enactusapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.Utils.PermissionsUtils;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.ToastUtils;
import com.example.enactusapp.WebSocket.WebSocketClientManager;

import org.json.JSONObject;

import pl.droidsonroids.gif.GifImageView;
import pub.devrel.easypermissions.AfterPermissionGranted;

public class LoginActivity extends BaseActivity implements OnTaskCompleted {

    private static final String TAG = "LoginActivity";

    private static final int LOGIN = 1;
    private static final int REC_PERMISSION = 100;
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
    };

    private EditText mUsername;
    private EditText mPassword;
    private Button mBtnSignIn;
    private Button mBtnDefaultMode;
    private Button mBtnBluetoothMode;
    private Button mBtnSocketMode;
    private LinearLayout mLlSocketAddressContainer;
    private Button mBtnConfirm;
    private EditText mEtSocketAddress;
    private GifImageView mGivLoading;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();

        if (Config.sIsLogin) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        requestPermission();

        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                Config.setMode(SpUtilValueConstants.DEFAULT_MODE);
                mLlSocketAddressContainer.setVisibility(View.INVISIBLE);
                WebSocketClientManager.getInstance().close();
            }
        });

        mBtnBluetoothMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setMode(SpUtilValueConstants.BLUETOOTH_MODE);
                mLlSocketAddressContainer.setVisibility(View.INVISIBLE);
            }
        });

        mBtnSocketMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Config.setMode(SpUtilValueConstants.SOCKET_MODE);
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
            }
        });
    }

    private void initView() {
        mUsername = (EditText) findViewById(R.id.et_username);
        mPassword = (EditText) findViewById(R.id.et_password);
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
        }, PERMISSIONS, REC_PERMISSION, R.string.rationale_init);
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

    @Override
    public void onTaskCompleted(String response, int requestId) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == LOGIN) {
            retrieveFromJSONLogin(response);
        }
    }
}
