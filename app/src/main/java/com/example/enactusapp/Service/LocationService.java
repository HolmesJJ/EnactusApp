package com.example.enactusapp.Service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;

import org.json.JSONObject;

public class LocationService extends Service implements OnTaskCompleted {

    private static final String TAG = "LocationService";
    private static final int UPDATE_LNG_LAT = 1;
    private static final long MIN_DISTANCE = 0;
    private static final long LOCATION_REQUEST_INTERVAL = 10000;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private LocationManager mLocationManager;
    private LocationListener mGPSListener;  // GPS监听
    private LocationListener mNetworkListener; // 网络监听
    private Criteria mCriteria;
    private Location mLocation;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mCriteria != null) {
            mCriteria = new Criteria();
            mCriteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
            mCriteria.setAltitudeRequired(false); // 不要求海拔信息
            mCriteria.setBearingRequired(false); // 不要求方位信息
            mCriteria.setCostAllowed(true); // 是否允许付费
            mCriteria.setPowerRequirement(Criteria.POWER_LOW); // 对电量的要求
        }
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // GPS监听
        mGPSListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isBetterLocation(location, mLocation)) {
                    mLocation = location;
                    Log.i(TAG, "GPS Lat:" + mLocation.getLatitude() + ", Lng:" + mLocation.getLongitude());
                    HttpAsyncTaskPost task = new HttpAsyncTaskPost(LocationService.this, UPDATE_LNG_LAT);
                    double longitude = (double) Math.round(mLocation.getLongitude() * 1000) / 1000; // 保留三位小数
                    double latitude = (double) Math.round(mLocation.getLatitude() * 1000) / 1000; // 保留三位小数
                    Config.setLongitude(longitude);
                    Config.setLatitude(latitude);
                    String jsonData = convertToJSONUpdateLngLat(Config.sUserId, longitude, latitude);
                    task.execute(Constants.IP_ADDRESS + "update_lng_lat.php", jsonData, null);
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        // 网络监听
        mNetworkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isBetterLocation(location, mLocation)) {
                    mLocation = location;
                    Log.i(TAG, "Network Lat:" + mLocation.getLatitude() + ", Lng:" + mLocation.getLongitude());
                    HttpAsyncTaskPost task = new HttpAsyncTaskPost(LocationService.this, UPDATE_LNG_LAT);
                    double longitude = (double) Math.round(mLocation.getLongitude() * 1000) / 1000; // 保留三位小数
                    double latitude = (double) Math.round(mLocation.getLatitude() * 1000) / 1000; // 保留三位小数
                    Config.setLongitude(longitude);
                    Config.setLatitude(latitude);
                    String jsonData = convertToJSONUpdateLngLat(Config.sUserId, longitude, latitude);
                    task.execute(Constants.IP_ADDRESS + "update_lng_lat.php", jsonData, null);
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }

        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        requestLocation();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        removeListener();
        super.onDestroy();
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // 获取最佳服务对象
        if (mCriteria != null) {
            String provider = mLocationManager.getBestProvider(mCriteria, true);
            if (provider != null) {
                mLocationManager.getLastKnownLocation(provider);
            }
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REQUEST_INTERVAL, MIN_DISTANCE, mGPSListener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_REQUEST_INTERVAL, MIN_DISTANCE, mNetworkListener);
    }

    public void removeListener() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mNetworkListener);
            mLocationManager.removeUpdates(mGPSListener);
            mLocationManager = null;
        }
    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use
        // the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be
            // worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
                .getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and
        // accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate
                && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private String convertToJSONUpdateLngLat(int userId, double longitude, double latitude) {
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("Id", userId);
            jsonMsg.put("Longitude", longitude);
            jsonMsg.put("Latitude", latitude);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONUpdateLngLat(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int code = jsonObject.getInt("code");
            String message = jsonObject.getString("message");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskCompleted(String response, int requestId) {
        if (requestId == UPDATE_LNG_LAT) {
            retrieveFromJSONUpdateLngLat(response);
        }
    }
}
