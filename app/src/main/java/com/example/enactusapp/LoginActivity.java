package com.example.enactusapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.enactusapp.Utils.PermissionsUtils;
import com.example.enactusapp.config.Config;

import androidx.appcompat.widget.Toolbar;

import pub.devrel.easypermissions.AfterPermissionGranted;

public class LoginActivity extends BaseActivity {

    private static final int REC_PERMISSION = 100;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private LinearLayout loginForm;
    private ProgressBar loginProgress;
    private Toolbar mToolbar;
    private EditText mNRIC;
    private Button email_sign_in_button;

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

        email_sign_in_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                showProgress(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showProgress(false);
                        if (mNRIC.getText().toString().equals("A1234567B") || mNRIC.getText().toString().equals("C7654321D")) {
                            Config.setIsLogin(true);
                            Config.setUserId(mNRIC.getText().toString());
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            mNRIC.setError("Wrong NRIC!");
                        }
                    }
                }, 1000);

            }
        });
    }

    private void initView() {
        loginForm = (LinearLayout) findViewById(R.id.loginForm);
        loginProgress = (ProgressBar) findViewById(R.id.loginProgress);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.login);
        mNRIC = (EditText) findViewById(R.id.nric);
        email_sign_in_button = (Button) findViewById(R.id.email_sign_in_button);
    }

    @AfterPermissionGranted(REC_PERMISSION)
    private void requestPermission() {
        email_sign_in_button.setEnabled(false);
        PermissionsUtils.doSomeThingWithPermission(this, () -> {
            email_sign_in_button.setEnabled(true);
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

            loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            loginProgress.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
