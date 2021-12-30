package com.example.enactusapp.Service;

import androidx.annotation.NonNull;

import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.NotificationEvent;
import com.example.enactusapp.Event.UpdateTokenEvent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.File;

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
    public void onNewToken(@NonNull String token) {
        EventBus.getDefault().post(new UpdateTokenEvent(token));
    }

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
                String thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + id + ".jpg";
                User user = new User(id, username, name, thumbnail, firebaseToken, longitude, latitude);
                if (type.equals(MessageType.GREETING.getValue())) {
                    EventBus.getDefault().post(new NotificationEvent(user, message, MessageType.GREETING));
                } else {
                    EventBus.getDefault().post(new NotificationEvent(user, message, MessageType.NORMAL));
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
