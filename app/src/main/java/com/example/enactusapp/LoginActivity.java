package com.example.enactusapp;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.Utils.PermissionsUtils;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.ToastUtils;

import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

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
    };

    private LinearLayout loginForm;
    private ProgressBar mPbLoading;
    private Toolbar mToolbar;
    private EditText mUsername;
    private EditText mPassword;
    private Button mBtnSignIn;

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
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                showProgress(true);
                HttpAsyncTaskPost task = new HttpAsyncTaskPost(LoginActivity.this, LOGIN);
                String jsonData = convertToJSONLogin(mUsername.getText().toString(), mPassword.getText().toString());
                task.execute(Constants.IP_ADDRESS + "login.php", jsonData, null);
            }
        });
    }

    private void initView() {
        loginForm = (LinearLayout) findViewById(R.id.loginForm);
        mPbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.login);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mBtnSignIn = (Button) findViewById(R.id.btn_sign_in);
    }

    @AfterPermissionGranted(REC_PERMISSION)
    private void requestPermission() {
        mBtnSignIn.setEnabled(false);
        PermissionsUtils.doSomeThingWithPermission(this, () -> {
            mBtnSignIn.setEnabled(true);
        }, PERMISSIONS, REC_PERMISSION, R.string.rationale_init);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            loginForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            mPbLoading.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
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
        showProgress(false);
        if (requestId == LOGIN) {
            retrieveFromJSONLogin(response);
        }
    }
}
