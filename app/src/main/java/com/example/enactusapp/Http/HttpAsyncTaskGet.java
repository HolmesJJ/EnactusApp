package com.example.enactusapp.Http;

import android.os.AsyncTask;
import android.util.Log;

import com.example.enactusapp.Listener.OnTaskCompleted;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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

public class HttpAsyncTaskGet extends AsyncTask<String, Void, String> {

    private final static String TAG = "HttpAsyncTaskGet";
    private OnTaskCompleted listener;
    private int requestId;

    public HttpAsyncTaskGet(OnTaskCompleted listener, int requestId) {
        this.listener = listener;
        this.requestId = requestId;
    }

    public static String GET(String urlString) {
        String result = "";
        try {
            Log.d(TAG, "Sending url["+urlString+"]");
            // create HttpPost
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = null;
            try {
                urlConnection.setRequestMethod("GET");
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
        return GET(urls[0]);
    }
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String response) {
        Log.d(TAG, response);
        listener.onTaskCompleted(response, requestId);
    }
}
