package com.hc.bluetoothlibrary.bleBluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.hc.bluetoothlibrary.DeviceModule;
import com.hc.bluetoothlibrary.tootl.IScanCallback;
import com.hc.bluetoothlibrary.tootl.IDataCallback;
import com.hc.bluetoothlibrary.tootl.ModuleParameters;
import com.hc.bluetoothlibrary.tootl.ToolClass;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/*
* 蓝牙的管理类
* 这个类的实现，最好放在设计为单例模式
* downloadBinder 通过这个成员变量控制服务
*
* 使用此类很简单:
* 扫描模块  -->  void scanBlueTooth()
* 连接模块  -->  void connectBluetooth(BluetoothDevice bluetoothDevice, Handler mDataHandler)
* 发送数据  -->  void sendData(String data)
* 断开模块  -->  void disConnectBluetooth()
* */
public class BleBluetoothManage {

    static final int SERVICE_CALLBACK = 0x00;
    static final int SERVICE_CONNECT_SUCCEED = 0x01;
    static final int SERVICE_CONNECT_FAIL = 0x02;
    static final int SERVICE_ERROR_DISCONNECT = 0x03;
    static final int SERVICE_SEND_DATA_NUMBER = 0x04;
    static final int SERVICE_READ_LOG = 0x05;

    static final String SERVICE_SEPARATOR ="/**separator**/";

    private BluetoothAdapter mBluetoothAdapter;// 蓝牙适配器

    private ScanCallback mScanCallback,mScanCallbackMessyCode;//Android5.0以上的扫描回调

    private BluetoothLeScanner mBluetoothLeScanner;//Android5.0以上的扫描方式

    private static final long SCAN_PERIOD = 15*1000;//扫描时间

    private Handler mTimeHandler = new Handler();//延时执行

    private Context mContext;

    private List<DeviceModule> mListDevices;//搜索得到的device

    private boolean isOffScan = true;//开关，搜索蓝牙的第一层开关
    private boolean isTimeScan = true;//开关，搜索蓝牙的时间开关


    //服务的控制类：1.setHandler() 设置handler 2.connect() 初步连接模块
    //             3.send() 发送数据          4.disconnect() 断开连接
    private BluetoothLeService.DownloadBinder downloadBinder;

    private IScanCallback mIScanCallback;//扫描回调
    private IDataCallback mIDataCallback;//连接数据回调

    private String mConnectedMac = null;//连接了的蓝牙模块物理地址

    private Handler mDataHandler;

    private List<byte[]> mDataArray = new ArrayList<>();



    //唯一指定构造方法
    public BleBluetoothManage(Context context){
        this.mContext = context;

        init_ble();//初始化ble，打开蓝牙

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setScanCallBack();

        initData();
        setHandler();
    }

    //初始化数据
    private void initData() {
        mListDevices = new ArrayList<>();
    }

