package ca.joel.mapapp;

        import java.text.SimpleDateFormat;
        import java.util.Calendar;
        import java.util.Date;

//Java class to encapsulate/persist a Target
public class Coordinate {

    String id;
    Date timestamp;
    double latitude;
    double longitude;

    public Coordinate() {}

    public Coordinate(String id, double latitude, double longitude) {
        this.id = id;
        this.timestamp = Calendar.getInstance().getTime();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return new SimpleDateFormat("MM/dd/yyyy hh:mm:ss").format(timestamp);
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
