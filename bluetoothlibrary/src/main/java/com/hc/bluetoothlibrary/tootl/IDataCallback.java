package com.hc.bluetoothlibrary.tootl;

public interface IDataCallback {

    //读取到模块的数据
    void readData(byte[] data,String mac);

    //连接失败 cause：原因
    void connectionFail(String mac,String cause);

    //连接成功
    void connectionSucceed(String mac);

    //处于接收中的回调
    void reading(boolean isStart);

    //蓝牙异常断开
    void errorDisconnect(String mac);

    //蓝牙收到数据字节数
    void readNumber(int number);

    //获取日志
    void readLog(String className,String data,String lv);
}
