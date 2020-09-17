package com.hc.bluetoothlibrary.bleBluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.bluetoothlibrary.tootl.ModuleParameters;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
* 蓝牙服务，通过调用类DownloadBinder里的方法，实现控制服务
* 0000ffe1-0000-1000-8000-00805f9b34fb 这个UUID是汇承特有的UUID
* 00002902-0000-1000-8000-00805f9b34fb 这个是设置监听模块数据的UUID
* */
public class BluetoothLeService extends Service {
    //蓝牙的特征值，发送
    private final static String SERVICE_EIGENVALUE_SEND = "0000ffe1-0000-1000-8000-00805f9b34fb";
    //蓝牙的特征值，接收
    private final static String SERVICE_EIGENVALUE_READ = "00002902-0000-1000-8000-00805f9b34fb";

    private Handler mHandler;
    private DownloadBinder mBinder = new DownloadBinder();

    private BluetoothGatt mBluetoothGatt;

    private Handler mTimeHandler = new Handler();

    private BluetoothGattCharacteristic mNeedCharacteristic;

    private boolean sendDataSign = true;//发送的标志位

    private ExecutorService mThreadService;

    private DeviceModule mDeiceModule;


    //控制服务的类
    class DownloadBinder extends Binder {

        void setHandler(Handler handler){
            if (mHandler == null)
                mHandler = handler;
            log("服务创建成功");
        }

        BluetoothDevice getDevice(){
            if (mBluetoothGatt != null)
                return mBluetoothGatt.getDevice();
            return null;
        }

        void connect(Context context, DeviceModule device){
            log("连接..");
            mBluetoothGatt = device.getDevice().connectGatt(context,false,gattCallback);
            mDeiceModule = device;
        }

        void send(byte[] data){
            //发送数据
            if (mNeedCharacteristic == null){
                log("错误，没有拿到写入特征","e");
                return;
            }
            sendThread(data);
        }

        void disconnect(){
            //断开连接
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }
    }


