package com.example.enactusapp.STT;

import android.content.Context;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.example.enactusapp.STT.Listener.IRecogListener;
import com.example.enactusapp.STT.Listener.RecogEventAdapter;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by fujiayi on 2017/6/13.
 * EventManager内的方法如send 都可以在主线程中进行，SDK中做过处理
 */

public class SDKRecognizer {

    private static final String TAG = "SDKRecognizer";
    /**
     * SDK 内部核心 EventManager 类
     */
    private EventManager mEventManager;

    // SDK 内部核心 事件回调类，用于开发者写自己的识别回调逻辑
    private EventListener eventListener;

    // 是否加载离线资源
    private static boolean isOfflineEngineLoaded = false;

    // 未release前，只能new一个
    private boolean mIsInitialized = false;

    /**
     * 初始化
     *
     * @param context
     * @param recogListener 将EventListener结果做解析的DEMO回调。使用RecogEventAdapter 适配EventListener
     */
    public SDKRecognizer(Context context, IRecogListener recogListener) {
        this(context, new RecogEventAdapter(recogListener));
    }

    /**
     * 初始化 提供 EventManagerFactory需要的Context和EventListener
     *
     * @param context
     * @param eventListener 识别状态和结果回调
     */
    public SDKRecognizer(Context context, EventListener eventListener) {
        if (mIsInitialized) {
            Log.e(TAG, "还未调用release()，请勿新建一个新类");
            throw new RuntimeException("还未调用release()，请勿新建一个新类");
        }
        mIsInitialized = true;
        this.eventListener = eventListener;
        // SDK集成步骤 初始化asr的EventManager示例，多次得到的类，只能选一个使用
        mEventManager = EventManagerFactory.create(context, "asr");
        // SDK集成步骤 设置回调event， 识别引擎会回调这个类告知重要状态和识别结果
        mEventManager.registerListener(eventListener);
    }


    /**
     * 离线命令词，在线不需要调用
     *
     * @param params 离线命令词加载参数，见文档“ASR_KWS_LOAD_ENGINE 输入事件参数”
     */
    public void loadOfflineEngine(Map<String, Object> params) {
        String json = new JSONObject(params).toString();
        Log.i(TAG + ".Debug", "离线命令词初始化参数（反馈请带上此行日志）:" + json);
        // SDK集成步骤（可选）加载离线命令词(离线时使用)
        mEventManager.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, json, null, 0, 0);
        isOfflineEngineLoaded = true;
     }

    /**
     * @param params
     */
    public void start(Map<String, Object> params) {
        if (!mIsInitialized) {
            throw new RuntimeException("release() was called");
        }
        // SDK集成步骤 拼接识别参数
        String json = new JSONObject(params).toString();
        Log.i(TAG + ".Debug", "识别参数（反馈请带上此行日志）" + json);
        mEventManager.send(SpeechConstant.ASR_START, json, null, 0, 0);
    }


    /**
     * 提前结束录音等待识别结果。
     */
    public void stop() {
        Log.i(TAG, "停止录音");
        // SDK 集成步骤（可选）停止录音
        if (!mIsInitialized) {
            throw new RuntimeException("release() was called");
        }
        mEventManager.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
    }

    /**
     * 取消本次识别，取消后将立即停止不会返回识别结果。
     * cancel 与stop的区别是 cancel在stop的基础上，完全停止整个识别流程，
     */
    public void cancel() {
        Log.i(TAG, "取消识别");
        if (!mIsInitialized) {
            throw new RuntimeException("release() was called");
        }
        // SDK集成步骤 (可选） 取消本次识别
        mEventManager.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
    }

    public void release() {
        if (mEventManager == null) {
            return;
        }
        cancel();
        if (isOfflineEngineLoaded) {
            // SDK集成步骤 如果之前有调用过 加载离线命令词，这里要对应释放
            mEventManager.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0);
            isOfflineEngineLoaded = false;
        }
        // SDK 集成步骤（可选），卸载listener
        mEventManager.unregisterListener(eventListener);
        mEventManager = null;
        mIsInitialized = false;
    }

    public void setEventListener(IRecogListener recogListener) {
        if (!mIsInitialized) {
            throw new RuntimeException("release() was called");
        }
        this.eventListener = new RecogEventAdapter(recogListener);
        mEventManager.registerListener(eventListener);
    }
}
