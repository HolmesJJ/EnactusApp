package com.example.enactusapp.WebSocket;

import android.util.Log;

import com.example.enactusapp.WebSocket.Callback.IClientMessageCallback;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class MiniWebSocketClient extends WebSocketClient {

    private static final String TAG = MiniWebSocketClient.class.getSimpleName();

    private final WebSocketClientManager webSocketClientManager;

    public MiniWebSocketClient(WebSocketClientManager webSocketClientManager, URI serverURI) {
        super(serverURI);
        this.webSocketClientManager = webSocketClientManager;
    }

    public MiniWebSocketClient(WebSocketClientManager webSocketClientManager, URI serverUri, Draft draft) {
        super(serverUri, draft);
        this.webSocketClientManager = webSocketClientManager;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        webSocketClientManager.onOpen(serverHandshake);
        Log.i(TAG, "onOpen");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        webSocketClientManager.onClose(code, reason, remote);
        Log.i(TAG, "onClose with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        webSocketClientManager.onMessage(message);
        Log.i(TAG, "OnMessage String " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        webSocketClientManager.onMessage(message);
        Log.i(TAG, "OnMessage ByteBuffer " + message);
    }

    @Override
    public void onError(Exception ex) {
        webSocketClientManager.onError(ex);
        Log.i(TAG, "onError " + ex);
    }
}