    private void setHandler() {

        mDataHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (mIDataCallback == null){
                    Log.e("AppRun"+getClass().getSimpleName(),"mIDataCallback is null");
                    return false;
                }
                switch (msg.what){
                    case SERVICE_CALLBACK:
                        splicingData((byte[]) msg.obj);
                        break;
                    case SERVICE_CONNECT_SUCCEED:
                        if (getAddress() != null)
                            mIDataCallback.connectionSucceed(getAddress());
                        break;
                    case SERVICE_CONNECT_FAIL:
                        if (getAddress() != null)
                            mIDataCallback.connectionFail(getAddress(),msg.obj.toString());
                        break;
                    case SERVICE_ERROR_DISCONNECT:
                        if (getAddress() != null)
                            mIDataCallback.errorDisconnect(getAddress());
                        break;
                    case SERVICE_SEND_DATA_NUMBER:
                        mIDataCallback.readNumber(Integer.parseInt(msg.obj.toString()));
                        break;
                    case SERVICE_READ_LOG:
                        String data = (String) msg.obj;
                        try {
                            mIDataCallback.readLog(ToolClass.analysis(data, 0, SERVICE_SEPARATOR),
                                    ToolClass.analysis(data, 1, SERVICE_SEPARATOR), ToolClass.analysis(data, 2, SERVICE_SEPARATOR));
                            //Log.e("AppRun" + getClass().getSimpleName(), "接收到，发送往AllBlue");
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        break;
                }
                return false;
            }
        });

    }

    private String getAddress(){
        String address = null;
        try {
            address = downloadBinder.getDevice().getAddress();
        }catch (Exception e){
            e.printStackTrace();
        }
        return address;
    }





    //搜索蓝牙 --> 调用此方法会五秒内持续搜索蓝牙，五秒后自动停止，五秒内再调用会直接停止扫描
    public void scanBluetooth(IScanCallback iScanCallback){

        this.mIScanCallback = iScanCallback;

        if (mBluetoothAdapter == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            else {
                BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        }
        if (mBluetoothLeScanner == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        if (isOffScan) {
            isOffScan = false;
            isTimeScan = true;
            mTimeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isTimeScan){
                        log("时间到，已提前停止扫描");
                        return;
                    }
                    isOffScan = true;
                    log("自动停止扫描");
                    mIScanCallback.stopScan();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        mBluetoothLeScanner.stopScan(mScanCallback);
                    else
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    log("搜索到个数: "+mListDevices.size());
                }
            },SCAN_PERIOD);

            log("开始扫描...");
            mListDevices.clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mBluetoothLeScanner.startScan(mScanCallback);
            else
                mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    public void stopScan() throws Exception{
        if (!isOffScan){
            isOffScan = true;
            isTimeScan = false;
            log("手动停止扫描");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mBluetoothLeScanner.stopScan(mScanCallback);
            else
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mTimeHandler.removeMessages(0);
            log("搜索到个数: "+mListDevices.size());
        }
    }

    //搜索蓝牙 --> 此方法用于蓝牙的名称转码，使其不会乱码
    public void scanBluetooth(List<DeviceModule> list, boolean isStart,final IScanCallback iScanCallback){

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || ((list == null||list.size() == 0) && isStart)){
            log("不需要修正或是手机版本过低..");
            if (iScanCallback != null)
                iScanCallback.stopScan();
            return;
        }

        if (mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (mBluetoothLeScanner == null){
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }


        if (isStart) {
            setScanCallBackMessyCode(list);
            mTimeHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    log("自动停止扫描");
                    iScanCallback.stopScan();
                    mBluetoothLeScanner.stopScan(mScanCallbackMessyCode);
                }
            }, SCAN_PERIOD/2);
        }

        if (isStart) {
            mBluetoothLeScanner.startScan(mScanCallbackMessyCode);
        }else {
            log("停止扫描");
            mTimeHandler.removeMessages(0);
            mBluetoothLeScanner.stopScan(mScanCallbackMessyCode);
            if (iScanCallback != null)
                iScanCallback.stopScan();
        }
    }

    //连接蓝牙
    public void connectBluetooth(final DeviceModule module,IDataCallback iDataCallback){
        this.mIDataCallback = iDataCallback;
        mConnectedMac = module.getDevice().getAddress();
        log("获取需要连接的MAC: "+mConnectedMac);
        Intent serviceInter = new Intent(mContext,BluetoothLeService.class);
        mContext.bindService(serviceInter,connection, Context.BIND_AUTO_CREATE);
        mTimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadBinder.connect(mContext,module);
                downloadBinder.setHandler(mDataHandler);
            }
        },200);
    }

    //断开蓝牙
    public void disConnectBluetooth(){
        mConnectedMac = null;
        if (downloadBinder == null)
            return;
            downloadBinder.disconnect();
        mTimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mContext.unbindService(connection);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        },500);
    }

    //获取当前连接的Mac
    public String getMac(){
        return mConnectedMac;
    }

    //绑定服务的回调
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (BluetoothLeService.DownloadBinder) service;
            log("绑定服务..");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("onServiceDisconnected");
        }
    };

    //发送数据给模块
    public void sendData(byte[] data){
        downloadBinder.send(data);
    }

    private void splicingData(byte[] bytes) {
        mCountDownHandler.removeMessages(0);
        mDataArray.add(bytes);
        if (mDataArray.size()==10)
            mIDataCallback.reading(true);
        if (mDataArray.size() == ModuleParameters.getBleReadBuff()/20)
            dataToPhone();
        mCountDownHandler.sendMessageDelayed(mCountDownHandler.obtainMessage(),ModuleParameters.getTime());
    }

    private Handler mCountDownHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            dataToPhone();
            mIDataCallback.reading(false);
            return false;
        }
    });

    private void dataToPhone(){
        int length = 0;
        for (byte[] bytes : mDataArray) {
            length += bytes.length;
        }
        byte[] bytes = new byte[length];
        int start = 0;
        for (byte[] data : mDataArray) {
            System.arraycopy(data,0,bytes,start,data.length);
            start += data.length;
        }
        mIDataCallback.readData(bytes.clone(),mConnectedMac);
        mDataArray.clear();
    }

    // 扫描蓝牙回调 device和rssi的回调
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     byte[] scanRecord) {
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //mLeDeviceListAdapter.addDevice(device);
                            //log(device.toString());
                            addDeviceModel(device,rssi,null,null);
                        }
                    });
                }
            };

    //Android5.0以上扫描蓝牙回调，支持名称中文
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setScanCallBack(){
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                final BluetoothDevice device = result.getDevice();
                String moduleName = null;
                if(null != device && null != result.getScanRecord() && ToolClass.pattern(device.getName())) {
                    try {
                        if (device.getName()!=null) {
                            byte[] name = ParseLeAdvData.adv_report_parse(ParseLeAdvData.BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME,result.getScanRecord().getBytes());
                            if (name != null) {
                                moduleName = new String(name, "GBK");
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                if (moduleName == null && device != null)
                    moduleName = device.getName();

                final String finalModuleName = moduleName;
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addDeviceModel(device,result.getRssi(), finalModuleName,result);
                    }
                });
            }
            @Override
            public void onScanFailed(final int errorCode) {
                super.onScanFailed(errorCode);
                ((Activity)mContext).runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(mContext, "扫描出错:"+errorCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setScanCallBackMessyCode(final List<DeviceModule> list){
        mScanCallbackMessyCode = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                final BluetoothDevice device = result.getDevice();
                boolean isEquals = false;
                int listNumber = 0;
                for (;listNumber<list.size();listNumber++) {
                    if (device != null && list.get(listNumber).getMac().equals(device.getAddress())){
                        if (ToolClass.pattern(list.get(listNumber).getName())) {
                            isEquals = true;
                            break;
                        }
                    }
                }
                if (!isEquals){
                    return;
                }
                String moduleName = null;
                if( null != result.getScanRecord() && ToolClass.pattern(device.getName())) {
                    try {
                        byte[] name = ParseLeAdvData.adv_report_parse(ParseLeAdvData.BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME,result.getScanRecord().getBytes());
                        if (name != null) {
                            moduleName = new String(name, "GBK");
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                if (moduleName == null)
                    moduleName = device.getName();
                list.remove(listNumber);
                list.add(listNumber,new DeviceModule(device,result.getRssi(),moduleName,mContext,result));
            }
            @Override
            public void onScanFailed(final int errorCode) {
                super.onScanFailed(errorCode);
                ((Activity)mContext).runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(mContext, "扫描出错:"+errorCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }


    //添加搜索到的模块 --> 已经去除重复
    private void addDeviceModel(BluetoothDevice device,int rssi,String name,ScanResult result){
        if (mListDevices.size()==0){
            mListDevices.add(new DeviceModule(device,rssi,name,mContext,result));
            mIScanCallback.updateRecycler(mListDevices.get(0));
            return;
        }

        for (DeviceModule mListDevice : mListDevices) {
            if(mListDevice.getDevice().toString().equals(device.toString())){
                return;
            }
        }
        DeviceModule deviceModule = new DeviceModule(device,rssi,name,mContext,result);
        mListDevices.add(deviceModule);
        mIScanCallback.updateRecycler(deviceModule);
    }

    //初始化ble
    private void init_ble() {
        // 手机硬件支持蓝牙
        if (!mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, "不支持BLE蓝牙，请退出...", Toast.LENGTH_SHORT).show();
            ((Activity)mContext).finish();
            return;
        }
        // Initializes Bluetooth adapter.
        // 获取手机本地的蓝牙适配器
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        } else {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    private void log(String str){
        Log.d("AppRun"+getClass().getSimpleName(),str);
        if (mIDataCallback != null){
            mIDataCallback.readLog(getClass().getSimpleName(),str,"d");
        }
    }

}
