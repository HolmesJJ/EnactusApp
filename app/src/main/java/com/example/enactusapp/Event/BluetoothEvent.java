package com.example.enactusapp.Event;

public class BluetoothEvent {

    private final String mChannel1;
    private final String mChannel2;
    private final int mCurrentPosition;

    public BluetoothEvent(String mChannel1, String mChannel2, int mCurrentPosition) {
        this.mChannel1 = mChannel1;
        this.mChannel2 = mChannel2;
        this.mCurrentPosition = mCurrentPosition;
    }

    public String getChannel1() {
        return mChannel1;
    }

    public String getChannel2() {
        return mChannel2;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }
}
