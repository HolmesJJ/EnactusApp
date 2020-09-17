package com.hc.bluetoothlibrary;

import java.util.List;

public interface IBluetooth {

    //更新列表
    void updateList(DeviceModule deviceModule);

    //连接成功
    void connectSucceed(DeviceModule deviceModule);

    //刷新结束
    void updateEnd();

    void updateMessyCode(DeviceModule deviceModule);

    //读取数据
    void readData(String mac,byte[] data);

    //更新读取中的状态
    void reading(boolean isStart);

    //蓝牙异常断开
    void errorDisconnect(DeviceModule deviceModule);

    //蓝牙收到的字节数
    void readNumber(int number);

    //获取日志
    void readLog(String className,String data,String lv);
}
