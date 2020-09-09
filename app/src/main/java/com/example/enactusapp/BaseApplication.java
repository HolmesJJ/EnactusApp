package com.example.enactusapp;

import android.app.Application;

import com.example.enactusapp.Utils.ContextUtils;

import me.yokeyword.fragmentation.Fragmentation;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Fragmentation is recommended to initialize in the Application
        Fragmentation.builder()
                // show stack view. Mode: BUBBLE, SHAKE, NONE
                .stackViewMode(Fragmentation.NONE)
                .debug(BuildConfig.DEBUG)
                .install();

        ContextUtils.init(this);
    }
}
