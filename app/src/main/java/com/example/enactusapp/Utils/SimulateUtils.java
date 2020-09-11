package com.example.enactusapp.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class SimulateUtils {

    /**
     * 模拟点击
     */
    private static void simulateClick(View view, float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long interval = 20;
        // 模拟双击重置点击位置（点击任意空白位置）
        simulateClick(view, downTime, interval, 6, 110);
        downTime = downTime + interval * 2;
        simulateClick(view, downTime, interval, 6, 110);
        // 模拟双击全选
        downTime = downTime + interval * 2;
        simulateClick(view, downTime, interval, x, y);
        downTime = downTime + interval * 2;
        simulateClick(view, downTime, interval, x, y);
    }

    /**
     * 模拟点击
     */
    private static void simulateClick(View view, long start, long interval, float x, float y) {
        final MotionEvent downEvent = MotionEvent.obtain(start, start, MotionEvent.ACTION_DOWN, x, y, 0);
        long end = start + interval;
        final MotionEvent upEvent = MotionEvent.obtain(end, end, MotionEvent.ACTION_UP, x, y, 0);
        view.onTouchEvent(downEvent);
        view.onTouchEvent(upEvent);
        downEvent.recycle();
        upEvent.recycle();
    }

    /**
     * 模拟点击
     */
    public static void simulateClick(Activity activity, int x, int y) {
        MotionEvent evenDown = MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis() + 100, MotionEvent.ACTION_DOWN, x, y, 0);
        activity.dispatchTouchEvent(evenDown);
        MotionEvent eventUp = MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis() + 100, MotionEvent.ACTION_UP, x, y, 0);
        activity.dispatchTouchEvent(eventUp);
        evenDown.recycle();
        eventUp.recycle();
    }
}
