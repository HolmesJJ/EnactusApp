package com.example.enactusapp.Utils;

import android.location.Location;

public class CalculateUtils {

    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results=new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000;
    }
}
