package com.example.enactusapp.Http;

import android.os.AsyncTask;
import android.util.Log;

import com.example.enactusapp.Listener.OnTaskCompleted;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */

public class HttpAsyncTaskPost extends AsyncTask<String, Void, String> {

    private final static String TAG = "HttpAsynTaskPost";
    private final static String basicAuth = "key=AAAATXQDbI0:APA91bH_feXkLVx3JfZOdEJ2azTPegMuRcpd65NOV2MGRCBsixeREJ5beX5BZzueNxssd8839iEL71OR-oZiJVT_b-mYeymqBXdJfy85UicC9kE2nYqyD8Fbj2I6HmGGGls07r5UAo4q";
    private OnTaskCompleted listener;

    public HttpAsyncTaskPost(OnTaskCompleted listener) {
        this.listener=listener;
    }

    public static String POST(String urlString, String data) {
        String result = "";
        try {
            Log.d(TAG, "Sending data["+data+"]");
            Log.d(TAG, "Sending url["+urlString+"]");
            // create HttpPost
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = null;
            try {
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", basicAuth);
                DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
                outputStream.write(data.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();
                // receive response as inputStream
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
                if (inputStream != null)
                    // convert inputstream to string
                    result = convertInputStreamToString(inputStream);
                else
                    result = "Did not work!";
            } finally {
                if (inputStream!=null)
                    inputStream.close();
                if (urlConnection!=null)
                    urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream)throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }

    // doInBackground execute tasks when asynctask is run
    @Override
    protected String doInBackground(String... urls) {
        return POST(urls[0], urls[1]);
    }
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String response) {
        Log.d(TAG, response);
        listener.onTaskCompleted(response);
    }
}
