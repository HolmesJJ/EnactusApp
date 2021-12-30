package com.example.enactusapp.WebSocket;

import android.util.Log;

import com.example.enactusapp.Utils.ToastUtils;
import com.example.enactusapp.WebSocket.Callback.IClientMessageCallback;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class WebSocketClientManager {

    private static final String TAG = WebSocketClientManager.class.getSimpleName();

    private MiniWebSocketClient miniWebSocketClient = null;
    private IClientMessageCallback iClientMessageCallback;

    private boolean isConnected = false;

    public WebSocketClientManager() {

    }

    private static class SingleInstance {
        private static final WebSocketClientManager INSTANCE = new WebSocketClientManager();
    }

    public static WebSocketClientManager getInstance() {
        return WebSocketClientManager.SingleInstance.INSTANCE;
    }

    public void setClientMessageCallback(IClientMessageCallback iClientMessageCallback) {
        this.iClientMessageCallback = iClientMessageCallback;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect(String url) {
        try {
            isConnected = true;
            miniWebSocketClient = new MiniWebSocketClient(this, new URI(url));
            miniWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "URL error");
            isConnected = false;
        }
    }

    public void close() {
        if (miniWebSocketClient != null) {
            miniWebSocketClient.close();
            miniWebSocketClient = null;
        }
    }

    public void onOpen(ServerHandshake serverHandshake) {
        ToastUtils.showShortSafe("WS connected");
    }

    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        ToastUtils.showShortSafe("WS closed");
    }

    public void onMessage(String message) {
        if(iClientMessageCallback != null) {
            iClientMessageCallback.onMessage(message);
        }
    }

    public void onMessage(ByteBuffer message) {

    }

    public void onError(Exception ex) {
        isConnected = false;
        ToastUtils.showShortSafe("WS error " + ex.getMessage());
    }
}
