package com.example.enactusapp.TTS.Listener;

import com.baidu.tts.client.SpeechError;

public interface TTSListener {

    // TTS Engine
    void onTTSInitSuccess();
    void onTTSInitFailed();
    void onTTSSynthesizeStart(String utteranceId);
    void onTTSSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress);
    void onTTSSynthesizeFinish(String utteranceId);
    void onTTSSpeechStart(String utteranceId);
    void onTTSSpeechProgressChanged(String utteranceId, int progress);
    void onTTSSpeechFinish(String utteranceId);
    void onTTSError(String utteranceId, SpeechError speechError);
}