    //蓝牙连接完的所有回调
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if(newState == 133){
                log("出现133问题，需要扫描重连","e");
                sendHandler(BleBluetoothManage.SERVICE_CONNECT_FAIL,"出现133错误");
            }
            if (newState == BluetoothGatt.STATE_CONNECTED){
                log("连接成功，开始获取服务UUID");
                if (mBluetoothGatt ==null){
                    log("出现未知错误，服务关闭，GATT is null","e");
                    stopSelf();
                    sendHandler(BleBluetoothManage.SERVICE_CONNECT_FAIL,"未知错误");
                }
                mTimeHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothGatt.discoverServices();//扫描服务
                    }
                },1000);//坑：设置延迟时间过短，很可能发现不了服务
                mTimeHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        log("获取服务UUID超时，断开重连","e");
                        sendHandler(BleBluetoothManage.SERVICE_ERROR_DISCONNECT,null);
                    }
                },5000);


            }else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                log("蓝牙断开","e");
                sendHandler(BleBluetoothManage.SERVICE_ERROR_DISCONNECT,null);
            }

        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS){
                //所有app发送给模块数据成功的回调都在这里
                sendHandler(BleBluetoothManage.SERVICE_SEND_DATA_NUMBER, String.valueOf(characteristic.getValue().length));
                sendDataSign = true;//等到发送数据回调成功才可以继续发送
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                //mBluetoothGatt.writeDescriptor(descriptor);
                //来到这里，才算真正的建立连接
                log("设置监听成功,可以发送数据了...");
                mDeiceModule.setUUID(null,descriptor.getUuid().toString(),null);
                log("服务中连接成功，给与的返回名称是->"+gatt.getDevice().getName());
                log("服务中连接成功，给与的返回地址是->"+gatt.getDevice().getAddress());
                sendHandler(BleBluetoothManage.SERVICE_CONNECT_SUCCEED,null);
            }

        }


        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            //扫描到蓝牙服务的回调
            // （思想：获取该模块的所有服务，然后再轮询服务下面的所有特征的UUID，再与汇承的UUID比较
            // 　　　　找到汇承的UUID后，建立监听模块数据的回调才算完成真正的连接。）
            mTimeHandler.removeMessages(0);
            List<BluetoothGattService> servicesLists = mBluetoothGatt.getServices();//获取模块的所有服务
            log("扫描到服务的个数:"+servicesLists.size());
            int i = 0;

            for (final BluetoothGattService servicesList : servicesLists) {
                ++i;
                log("-----------打印服务----------");
                log(i+"号服务的uuid: "+servicesList.getUuid().toString());
                List<BluetoothGattCharacteristic> gattCharacteristics = servicesList
                        .getCharacteristics();//获取单个服务下的所有特征
                int j=0;
                log("----------打印特征-----------");
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    ++j;
                    if (gattCharacteristic.getUuid().toString().equals(SERVICE_EIGENVALUE_SEND)){//汇承蓝牙的UUID
                        log(i+"号服务的第"+j+"个特征"+gattCharacteristic.getUuid().toString(),"w");
                        mDeiceModule.setUUID(servicesList.getUuid().toString(),null,gattCharacteristic.getUuid().toString());//存下特征
                        mNeedCharacteristic = gattCharacteristic;
                        log("发送特征："+mNeedCharacteristic.getUuid().toString(),"w");
                        mBluetoothGatt.setCharacteristicNotification(
                                mNeedCharacteristic, true);
                        mTimeHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothGattService linkLossService = gatt.getService(servicesList.getUuid());
                                BluetoothGattDescriptor clientConfig = mNeedCharacteristic
                                        .getDescriptor(UUID.fromString(SERVICE_EIGENVALUE_READ));//这个收取数据的UUID
                                if (clientConfig != null) {
                                    //BluetoothGatt.getService(service)
                                    clientConfig
                                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mBluetoothGatt.writeDescriptor(clientConfig);//必须是设置这个才能监听模块数据
                                }else {
                                    log("备用方法测试","w");
                                    setNotification(mBluetoothGatt,linkLossService.getCharacteristic(UUID.fromString(SERVICE_EIGENVALUE_READ)),true);
                                }
                            }
                        },200);
                    }else {
                        log(i + "号服务的第" + j + "个特征" + gattCharacteristic.getUuid().toString());
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt,characteristic);
            //模块发送的所有数据都会回调到这里
            //蓝牙发送给app的回调
            log("接收到数据回调");
            try {
                log("数据是："+new String(characteristic.getValue(), 0, characteristic.getValue().length, "GB2312"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            sendHandler( characteristic.getValue().clone());//再转码后发送给handler更新UI
        }
    };


    //发送线程 --> app发送给模块数据
    private void sendThread(final byte[] buff){

        if (mThreadService == null){
            mThreadService = Executors.newFixedThreadPool(1);
        }

        log("进入发送方法");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                List<byte[]> sendDataArray = getSendDataByte(buff);
                int number = 0;
                for (byte[] sendData : sendDataArray) {
                    try {
                        if (ModuleParameters.getLevel() != 0){//设置发送间隔等级，从0到10，
                            Thread.sleep(ModuleParameters.getLevel()*10);//最高,多延时100ms
                        }
                        Thread.sleep(5+10*ModuleParameters.getState());//每次发包前，延时一会，更容易成功
                        mNeedCharacteristic.setValue(sendData);
                        sendDataSign = !mBluetoothGatt.writeCharacteristic(mNeedCharacteristic);//蓝牙发送数据，一次顶多20字节
                        if (sendDataSign){
                            Thread.sleep(1000+500*ModuleParameters.getState());
                            log("发送失败....","e");
                            sendDataSign = !mBluetoothGatt.writeCharacteristic(mNeedCharacteristic);
                            if (sendDataSign){
                                log("无法发送数据","e");
                                return;
                            }
                        }
                        while (!sendDataSign){
                            Thread.sleep(10+10*ModuleParameters.getState());
                            number++;
                            if (number == 40) {
                                mNeedCharacteristic.setValue(new byte[0]);//额外发送会导致发包重复，所以发一个空包去提醒
                                sendDataSign = !mBluetoothGatt.writeCharacteristic(mNeedCharacteristic);
                                log("额外发送一次,"+sendDataSign,"w");
                            }
                            if (number == 80){
                                mNeedCharacteristic.setValue(new byte[0]);//额外发送会导致发包重复，所以发一个空包去提醒
                                sendDataSign = !mBluetoothGatt.writeCharacteristic(mNeedCharacteristic);
                                log("再次额外发送一次,"+sendDataSign,"e");
                            }

                            if (number == 180){
                                mNeedCharacteristic.setValue(new byte[0]);//额外发送会导致发包重复，所以发一个空包去提醒
                                sendDataSign = !mBluetoothGatt.writeCharacteristic(mNeedCharacteristic);
                                log("第三次额外发送一次,"+sendDataSign,"e");
                            }

                            if (number == 300){
                                sendDataSign = true;
                                log("发送失败,关闭线程","e");
                                return;
                            }
                        }
                        number = 0;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mThreadService.execute(runnable);
        log("进入线程池发送方法");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("开启服务..");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("服务关闭..");
        if (mThreadService != null) {
            mThreadService.shutdown();
            mThreadService = null;
        }
    }



    //将数据分包
    private int[] dataSeparate(int len) {
        int[] lens = new int[2];
        lens[0]=len/20;
        lens[1]=len%20;
        return lens;
    }

    //数据转byte数组
    private byte[] getByteArray(String data){
        byte[] buff =null;
        try {
            buff =data.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return buff;
    }

    //将String字符串分包为List byte数组
    private List<byte[]> getSendDataByte(byte[] buff){
        List<byte[]> listSendData = new ArrayList<>();
        int[] sendDataLength = dataSeparate(buff.length);
        for(int i=0;i<sendDataLength[0];i++) {
            byte[] dataFor20 = new byte[20];
            System.arraycopy(buff, i * 20, dataFor20, 0, 20);
            listSendData.add(dataFor20);
        }

        if(sendDataLength[1]>0) {
            byte[] lastData = new byte[sendDataLength[1]];
            System.arraycopy(buff, sendDataLength[0] * 20, lastData, 0, sendDataLength[1]);
            listSendData.add(lastData);
        }
        return listSendData;
    }

    //监听到的数据通过handler发送回activity
    private void sendHandler(byte[] data){
        if (mHandler == null){
            log("错误，返回信息的handler为空","e");
            return;
        }

        Message message = mHandler.obtainMessage();
        message.what = BleBluetoothManage.SERVICE_CALLBACK;
        message.obj = data.clone();
        mHandler.sendMessage(message);

    }

    private void sendHandler(int type,String data){
        if (mHandler != null){
            Message message = mHandler.obtainMessage();
            message.what = type;
            if (data != null)
                message.obj = data;
            mHandler.sendMessage(message);
        }
    }


    public void setNotification(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean enable) {
        if (gatt == null || characteristic == null) {
            log("gatt == null || characteristic == null");
            return;
        }
        boolean success = gatt.setCharacteristicNotification(characteristic, enable);
        Log.e("TAG", "setNotification: " + success);
        if (success) {
            for (final BluetoothGattDescriptor dp : characteristic.getDescriptors()) {
                if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    log("路线1");
                } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    log("路线2");
                }else {
                    log("没有走");
                }
                mTimeHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gatt.writeDescriptor(dp);
                        log("监听的特征是: "+dp.getUuid().toString());
                    }
                },1000);

                mTimeHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothGatt.writeDescriptor(dp);
                        log("监听的特征是: "+dp.getUuid().toString());
                    }
                },5000);
            }
        }
    }

    private void sendLog(String data,String lv){
        if (mHandler == null)
            return;
        String str = BleBluetoothManage.SERVICE_SEPARATOR;
        Message message = mHandler.obtainMessage();
        message.what = BleBluetoothManage.SERVICE_READ_LOG;
        message.obj = getClass().getSimpleName()+str+data+str+lv+str;
        mHandler.sendMessage(message);
    }


    public void log(String str){
        Log.d("AppRunService",str);
        sendLog(str,"d");
    }
    public void log(String str,String e){
        if (e.equals("i")) {
            Log.e("AppRunService", str);
            return;
        }
        if(e.equals("e"))
            Log.e("AppRunService",str);
        else
            Log.w("AppRunService",str);
        sendLog(str,e);
    }

}
