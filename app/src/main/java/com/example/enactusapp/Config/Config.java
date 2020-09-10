package com.example.enactusapp.Config;

import com.example.enactusapp.Utils.SpUtils;
import com.example.enactusapp.Constants.SpUtilKeyConstants;

public class Config {

    public static final String SETTING_CONFIG = "SettingConfig";
    public static boolean sIsLogin;
    public static String sUserId;

    private static SpUtils sSp = SpUtils.getInstance(SETTING_CONFIG);

    public Config() {
    }

    public static void setIsLogin(boolean isLogin) {
        sSp.put(SpUtilKeyConstants.IS_LOGIN, isLogin);
        sIsLogin = isLogin;
    }

    public static void setUserId(String userId) {
        sSp.put(SpUtilKeyConstants.USER_ID, sUserId);
        sUserId = userId;
    }

    public static void resetConfig() {
        SpUtils.getInstance(SETTING_CONFIG).clear();
        loadConfig();
    }

    public static void loadConfig() {
        sIsLogin = sSp.getBoolean(SpUtilKeyConstants.IS_LOGIN, false);
        sUserId = sSp.getString(SpUtilKeyConstants.USER_ID, "");
    }

    static {
        loadConfig();
    }
}
