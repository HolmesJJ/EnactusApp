package com.example.enactusapp.TTS;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizeBag;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.SynthesizerTool;
import com.baidu.tts.client.TtsMode;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.TTS.Config.TTSConfig;
import com.example.enactusapp.TTS.Listener.TTSListener;
import com.example.enactusapp.TTS.Utils.AutoCheck;
import com.example.enactusapp.TTS.Utils.IOfflineResourceConst;
import com.example.enactusapp.TTS.Utils.OfflineResource;
import com.example.enactusapp.Utils.ContextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TTSHelper implements SpeechSynthesizerListener {

    private static final String TAG = "TTSHelper";
    private static final int ERROR_CODE = -99;

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； TtsMode.OFFLINE 纯离线合成，需要纯离线SDK
    private TtsMode ttsMode = IOfflineResourceConst.DEFAULT_SDK_TTS_MODE;
    private boolean isOnlineSDK = TtsMode.ONLINE.equals(IOfflineResourceConst.DEFAULT_SDK_TTS_MODE);

    private TTSConfig mTTSConfig;
    private TTSListener mTTSListener;
    private SpeechSynthesizer mSpeechSynthesizer;

    private boolean isInitialized = false;

    // 离线发音选择，VOICE_FEMALE即为离线女声发音。
    // assets目录下bd_etts_common_speech_m15_mand_eng_high_am-mix_vXXXXXXX.dat为离线男声模型文件；
    // assets目录下bd_etts_common_speech_f7_mand_eng_high_am-mix_vXXXXX.dat为离线女声模型文件;
    // assets目录下bd_etts_common_speech_yyjw_mand_eng_high_am-mix_vXXXXX.dat 为度逍遥模型文件;
    // assets目录下bd_etts_common_speech_as_mand_eng_high_am_vXXXX.dat 为度丫丫模型文件;
    // 在线合成sdk下面的参数不生效
    private String offlineVoice = OfflineResource.VOICE_DUXY;

    // ===============初始化参数设置完毕，更多合成参数请至getParams()方法中设置 =================

    private TTSHelper() {
    }

    private static class SingleInstance {
        private static TTSHelper INSTANCE = new TTSHelper();
    }

    public static TTSHelper getInstance() {
        return TTSHelper.SingleInstance.INSTANCE;
    }

    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return 合成参数Map
     */
    private Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        // 以下参数均为选填
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>, 其它发音人见文档
        params.put(SpeechSynthesizer.PARAM_SPEAKER, "3");
        // 设置合成的音量，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_VOLUME, "15");
        // 设置合成的语速，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_PITCH, "5");
        if (!isOnlineSDK) {
            // 在线SDK版本没有此参数。
            /*
            params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
            // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
            // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
            // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
            // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
            // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
            // params.put(SpeechSynthesizer.PARAM_MIX_MODE_TIMEOUT, SpeechSynthesizer.PARAM_MIX_TIMEOUT_TWO_SECOND);
            // 离在线模式，强制在线优先。在线请求后超时2秒后，转为离线合成。
            */
            // 离线资源文件， 从assets目录中复制到临时目录，需要在initTTs方法前完成
            OfflineResource offlineResource = createOfflineResource(offlineVoice);
            // 声学模型文件路径 (离线引擎使用), 请确认下面两个文件存在
            params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, offlineResource.getTextFilename());
            params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, offlineResource.getModelFilename());
        }
        return params;
    }

    private OfflineResource createOfflineResource(String voiceType) {
        OfflineResource offlineResource = null;
        try {
            offlineResource = new OfflineResource(ContextUtils.getContext(), voiceType);
        } catch (IOException e) {
            // IO 错误自行处理
            e.printStackTrace();
            Log.e(TAG, "【error】: copy files from assets failed. " + e.getMessage());
        }
        return offlineResource;
    }

    public void autoCheck() {
        AutoCheck.getInstance(ContextUtils.getContext()).check(mTTSConfig, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainAllMessage();
                        Log.w(TAG, "AutoCheckMessage: " + message);
                    }
                }
            }

        });
    }

    /**
     * 在线合成sdk，这个方法不会被调用。
     *
     * 切换离线发音。注意需要添加额外的判断：引擎在合成时该方法不能调用
     */
    private void loadModel(String mode) {
        offlineVoice = mode;
        OfflineResource offlineResource = createOfflineResource(offlineVoice);
        Log.i(TAG, "切换离线语音：" + offlineResource.getModelFilename());
        int result = mSpeechSynthesizer.loadModel(offlineResource.getModelFilename(), offlineResource.getTextFilename());
        if (result != 0) {
            Log.e(TAG, "loadModel error code :" + result);
        }
    }

    public void initTTS(TTSListener ttsListener) {
        this.mTTSListener = ttsListener;
        Map<String, String> params = getParams();
        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        if (isOnlineSDK) {
            mTTSConfig = new TTSConfig(Constants.TTS_STT_APP_ID, Constants.TTS_STT_APP_KEY, Constants.TTS_STT_SECRET_KEY, ttsMode, params, this);
        } else {
            mTTSConfig = new TTSConfig(Constants.TTS_STT_APP_ID, Constants.TTS_STT_APP_KEY, Constants.TTS_STT_SECRET_KEY, Constants.TTS_STT_REDMI_10X_SN, ttsMode, params, this);
        }
        autoCheck();
        initEngine();
    }

    public void initEngine() {
        if (!isOnlineSDK) {
            Log.i(TAG, "so version:" + SynthesizerTool.getEngineInfo());
        }
        releaseEngine();
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(ContextUtils.getContext());
        Log.i(TAG, "PackageName: " + ContextUtils.getContext().getPackageName());
        mSpeechSynthesizer.setSpeechSynthesizerListener(this);

        // AppID, AppKey, SecretKey
        mSpeechSynthesizer.setAppId(mTTSConfig.getAppId());
        mSpeechSynthesizer.setApiKey(Constants.TTS_STT_APP_KEY, Constants.TTS_STT_SECRET_KEY);
        Log.i(TAG, "AppID: " + mTTSConfig.getAppId() + ", AppKey: " + Constants.TTS_STT_APP_KEY + "SecretKey: " + Constants.TTS_STT_SECRET_KEY);

        for (Map.Entry<String, String> e : mTTSConfig.getParams().entrySet()) {
            mSpeechSynthesizer.setParam(e.getKey(), e.getValue());
        }

        // 初始化tts
        int result = mSpeechSynthesizer.initTts(mTTSConfig.getTtsMode());
        if (result != 0) {
            Log.i(TAG, "【error】initTts 初始化失败 + errorCode：" + result);
            mTTSListener.onTTSInitFailed();
            isInitialized = false;
        } else {
            mTTSListener.onTTSInitSuccess();
            isInitialized = true;
        }
    }

    public void releaseEngine() {
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.stop();
            mSpeechSynthesizer.release();
            mSpeechSynthesizer = null;
            isInitialized = false;
        }
    }

    public void releaseTTS() {
        releaseEngine();
        mTTSListener = null;
        mTTSConfig = null;
    }

    /**
     * 合成并播放
     * @param text 小于1024 GBK字节，即512个汉字或者字母数字
     * @return =0表示成功
     */
    public int speak(String text) {
        if (isInitialized) {
            Log.i(TAG, "speak text:" + text);
            return mSpeechSynthesizer.speak(text);
        } else {
            return ERROR_CODE;
        }
    }

    /**
     * 合成并播放
     *
     * @param text        小于1024 GBK字节，即512个汉字或者字母数字
     * @param utteranceId 用于listener的回调，默认"0"
     * @return =0表示成功
     */
    public int speak(String text, String utteranceId) {
        if (isInitialized) {
            Log.i(TAG, "speak text:" + text + ", utteranceId: " + utteranceId);
            return mSpeechSynthesizer.speak(text, utteranceId);
        } else {
            return ERROR_CODE;
        }
    }

    /**
     * 只合成不播放
     *
     * @param text 合成的文本
     * @return =0表示成功
     */
    public int synthesize(String text) {
        if (isInitialized) {
            Log.i(TAG, "synthesize text:" + text);
            return mSpeechSynthesizer.synthesize(text);
        } else {
            return ERROR_CODE;
        }
    }

    /**
     * 只合成不播放
     *
     * @param text 合成的文本
     * @param utteranceId 用于listener的回调，默认"0"
     * @return =0表示成功
     */
    public int synthesize(String text, String utteranceId) {
        if (isInitialized) {
            Log.i(TAG, "synthesize text:" + text + ", utteranceId: " + utteranceId);
            return mSpeechSynthesizer.synthesize(text, utteranceId);
        } else {
            return ERROR_CODE;
        }
    }

    public int batchSpeak(List<Pair<String, String>> texts) {
        if (isInitialized) {
            List<SpeechSynthesizeBag> bags = new ArrayList<SpeechSynthesizeBag>();
            for (Pair<String, String> pair : texts) {
                Log.i(TAG, "batchSpeak text:" + texts);
                SpeechSynthesizeBag speechSynthesizeBag = new SpeechSynthesizeBag();
                speechSynthesizeBag.setText(pair.first);
                if (pair.second != null) {
                    speechSynthesizeBag.setUtteranceId(pair.second);
                }
                bags.add(speechSynthesizeBag);
            }
            return mSpeechSynthesizer.batchSpeak(bags);
        } else {
            return ERROR_CODE;
        }
    }

    public int pause() {
        return mSpeechSynthesizer.pause();
    }

    public int resume() {
        return mSpeechSynthesizer.resume();
    }

    public int stop() {
        return mSpeechSynthesizer.stop();
    }

    /**
     * 播放开始，每句播放开始都会回调
     *
     * @param utteranceId
     */
    @Override
    public void onSynthesizeStart(String utteranceId) {
        Log.i(TAG, "准备开始合成, 序列号: " + utteranceId);
        mTTSListener.onTTSSynthesizeStart(utteranceId);
    }

    /**
     * 语音流 16K采样率 16bits编码 单声道 。
     *
     * @param utteranceId
     * @param bytes       二进制语音 ，注意可能有空data的情况，可以忽略
     * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法和合成到第几个字对应。
     *                    engineType 下版本提供。1:音频数据由离线引擎合成； 0：音频数据由在线引擎（百度服务器）合成。
     */
    public void onSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress) {
        Log.i(TAG, "合成进度回调, progress：" + progress + ";序列号:" + utteranceId);
        // + ";" + (engineType == 1? "离线合成":"在线合成"));
        mTTSListener.onTTSSynthesizeDataArrived(utteranceId, bytes, progress);
    }

    @Override
    // engineType 下版本提供。1:音频数据由离线引擎合成； 0：音频数据由在线引擎（百度服务器）合成。
    public void onSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress, int engineType) {
        onSynthesizeDataArrived(utteranceId, bytes, progress);
    }

    /**
     * 合成正常结束，每句合成正常结束都会回调，如果过程中出错，则回调onError，不再回调此接口
     *
     * @param utteranceId
     */
    @Override
    public void onSynthesizeFinish(String utteranceId) {
        Log.i(TAG, "合成结束回调, 序列号:" + utteranceId);
        mTTSListener.onTTSSynthesizeFinish(utteranceId);
    }

    @Override
    public void onSpeechStart(String utteranceId) {
        Log.i(TAG, "播放开始回调, 序列号: " + utteranceId);
        mTTSListener.onTTSSpeechStart(utteranceId);
    }

    /**
     * 播放进度回调接口，分多次回调
     *
     * @param utteranceId
     * @param progress    如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法保证和合成到第几个字对应。
     */
    @Override
    public void onSpeechProgressChanged(String utteranceId, int progress) {
        Log.i(TAG, "播放进度回调, progress: " + progress + "; 序列号: " + utteranceId);
        mTTSListener.onTTSSpeechProgressChanged(utteranceId, progress);
    }

    /**
     * 播放正常结束，每句播放正常结束都会回调，如果过程中出错，则回调onError,不再回调此接口
     *
     * @param utteranceId
     */
    @Override
    public void onSpeechFinish(String utteranceId) {
        Log.i(TAG, "播放结束回调, 序列号: " + utteranceId);
        mTTSListener.onTTSSpeechFinish(utteranceId);
    }

    /**
     * 当合成或者播放过程中出错时回调此接口
     *
     * @param utteranceId
     * @param speechError 包含错误码和错误信息
     */
    @Override
    public void onError(String utteranceId, SpeechError speechError) {
        Log.i(TAG, "错误发生: " + speechError.description + ", 错误编码: " + speechError.code + ", 序列号: " + utteranceId);
        mTTSListener.onTTSError(utteranceId, speechError);
    }
}
