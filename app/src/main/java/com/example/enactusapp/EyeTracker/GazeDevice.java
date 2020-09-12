package com.example.enactusapp.EyeTracker;

import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class GazeDevice {

    private Map<String, GazeDevice.Info> deviceInfoList = new HashMap();

    public GazeDevice() {
    }

    public void addDeviceInfo(String modelName, float screen_origin_x, float screen_origin_y) {
        this.deviceInfoList.put(modelName, new GazeDevice.Info(modelName, screen_origin_x, screen_origin_y));
    }

    public boolean isCurrentDeviceFound() {
        return GazeDevice.Device.isDeviceFound();
    }

    public GazeDevice.Info getCurrentDeviceInfo() {
        GazeDevice.Info info = (GazeDevice.Info)this.deviceInfoList.get(Build.MODEL);
        if (info != null) {
            return info;
        } else {
            GazeDevice.Device device = GazeDevice.Device.getDevice();
            return new GazeDevice.Info(device.model, device.screen_origin_x, device.screen_origin_y);
        }
    }

    public GazeDevice.Info[] getAvailableDevices() {
        GazeDevice.Info[] avaliableDevices = new GazeDevice.Info[GazeDevice.Device.values().length];

        for(int i = 0; i < GazeDevice.Device.values().length; ++i) {
            avaliableDevices[i] = new GazeDevice.Info(GazeDevice.Device.values()[i].model, GazeDevice.Device.values()[i].screen_origin_x, GazeDevice.Device.values()[i].screen_origin_y);
        }

        return avaliableDevices;
    }

    public static class Info {
        public String modelName;
        public float screen_origin_x;
        public float screen_origin_y;

        public Info(String modelName, float screen_origin_x, float screen_origin_y) {
            this.modelName = modelName;
            this.screen_origin_x = screen_origin_x;
            this.screen_origin_y = screen_origin_y;
        }
    }

    private static enum Device {
        DEFAULT("default", 0.0F, 0.0F),
        SM_G977N("SM-G977N", -57.0F, 3.0F),
        SM_G965N("SM-G965N", -50.0F, -3.0F),
        SM_G960N("SM-G960N", -45.0F, -3.0F),
        SM_G950N("SM-G950N", -45.0F, -3.0F),
        SM_G930L("SM-G930L", -55.0F, -9.0F),
        SM_T865N("SM-T865N", -71.0F, -5.0F),
        SM_P615N("SM-P615N", -68.0F, -5.0F),
        SM_P615("SM-P615", -68.0F, -5.0F),
        SM_P610N("SM-P610N", -68.0F, -5.0F),
        SM_P610("SM-P610", -68.0F, -5.0F),
        SM_T720("SM-T720", -72.0F, -4.0F),
        SM_T536("SM-T536", -145.0F, 89.0F),
        LG_F600S("LG-F600S", -12.0F, -5.5F),
        LM_G820N("LM-G820N", -25.0F, 1.0F),
        PAFM00("PAFM00", -52.0F, -5.5F),
        PCRM00("PCRM00", -7.5F, 0.0F),
        // 红米10X
        M2004J78C("M2004J7BC", -34f, -1.0f),
        // 华为Mate30Pro
        LIO_AN00("LIO-AN00", -46f, -1.0f);

        String model;
        float screen_origin_x;
        float screen_origin_y;

        private Device(String model, float screen_origin_x, float screen_origin_y) {
            this.model = model;
            this.screen_origin_x = screen_origin_x;
            this.screen_origin_y = screen_origin_y;
        }

        static GazeDevice.Device getDevice() {
            GazeDevice.Device[] var0 = values();
            int var1 = var0.length;

            for(int var2 = 0; var2 < var1; ++var2) {
                GazeDevice.Device device = var0[var2];
                if (device.model.equals(Build.MODEL)) {
                    return device;
                }
            }

            return DEFAULT;
        }

        static boolean isDeviceFound() {
            return getDevice() != DEFAULT;
        }
    }
}
