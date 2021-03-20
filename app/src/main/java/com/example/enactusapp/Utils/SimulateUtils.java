package com.example.enactusapp.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SimulateUtils {

    /**
     * 模拟点击
     */
    private static void simulateClick(Activity activity, View view, float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long interval = 20;
        // 模拟双击重置点击位置（点击任意空白位置）
        simulateClick(activity, view, downTime, interval, 6, 110);
        downTime = downTime + interval * 2;
        simulateClick(activity, view, downTime, interval, 6, 110);
        // 模拟双击全选
        downTime = downTime + interval * 2;
        simulateClick(activity, view, downTime, interval, x, y);
        downTime = downTime + interval * 2;
        simulateClick(activity, view, downTime, interval, x, y);
    }

    /**
     * 模拟点击
     */
    private static void simulateClick(Activity activity, View view, long start, long interval, float x, float y) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MotionEvent downEvent = MotionEvent.obtain(start, start + 100, MotionEvent.ACTION_DOWN, x, y, 0);
                long end = start + interval;
                final MotionEvent upEvent = MotionEvent.obtain(end, end + 100, MotionEvent.ACTION_UP, x, y, 0);
                view.onTouchEvent(downEvent);
                view.onTouchEvent(upEvent);
                downEvent.recycle();
                upEvent.recycle();
            }
        });
    }

    /**
     * 模拟点击
     */
    public static void simulateClick(Activity activity, int x, int y) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                long end = start + 100;
                MotionEvent evenDown = MotionEvent.obtain(start, start + 100, MotionEvent.ACTION_DOWN, x, y, 0);
                MotionEvent eventUp = MotionEvent.obtain(end, end + 100, MotionEvent.ACTION_UP, x, y, 0);
                activity.dispatchTouchEvent(evenDown);
                activity.dispatchTouchEvent(eventUp);
                evenDown.recycle();
                eventUp.recycle();
            }
        });
    }

    /**
     * 模拟点击
     */
    public static void simulateClick2(Activity activity, int x, int y) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis();
                MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[1];
                MotionEvent.PointerProperties pp1 = new MotionEvent.PointerProperties();
                pp1.id = 0;
                pp1.toolType = MotionEvent.TOOL_TYPE_FINGER;
                properties[0] = pp1;
                MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[1];
                MotionEvent.PointerCoords pc1 = new MotionEvent.PointerCoords();
                pc1.x = x;
                pc1.y = y;
                pc1.pressure = 1;
                pc1.size = 1;
                pointerCoords[0] = pc1;
                MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime,
                        MotionEvent.ACTION_DOWN, 1, properties,
                        pointerCoords, 0,  0, 1, 1, 0, 0, 0, 0 );
                activity.dispatchTouchEvent(motionEvent);
                motionEvent = MotionEvent.obtain(downTime, eventTime,
                        MotionEvent.ACTION_UP, 1, properties,
                        pointerCoords, 0,  0, 1, 1, 0, 0, 0, 0 );
                activity.dispatchTouchEvent(motionEvent);
            }
        });
    }
}
