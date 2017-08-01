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

public class MainActivity extends FragmentActivity implements LocationListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int LOCATION_PERMISSION_REQUEST = 7389;
    private final static int TARGET_INTENT = 9893;

    private GoogleApiClient googleApiClient;
    private GoogleMap googleMap;

    private double latitude;
    private double longitude;
    private float degrees;

    private int range;
    private LatLng target;
    private TextView seekLabel;

    private DatabaseReference firebaseDB;

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDB = FirebaseDatabase.getInstance().getReference("coordinates");

        setupMap();
        setupSeekBar();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    private void setupMap() {
        // Create a GoogleAPIClient instance
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Set this activity to listen to map callbacks
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(this);
    }

    // Distance SeekBar
    public void setupSeekBar() {

        // Distance variables
        int minDist = 0;
        int maxDist = 5000;

        // Set distance string as minDist
        seekLabel = (TextView) findViewById(R.id.seekLabel);
        seekLabel.setText(getLabelFor(minDist));

        // Set max value
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax(maxDist);

        // When the SeekBar is slided,
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                range = progress;
                updateSeekLabel();
                updateMarker();
            }

            private void updateSeekLabel() {
                seekLabel.setText(getLabelFor(range));
            }

            private void updateMarker() {
                target = createTargetFor(range);
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(target).title("Target Location"));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private String getLabelFor(int minDist) {
        return String.format(getResources().getString(R.string.distance), minDist);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Instantiate map object
        this.googleMap = googleMap;

        // Check permission for setMyLocationEnabled
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        // Using MyLocation feature
        this.googleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        updateCoordinates(location);

        // Move camera to here
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(latitude, longitude), googleMap.getCameraPosition().zoom));
    }

    // Store coordinates from current location
    private void updateCoordinates(Location loc) {
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        degrees = googleMap.getCameraPosition().bearing;
    }

    // Compute cartesian coordinate from polar coordinate
    private LatLng createTargetFor(int range) {

        float radius = 6371;

        double distance = (double) range / 1000;

        //New latitude in degrees
        double new_latitude = radToDeg(Math.asin(Math.sin(degToRad(latitude)) *
                Math.cos(distance / radius) + Math.cos(degToRad(latitude)) *
                Math.sin(distance / radius) * Math.cos(degToRad(degrees))));

        //	New longitude in degrees.
        double new_longitude = radToDeg(degToRad(longitude) + Math.atan2(Math.sin(degToRad(degrees)) *
                Math.sin(distance / radius) *  Math.cos(degToRad(latitude)),
                Math.cos(distance / radius) - Math.sin(degToRad(latitude)) *
                        Math.sin(degToRad(new_latitude))));

        // Return instantiated LatLng object
        return new LatLng(new_latitude, new_longitude);
    }

    private double degToRad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double radToDeg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Instantiate LocationRequest object and its set properties
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        // To request location updates
        LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
        if (locationAvailability.isLocationAvailable()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public void showMyTargets(View view) {
        // Create intent for finger drawing
        Intent resultIntent = new Intent(MainActivity.this, TargetActivity.class);
        startActivityForResult(resultIntent, TARGET_INTENT);
    }

    public void persistCoordinate(View view) {
        String id = firebaseDB.push().getKey();
        Coordinate coordinate = new Coordinate(id, target.latitude, target.longitude);
        firebaseDB.child(id).setValue(coordinate);

        Toast.makeText(this, "Target persisted on Firebase...", Toast.LENGTH_LONG).show();
    }
}