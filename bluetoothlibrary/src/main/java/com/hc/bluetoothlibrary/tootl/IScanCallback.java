package com.hc.bluetoothlibrary.tootl;


import com.hc.bluetoothlibrary.DeviceModule;

public interface IScanCallback {
    void stopScan();
    void updateRecycler(DeviceModule deviceModule);
}
