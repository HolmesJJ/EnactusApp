package com.example.enactusapp.Thread;

import androidx.annotation.NonNull;

import java.util.concurrent.ThreadFactory;

public class CustomThreadFactory implements ThreadFactory {

    private int priority = Thread.NORM_PRIORITY;

    public CustomThreadFactory() {
    }

    public CustomThreadFactory(int priority) {
        this.priority = priority;
    }

    @Override
    public Thread newThread(@NonNull Runnable r) {
        Thread t = new Thread(r);
        t.setPriority(priority);
        return t;
    }
}
