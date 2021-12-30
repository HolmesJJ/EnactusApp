package com.example.enactusapp.Listener;

public interface OnTaskCompleted {
    void onTaskCompleted(String response, int requestId, String... others);
}
