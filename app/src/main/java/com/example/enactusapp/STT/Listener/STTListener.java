package com.example.enactusapp.STT.Listener;

import com.example.enactusapp.STT.RecogResult;

public interface STTListener {

    // STT Engine
    void onSTTAsrReady();
    void onSTTAsrBegin();
    void onSTTAsrEnd();
    void onSTTAsrPartialResult(String[] results, RecogResult recogResult);
    void onSTTAsrOnlineNluResult(String nluResult);
    void onSTTAsrFinalResult(String[] results, RecogResult recogResult);
    void onSTTAsrFinish(RecogResult recogResult);
    void onSTTAsrFinishError(int errorCode, int subErrorCode, String descMessage, RecogResult recogResult);
    void onSTTAsrLongFinish();
    void onSTTAsrVolume(int volumePercent, int volume);
    void onSTTAsrAudio(byte[] data, int offset, int length);
    void onSTTAsrExit();
    void onSTTOfflineLoaded();
    void onSTTOfflineUnLoaded();
}
