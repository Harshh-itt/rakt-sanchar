package com.example.blood.util;

/**
 * Pure Java Haversine distance utility for in-memory nearby matching and stock sorting.
 * No external or paid geolocation APIs are used.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtils() {
    }

    public static boolean hasCoordinates(Double lat, Double lon) {
        return lat != null && lon != null
                && !lat.isNaN() && !lon.isNaN()
                && lat >= -90.0 && lat <= 90.0
                && lon >= -180.0 && lon <= 180.0;
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2.0), 2)
                + Math.cos(rLat1) * Math.cos(rLat2) * Math.pow(Math.sin(dLon / 2.0), 2);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }
}
