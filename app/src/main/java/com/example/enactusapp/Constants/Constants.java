package com.example.enactusapp.Constants;

public class Constants {

    public static final String SERVER = "PHP"; // PHP or Microsoft SQL Server (MS)
    public static final String IP_ADDRESS = "http://8.214.40.74/";
    public static final String SMART_ANSWERING_IP_ADDRESS = "https://silverplus-chatbot.azurewebsites.net/qnamaker/knowledgebases/65148328-b5b2-4ce6-831c-50499e296d93/generateAnswer";
    public static final String SMART_ANSWERING_TOKEN = "EndpointKey 805d2b34-f07e-4148-93c2-bbd0e7317f72";
    public static final String FIREBASE_ADDRESS = "https://fcm.googleapis.com/fcm/send";
    public static final String SERVER_KEY = "key=" + "AAAApCBXmvo:APA91bE9AqJEgc3vlpLru-Zh53gQec5T8XtTHxBhKBihFFWIgdrPpAjvk6R5AGCGeTGKsbYvw8r2b0wUtjNb2y0FcE-gh5KCzxh_c-O638BKyGi29ek74WjIYQq6tBECJRB2OYuq50ca";

    public static final int UNSPECIFIED_USER_ID = -1;
    public static final String UNSPECIFIED_FIREBASE_TOKEN = "unspecified_token";

    // GazeService
    public static final int GAZE_SERVICE_CHANNEL_ID = 101;
    public static final String GAZE_SERVICE_CHANNEL = "GazeServiceChannel";
    public static final String GAZE_SERVICE_START = "GazeServiceStart";
    public static final String GAZE_SERVICE_STOP = "GazeServiceStop";

    // WebSocketService
    public static final int WEB_SOCKET_SERVICE_CHANNEL_ID = 102;
    public static final String WEB_SOCKET_SERVICE_CHANNEL = "WebSocketServiceChannel";
    public static final String WEB_SOCKET_SERVICE_START = "WebSocketServiceStart";
    public static final String WEB_SOCKET_SERVICE_STOP = "WebSocketServiceStop";

    // Fragment ID
    public static final int CONTACT_FRAGMENT_ID = 0;
    public static final int OBJECT_DETECTION_FRAGMENT_ID = 1;
    public static final int PROFILE_FRAGMENT_ID = 2;
    public static final int NOTIFICATION_FRAGMENT_ID = 100;
    public static final int DIALOG_FRAGMENT_ID = 101;
    public static final int BLUETOOTH_FRAGMENT_ID = 102;

    // GAZE密钥
    public static final String GAZE_LICENSE_KEY = "dev_4wgm4yzqvyk2gdnofwonumr2zwxci8ntw1m6mqyh";

    // TTS密钥
    public static final String TTS_STT_APP_ID = "23861843";
    public static final String TTS_STT_APP_KEY = "D3nC11vOUxWW3azo9o3znO8h";
    public static final String TTS_STT_SECRET_KEY = "l1z1zrxebAuHdlbPennGEpp7EnjSBruU";
    public static final String TTS_STT_REDMI_10X_SN = "9831ff5c-704b2ee1-07d1-0046-3a47b-00";
    public static final String TTS_HUAWEI_MATE30PRO_SN = "7c306c71-4f2a9433-07d1-00e1-3a47c-00";

    public Constants() {
    }
}
