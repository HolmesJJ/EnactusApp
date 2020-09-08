package com.example.enactusapp.Service;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class FcmMessagingService extends FirebaseMessagingService {

    private String message;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            System.out.println("MessageEvent data payload: " + remoteMessage.getData());
            message = remoteMessage.getData().toString();
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            System.out.println("MessageEvent Notification Body: " + remoteMessage.getNotification().getBody());
            message = remoteMessage.getNotification().getBody();
        }

        if(message != null && message.equals("user2ChatWithYou")) {
            Intent notificationIntent = new Intent("getChatIntent");
            notificationIntent.putExtra("chat", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationIntent);
        }
        else if(message != null && message.equals("user1ChatWithYou")) {
            Intent notificationIntent = new Intent("getChatIntent");
            notificationIntent.putExtra("chat", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationIntent);
        }
        else {
            Intent notificationIntent = new Intent("getMessageIntent");
            notificationIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationIntent);
        }
    }
}
