package com.hc.bluetoothlibrary.tootl;

import android.content.Context;
import android.location.LocationManager;

public class ToolClass {

    public static boolean pattern(String str){
        if (str == null)
            return false;
        int number = str.length();
        for (int i= 1;i<number;i++){
            if (str.substring(i-1,i).equals("�")){
                return true;
            }
        }
        return false;
    }

    //按间隔取出，例如analysis("123aa456aa789aa000",2,"aa") ->"789"
    //最多3个间隔，4个字符串，从0开始取
    public static String analysis(final String data,int number,String key){
        if (number == 0)
            return data.substring(0,data.indexOf(key));
        if (number == 1) {
            String string = data.substring(data.indexOf(key) + key.length());
            return string.substring(0,string.indexOf(key));
        }
        if (number == 2){
            int length2 = analysis(data,1,key).length();
            int length1 = analysis(data,0,key).length();
            String string = data.substring(length1+length2+key.length()*2);
            return string.substring(0,string.indexOf(key));
        }else {
            int length = analysis(data,0,key).length()+analysis(data,1,key).length()+analysis(data,2,key).length()+key.length()*3;
            return data.substring(length);
        }
    }

    //判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
    public static boolean isOpenGPS(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = true,network = true;
        // GPS定位
        if (locationManager != null)
            gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 网络服务定位
        if (locationManager != null)
            network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

}
