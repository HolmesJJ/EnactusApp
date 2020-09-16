package com.example.enactusapp.Service;

import android.content.Intent;

import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Utils.ToastUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class FcmMessagingService extends FirebaseMessagingService {

    private int id;
    private String username;
    private String name;
    private String firebaseToken;
    private double longitude;
    private double latitude;
    private String message;
    private String body;
    private String type;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            System.out.println("MessageEvent Notification Body: " + remoteMessage.getNotification().getBody());
            body = remoteMessage.getNotification().getBody();
            type = remoteMessage.getNotification().getTitle();
        }

        if (body != null) {
            if (retrieveFromJSON(body)) {
                if (type.equals(MessageType.GREETING.getValue())) {
                    Intent notificationIntent = new Intent(MessageType.GREETING.getValue());
                    notificationIntent.putExtra("id", id);
                    notificationIntent.putExtra("username", username);
                    notificationIntent.putExtra("name", name);
                    notificationIntent.putExtra("firebaseToken", firebaseToken);
                    notificationIntent.putExtra("longitude", longitude);
                    notificationIntent.putExtra("latitude", latitude);
                    notificationIntent.putExtra("message", message);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationIntent);
                } else {
                    Intent notificationIntent = new Intent(MessageType.NORMAL.getValue());
                    notificationIntent.putExtra("id", id);
                    notificationIntent.putExtra("username", username);
                    notificationIntent.putExtra("name", name);
                    notificationIntent.putExtra("firebaseToken", firebaseToken);
                    notificationIntent.putExtra("longitude", longitude);
                    notificationIntent.putExtra("latitude", latitude);
                    notificationIntent.putExtra("message", message);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notificationIntent);
                }
            }
        }
    }

    private boolean retrieveFromJSON(String body) {
        try {
            JSONObject jsonBodyObject = new JSONObject(body);
            String from = jsonBodyObject.getString("from");
            message = jsonBodyObject.getString("message");
            JSONObject jsonFromObject = new JSONObject(from);
            id = jsonFromObject.getInt("id");
            username = jsonFromObject.getString("username");
            name = jsonFromObject.getString("name");
            firebaseToken = jsonFromObject.getString("firebaseToken");
            longitude = jsonFromObject.getDouble("longitude");
            latitude = jsonFromObject.getDouble("latitude");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
