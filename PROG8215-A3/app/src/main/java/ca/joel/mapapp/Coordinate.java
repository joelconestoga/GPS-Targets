package ca.joel.mapapp;

import java.util.Calendar;
import java.util.Date;

public class Coordinate {

    String id;
    Date timestamp;
    double latitude;
    double longitude;

    public Coordinate(String id, double latitude, double longitude) {
        this.id = id;
        this.timestamp = Calendar.getInstance().getTime();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
