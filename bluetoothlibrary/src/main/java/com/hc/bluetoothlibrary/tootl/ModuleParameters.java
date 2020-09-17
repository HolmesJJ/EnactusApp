package com.hc.bluetoothlibrary.tootl;

import android.content.Context;
import android.os.Build;

public class ModuleParameters {

    private static int state = 1;//0,1,2

    private static int bleReadBuff = 1000;//一个包20bit 一共50个包

    private static int classicReadBuff = 1500;//缓存1500 bit

    private static int time = 100;

    private static int level = 0;

    private static final String partition = "/%partition%/";

    public static void setParameters(int state, int bleReadBuff, int classicReadBuff,int bleTime, Context context) {
        if ( ModuleParameters.state != state || ModuleParameters.bleReadBuff != bleReadBuff || ModuleParameters.classicReadBuff != classicReadBuff || ModuleParameters.time != bleTime) {
            ModuleParameters.state = state;
            if (bleReadBuff<20)
                bleReadBuff = 20;
            ModuleParameters.bleReadBuff = bleReadBuff;
            if (classicReadBuff<20)
                classicReadBuff = 20;
            ModuleParameters.classicReadBuff = classicReadBuff;
            if (bleTime<20)
                bleTime = 20;
            ModuleParameters.time = bleTime;
            new DataMemory(context).saveParameters(state+partition+bleReadBuff+partition+classicReadBuff+partition+bleTime);
        }
    }

    public static void saveLevel(int moduleLevel,Context context){
        if (level != moduleLevel){
            level = moduleLevel;
            new DataMemory(context).saveModuleLevel(level);
        }
    }

    public static void init(Context context){
        DataMemory dataMemory = new DataMemory(context);
        String data = dataMemory.getParameters();
        level = dataMemory.getModuleLevel();
        if (data != null){
            ModuleParameters.state = Integer.parseInt(ToolClass.analysis(data,0,partition));
            ModuleParameters.bleReadBuff = Integer.parseInt(ToolClass.analysis(data,1,partition));
            ModuleParameters.classicReadBuff = Integer.parseInt(ToolClass.analysis(data,2,partition));
            ModuleParameters.time = Integer.parseInt(ToolClass.analysis(data,3,partition));
        }
    }

    public static int addLevel(){
        ++level;
        if (level>10)
            level = 10;
        return level;
    }

    public static int minusLevel(){
        --level;
        if (level<0)
            level = 0;
        return level;
    }

    public static int getTime() {
        return time;
    }

    public static int getState() {
        if (system()){
            return state+2;
        }
        return state;
    }

    public static int getBleReadBuff() {
        return bleReadBuff;
    }

    public static int getClassicReadBuff() {
        return classicReadBuff;
    }

    public static int getLevel() {
        return level;
    }

    public static boolean system() {//彻底杜绝华为机
        String manufacturer = Build.MANUFACTURER;
        //这个字符串可以自己定义,例如判断华为就填写huawei,魅族就填写meizu
        if ("huawei".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        if ("honor".equalsIgnoreCase(manufacturer)){
            return true;
        }
        return "rongyao".equalsIgnoreCase(manufacturer);
    }
}
