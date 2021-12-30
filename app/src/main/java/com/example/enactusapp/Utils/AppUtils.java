package com.example.enactusapp.Utils;

import android.content.Context;
import android.content.Intent;

public class AppUtils {

    public static void hideApp(Context context) {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startMain);
    }

    public static void showApp(Context context, Class<?> clz) {
        Intent intent = new Intent(context, clz);
        context.startActivity(intent);
    }
}
