package com.example.enactusapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.List;

import me.yokeyword.fragmentation.SupportActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public abstract class BaseActivity extends SupportActivity implements EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // 有权限都被准许后调用的接口
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // 有权限被拒绝后调用该接口
        new AppSettingsDialog.Builder(this).setTitle(R.string.need_permissions_str)
                .setRationale(getString(R.string.permissions_denied_content_str)).build().show();
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        // 开启申请权限时会调用该方法
    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (!onHasPermissions()) {
                exitApp();
            } else {
                onPermissionSuccessCallbackFromSetting();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 判断申请的权限是否都被允许
     */
    protected boolean onHasPermissions() {

        return true;
    }

    public void exitApp() {
        this.finish();
    }

    /**
     * 当冲设置界面回调程序中且授权成功时回调该方法
     */
    protected void onPermissionSuccessCallbackFromSetting() {

    }
}
