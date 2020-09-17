package com.hc.bluetoothlibrary.classicBluetooth;

import android.app.Activity;
import android.content.Context;

public class TaskThread {

    public interface WorkCallBack{
        void succeed();
        boolean work() throws Exception;
        void error(Exception e);
    }

    private WorkCallBack call;
    private Context context;

    public TaskThread(Context context){
        this.context = context;
    }

    public void setWorkCall(WorkCallBack call){
        this.call = call;
        start();
    }

    private void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(call != null){
                    Activity activity = (Activity) context;
                    final boolean b;
                    try {
                        b = call.work();
                    } catch (final Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                call.error(e);
                            }
                        });
                        e.printStackTrace();
                        return;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (b){
                                call.succeed();
                            }
                        }
                    });
                }
            }
        }).start();
    }

}
