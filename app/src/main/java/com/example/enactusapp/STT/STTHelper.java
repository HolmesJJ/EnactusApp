package com.example.enactusapp.STT;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.speech.asr.SpeechConstant;
import com.example.enactusapp.STT.Listener.IRecogListener;
import com.example.enactusapp.STT.Listener.STTListener;
import com.example.enactusapp.STT.Utils.AutoCheck;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class STTHelper implements IRecogListener {

    private static final String TAG = "STTHelper";
    private static final String DIR_PATH = "baiduSTT";

    // 是否需要调用离线命令词功能
    private static final boolean ENABLE_OFFLINE = false; 

    /**
     * 识别控制器，使用SDKRecognizer控制识别的流程
     */
    private SDKRecognizer mSDKRecognizer;
    private STTListener mSTTListener;

    private boolean isSpeaking = false;

    private Map<String, Object> mParams;

    private STTHelper() {
    }

    private static class SingleInstance {
        private static STTHelper INSTANCE = new STTHelper();
    }

    public static STTHelper getInstance() {
        return STTHelper.SingleInstance.INSTANCE;
    }

    /**
     * 转写的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return 合成参数Map
     */
    private Map<String, Object> getParams() {
        // 字符串格式的参数
        ArrayList<String> stringParams = new ArrayList<String>(Arrays.asList(
                SpeechConstant.VAD,
                SpeechConstant.IN_FILE
        ));
        // int格式的参数
        ArrayList<String> intParams = new ArrayList<String>(Arrays.asList(
                SpeechConstant.PID,
                SpeechConstant.LMID,
                SpeechConstant.VAD_ENDPOINT_TIMEOUT
        ));
        // bool格式的参数
        ArrayList<String> boolParams = new ArrayList<String>(Arrays.asList(
                SpeechConstant.ACCEPT_AUDIO_DATA,
                SpeechConstant.ACCEPT_AUDIO_VOLUME
        ));
        final Map<String, Object>  params = new HashMap<String, Object>();
        // 英语识别设置 {accept-audio-data=true, vad.endpoint-timeout=800, outfile=/storage/emulated/0/baiduASR/outfile.pcm, pid=1737, accept-audio-volume=false}
        params.put(intParams.get(0), 1737);
        params.put(intParams.get(2), 800);
        params.put(boolParams.get(0), true);
        params.put(boolParams.get(1), false);
        params.put(SpeechConstant.OUT_FILE, FileUtils.createTmpDir(ContextUtils.getContext(), DIR_PATH) + File.separator + "outfile.pcm");
        return params;
    }

    public void autoCheck() {
        // 复制此段可以自动检测常规错误
        (new AutoCheck(ContextUtils.getContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainAllMessage();
                        Log.w(TAG, "AutoCheckMessage: " + message);
                    }
                }
            }
        }, ENABLE_OFFLINE)).checkAsr(mParams);
    }

    public void initSTT(STTListener sttListener) {
        this.mSTTListener = sttListener;
        if (mSDKRecognizer != null) {
            mSDKRecognizer.release();
            mSDKRecognizer = null;
        }
        mSDKRecognizer = new SDKRecognizer(ContextUtils.getContext(), this);
        if (ENABLE_OFFLINE) {
            Map<String, Object> mOfflineParams = new HashMap<String, Object>();
            mOfflineParams.put(SpeechConstant.DECODER, 2);
            mOfflineParams.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets:///baidu_speech_grammar.bsg");
            mSDKRecognizer.loadOfflineEngine(mOfflineParams);
        }
        // DEMO集成步骤2.1 拼接识别参数： 此处params可以打印出来，直接写到你的代码里去，最终的json一致即可。
        mParams = getParams();
        // params 也可以根据文档此处手动修改，参数会以jsonMyRecognizer的格式在界面和logcat日志中打印
        Log.i(TAG, "设置的start输入参数：" + mParams);
        autoCheck();
    }

    public void releaseSTT() {
        if (mSDKRecognizer != null) {
            mSDKRecognizer.release();
            mSDKRecognizer = null;
        }
        mSTTListener = null;
        mParams = null;
    }

    /**
     * 开始录音，点击“开始”按钮后调用
     */
    public void start() {
        if (mSDKRecognizer != null) {
            mSDKRecognizer.start(mParams);
        }
    }

    /**
     * 开始录音后，手动点击“停止”按钮。
     * SDK会识别不会再识别停止后的录音。
     * 基于DEMO集成4.1 发送停止事件 停止录音
     */
    public void stop() {
        if (mSDKRecognizer != null) {
            mSDKRecognizer.stop();
        }
    }

    /**
     * 开始录音后，手动点击“取消”按钮。
     * SDK会取消本次识别，回到原始状态。
     * 基于DEMO集成4.2 发送取消事件 取消本次识别
     */
    public void cancel() {
        if (mSDKRecognizer != null) {
            mSDKRecognizer.cancel();
        }
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void setSpeaking(boolean speaking) {
        if (mSDKRecognizer != null) {
            isSpeaking = speaking;
        }
    }

    // 引擎准备完毕
    @Override
    public void onAsrReady() {
        Log.i(TAG, "onAsrReady");
        mSTTListener.onSTTAsrReady();
    }

    // 用户开始说话到用户说话完毕前
    @Override
    public void onAsrBegin() {
        Log.i(TAG, "onAsrBegin");
        mSTTListener.onSTTAsrBegin();
    }

    // 用户说话完毕后，识别结束前
    @Override
    public void onAsrEnd() {
        Log.i(TAG, "onAsrEnd");
        mSTTListener.onSTTAsrEnd();
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        Log.i(TAG, "onAsrPartialResult results: " + Arrays.toString(results) + ", recogResult: " + recogResult.toString());
        mSTTListener.onSTTAsrPartialResult(results, recogResult);
    }

    @Override
    public void onAsrOnlineNluResult(String nluResult) {
        Log.i(TAG, "onAsrOnlineNluResult nluResult: " + nluResult);
        mSTTListener.onSTTAsrOnlineNluResult(nluResult);
    }

    // 获得最终识别结果
    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        Log.i(TAG, "onAsrFinalResult results: " + Arrays.toString(results) + ", recogResult: " + recogResult.toString());
        mSTTListener.onSTTAsrFinalResult(results, recogResult);
    }

    // 获得最终识别结果
    @Override
    public void onAsrFinish(RecogResult recogResult) {
        Log.i(TAG, "onAsrFinish recogResult: " + recogResult.toString());
        mSTTListener.onSTTAsrFinish(recogResult);
    }

    // 获得最终识别结果
    @Override
    public void onAsrFinishError(int errorCode, int subErrorCode, String descMessage, RecogResult recogResult) {
        Log.i(TAG, "onAsrFinishError errorCode: "+ errorCode + ", subErrorCode: " + subErrorCode + ", " + descMessage +", recogResult: " + recogResult.toString());
        mSTTListener.onSTTAsrFinishError(errorCode, subErrorCode, descMessage, recogResult);
    }

    @Override
    public void onAsrLongFinish() {
        Log.i(TAG, "onAsrLongFinish");
        mSTTListener.onSTTAsrLongFinish();
    }

    @Override
    public void onAsrVolume(int volumePercent, int volume) {
        Log.i(TAG, "onAsrVolume 音量百分比" + volumePercent + " ; 音量" + volume);
        mSTTListener.onSTTAsrVolume(volumePercent, volume);
    }

    @Override
    public void onAsrAudio(byte[] data, int offset, int length) {
        if (offset != 0 || data.length != length) {
            byte[] actualData = new byte[length];
            System.arraycopy(data, 0, actualData, 0, length);
            data = actualData;
        }
        Log.i(TAG, "onAsrAudio 音频数据回调, length:" + data.length);
        mSTTListener.onSTTAsrAudio(data, offset, length);
    }

    // 全部完成恢复初始状态
    @Override
    public void onAsrExit() {
        Log.i(TAG, "onAsrExit");
        mSTTListener.onSTTAsrExit();
    }

    @Override
    public void onOfflineLoaded() {
        Log.i(TAG, "onOfflineLoaded");
        mSTTListener.onSTTOfflineLoaded();
    }

    @Override
    public void onOfflineUnLoaded() {
        Log.i(TAG, "onOfflineUnLoaded");
        mSTTListener.onSTTOfflineUnLoaded();
    }
}
