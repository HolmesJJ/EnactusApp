package com.example.enactusapp.Event;

public class BluetoothEvent {

    private String mChannel1;
    private String mChannel2;
    private int mCurrentPosition;

    public BluetoothEvent(String mChannel1, String mChannel2, int mCurrentPosition) {
        this.mChannel1 = mChannel1;
        this.mChannel2 = mChannel2;
        this.mCurrentPosition = mCurrentPosition;
    }

    public String getChannel1() {
        return mChannel1;
    }

    public void setChannel1(String mChannel1) {
        this.mChannel1 = mChannel1;
    }

    public String getChannel2() {
        return mChannel2;
    }

    public void setChannel2(String mChannel2) {
        this.mChannel2 = mChannel2;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(int mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }
}
