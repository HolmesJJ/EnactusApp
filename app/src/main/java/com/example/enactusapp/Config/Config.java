package com.example.enactusapp.Config;

import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Utils.SpUtils;
import com.example.enactusapp.Constants.SpUtilKeyConstants;

public class Config {

    public static final String SETTING_CONFIG = "SettingConfig";
    public static int sControlMode;
    public static int sUseMode;
    public static String sSocketAddress;
    public static boolean sIsLogin;
    public static int sUserId;
    public static String sUsername;
    public static String sName;
    public static String sFirebaseToken;
    public static double sLongitude;
    public static double sLatitude;
    public static boolean sIsCalibrated;
    public static long sLastCalibratedTime;

    private static SpUtils sSp = SpUtils.getInstance(SETTING_CONFIG);

    public Config() {
    }

    public static void setControlMode(int controlMode) {
        sSp.put(SpUtilKeyConstants.CONTROL_MODE, controlMode);
        sControlMode = controlMode;
    }

    public static void setUseMode(int useMode) {
        sSp.put(SpUtilKeyConstants.USE_MODE, useMode);
        sUseMode = useMode;
    }

    public static void setSocketAddress(String socketAddress) {
        sSp.put(SpUtilKeyConstants.SOCKET_ADDRESS, socketAddress);
        sSocketAddress = socketAddress;
    }

    public static void setIsLogin(boolean isLogin) {
        sSp.put(SpUtilKeyConstants.IS_LOGIN, isLogin);
        sIsLogin = isLogin;
    }

    public static void setUserId(int userId) {
        sSp.put(SpUtilKeyConstants.USER_ID, userId);
        sUserId = userId;
    }

    public static void setUsername(String username) {
        sSp.put(SpUtilKeyConstants.USERNAME, username);
        sUsername = username;
    }

    public static void setName(String name) {
        sSp.put(SpUtilKeyConstants.NAME, name);
        sName = name;
    }

    public static void setFirebaseToken(String firebaseToken) {
        sSp.put(SpUtilKeyConstants.FIREBASE_TOKEN, firebaseToken);
        sFirebaseToken = firebaseToken;
    }

    public static void setLongitude(double longitude) {
        sSp.put(SpUtilKeyConstants.LONGITUDE, longitude);
        sLongitude = longitude;
    }

    public static void setLatitude(double latitude) {
        sSp.put(SpUtilKeyConstants.LATITUDE, latitude);
        sLatitude = latitude;
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
        int controlMode = Config.sControlMode;
        int useMode = Config.sUseMode;
        String socketAddress = Config.sSocketAddress;
        loadConfig();
        Config.setControlMode(controlMode);
        Config.setUseMode(useMode);
        Config.setSocketAddress(socketAddress);
    }

    public static void loadConfig() {
        sControlMode = sSp.getInt(SpUtilKeyConstants.CONTROL_MODE, SpUtilValueConstants.EYE_TRACKING_MODE);
        sUseMode = sSp.getInt(SpUtilKeyConstants.USE_MODE, SpUtilValueConstants.DEFAULT_MODE);
        sSocketAddress = sSp.getString(SpUtilKeyConstants.SOCKET_ADDRESS, SpUtilValueConstants.DEFAULT_SOCKET_ADDRESS);
        sIsLogin = sSp.getBoolean(SpUtilKeyConstants.IS_LOGIN, false);
        sUserId = sSp.getInt(SpUtilKeyConstants.USER_ID, -1);
        sUsername = sSp.getString(SpUtilKeyConstants.USERNAME, "");
        sName = sSp.getString(SpUtilKeyConstants.NAME, "");
        sFirebaseToken = sSp.getString(SpUtilKeyConstants.FIREBASE_TOKEN, "");
        sIsCalibrated = sSp.getBoolean(SpUtilKeyConstants.IS_CALIBRATED, false);
        sLastCalibratedTime = sSp.getLong(SpUtilKeyConstants.LAST_CALIBRATED_TIME, 0);
    }

    static {
        loadConfig();
    }
}
