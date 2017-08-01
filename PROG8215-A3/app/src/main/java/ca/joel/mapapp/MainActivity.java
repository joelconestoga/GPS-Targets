package ca.joel.mapapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

//Java class for the main activity logic
public class MainActivity extends FragmentActivity implements LocationListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private final static int TARGET_INTENT = 101;

    private GoogleApiClient googleApiClient;
    private GoogleMap googleMap;

    private double latitude;
    private double longitude;
    private float degrees;

    private int range;
    private LatLng target;
    private TextView seekLabel;

    private DatabaseReference firebaseDB;

    //Code required for multidex
    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    //App initialization, where the execution starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Code auto-generated, just kept unchanged
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instantiate a firebase reference
        firebaseDB = FirebaseDatabase.getInstance().getReference("coordinates");

        //Initializations
        setupMap();
        setupSeekBar();
    }

    //Connect with Google API
    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    //Disconnect from Google API
    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    //Setup Google Maps
    private void setupMap() {
        // Create Google API Client and add this activity as listener
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Bind Map Fragment to maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().
                findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(this);
    }

    //Setup the seekbar(slider)
    public void setupSeekBar() {

        //Boundaries
        int minDist = 0;
        int maxDist = 5000;

        //Seekbar label
        seekLabel = (TextView) findViewById(R.id.seekLabel);
        seekLabel.setText(getLabelFor(minDist));

        //Seekbar boundaries
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax(maxDist);

        //When seekbar is changed, update Label and Marker on Map
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                range = progress;
                updateSeekLabel();
                updateMarker();
            }

            //Updating the seekbar label
            private void updateSeekLabel() {
                seekLabel.setText(getLabelFor(range));
            }

            //Updating the Map Marker
            private void updateMarker() {
                target = createTargetFor(range);
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(target).title("Target Location"));
            }

            //Required methods - do nothing
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    //Formatting seekbar label
    private String getLabelFor(int minDist) {
        return String.format(getResources().getString(R.string.distance), minDist);
    }

    //Callback when the Map is ready (connected with Google API)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Keep a reference for the Google Map
        this.googleMap = googleMap;

        //Check permission for Device Location
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        //Activate Device Location
        this.googleMap.setMyLocationEnabled(true);
    }

    //Update coordinates and camera when the location is changed
    @Override
    public void onLocationChanged(Location location) {
        updateCoordinates(location);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(latitude, longitude), googleMap.getCameraPosition().zoom));
    }

    //Keep coordinates up to date
    private void updateCoordinates(Location loc) {
        //Get the coordinates
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();

        //Get the degrees in case the user rotated the Map
        degrees = googleMap.getCameraPosition().bearing;
    }

    //Compute the location for a new range (from the seekbar)
    private LatLng createTargetFor(int range) {

        float radius = 6371;

        double distance = (double) range / 1000;

        //New latitude in degrees
        double new_latitude = radToDeg(Math.asin(Math.sin(degToRad(latitude)) *
                Math.cos(distance / radius) + Math.cos(degToRad(latitude)) *
                Math.sin(distance / radius) * Math.cos(degToRad(degrees))));

        //New longitude in degrees
        double new_longitude = radToDeg(degToRad(longitude) + Math.atan2(Math.sin(degToRad(degrees)) *
                Math.sin(distance / radius) *  Math.cos(degToRad(latitude)),
                Math.cos(distance / radius) - Math.sin(degToRad(latitude)) *
                        Math.sin(degToRad(new_latitude))));

        //New position for the range provided
        return new LatLng(new_latitude, new_longitude);
    }

    //Conversions
    private double degToRad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    //Conversions
    private double radToDeg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    //Request Mobile location
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        //Create request
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);

        //Check permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        //Let the Google API knows the location
        LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
        if (locationAvailability.isLocationAvailable()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    //Keep connection with Google API
    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    //If connection fails, do nothing
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    //Show all my targets in another activity
    public void showMyTargets(View view) {
        Intent resultIntent = new Intent(MainActivity.this, TargetActivity.class);
        startActivityForResult(resultIntent, TARGET_INTENT);
    }

    //Save the position as a Target on Firebase and display confirmation
    public void persistCoordinate(View view) {
        String id = firebaseDB.push().getKey();
        Coordinate coordinate = new Coordinate(id, target.latitude, target.longitude);
        firebaseDB.child(id).setValue(coordinate);

        Toast.makeText(this, "Target persisted on Firebase...", Toast.LENGTH_LONG).show();
    }
}