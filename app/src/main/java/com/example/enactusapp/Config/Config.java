package com.example.enactusapp.Config;

import com.example.enactusapp.Utils.SpUtils;
import com.example.enactusapp.Constants.SpUtilKeyConstants;

public class Config {

    public static final String SETTING_CONFIG = "SettingConfig";
    public static boolean sIsLogin;
    public static String sUserId;
    public static boolean sIsCalibrated;
    public static long sLastCalibratedTime;

    private static SpUtils sSp = SpUtils.getInstance(SETTING_CONFIG);

    public Config() {
    }

    public static void setIsLogin(boolean isLogin) {
        sSp.put(SpUtilKeyConstants.IS_LOGIN, isLogin);
        sIsLogin = isLogin;
    }

    public static void setUserId(String userId) {
        sSp.put(SpUtilKeyConstants.USER_ID, userId);
        sUserId = userId;
    }

    public static void setIsCalibrated(boolean isCalibrated) {
        sSp.put(SpUtilKeyConstants.IS_CALIBRATED, isCalibrated);
        sIsCalibrated = isCalibrated;
    }

    public static void setLastCalibratedTime(long lastCalibratedTime) {
        sSp.put(SpUtilKeyConstants.LAST_CALIBRATED_TIME, lastCalibratedTime);
        sLastCalibratedTime = lastCalibratedTime;
    }

    public static void resetConfig() {
        SpUtils.getInstance(SETTING_CONFIG).clear();
        loadConfig();
    }

    public static void loadConfig() {
        sIsLogin = sSp.getBoolean(SpUtilKeyConstants.IS_LOGIN, false);
        sUserId = sSp.getString(SpUtilKeyConstants.USER_ID, "");
        sIsCalibrated = sSp.getBoolean(SpUtilKeyConstants.IS_CALIBRATED, false);
        sLastCalibratedTime = sSp.getLong(SpUtilKeyConstants.LAST_CALIBRATED_TIME, 0);
    }

    static {
        loadConfig();
    }
}
