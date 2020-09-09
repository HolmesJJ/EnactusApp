package com.example.enactusapp.Utils;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

public final class ContextUtils {
    @SuppressLint({"StaticFieldLeak"})
    private static Context context;

    private ContextUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static void init(@NonNull Context context) {
        ContextUtils.context = context.getApplicationContext();
    }

    public static Context getContext() {
        if (context != null) {
            return context;
        } else {
            throw new NullPointerException("should be initialized in application");
        }
    }
}
