package ryu.j.assignment3;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

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

public class MapsActivity extends FragmentActivity implements LocationListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Request code for location permission request.
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 7389;
    // Request code for ResultActivity intent
    private final static int RESULT_ON_MAP = 9893;

    // Initial location: Kitchener, ON
    private static LatLng INIT_LOCATION = new LatLng(43.449629, -80.484555);
    //private static LatLng INIT_LOCATION = new LatLng(43.3898641, -80.40478200000001);
    private static int ZOOM_LEVEL = 17;
    private static double METRE2DEG = 0.00001;

    // Google maps variables
    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;

    // Coordinate variables
    private double lastLatitude;
    private double lastLongitude;
    private double lastBearing;

    // Variables for user custom position
    private int range;
    private LatLng target;
    private TextView txvRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setupMap();

        // Set distance SeekBar on
        setupSeekBar();
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
        int maxDist = 100;

        // Set distance string as minDist
        txvRange = (TextView) findViewById(R.id.distanceIndicator);
        txvRange.setText(getLabelFor(minDist));

        // Set max value
        SeekBar rangeBar = (SeekBar) findViewById(R.id.setDistance);
        rangeBar.setMax(maxDist);

        // When the SeekBar is slided,
        rangeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txvRange.setText(getLabelFor(range));

                range = progress;

                target = createTargetFor(range);
                setUserMarker(target);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private String getLabelFor(int minDist) {
        return String.format(getResources().getString(R.string.distance), minDist);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Instantiate map object
        mMap = googleMap;

        // Check permission for setMyLocationEnabled
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        // Using MyLocation feature
        mMap.setMyLocationEnabled(true);
        // Show huge scale map centred on INIT_LOCATION
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(INIT_LOCATION, 2));
    }

    @Override
    public void onLocationChanged(Location location) {
        // Clear markers
        mMap.clear();
        // Get current coordinate
        updateCoordinates(location);

        // Move camera to here
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lastLatitude, lastLongitude), ZOOM_LEVEL));
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
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        // To request location updates
        LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient);
        if (locationAvailability.isLocationAvailable()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    // Store coordinates from current location
    private void updateCoordinates(Location loc) {
        lastLatitude = loc.getLatitude();
        lastLongitude = loc.getLongitude();
        lastBearing = loc.getBearing();
    }

    // Compute cartesian coordinate from polar coordinate
    private LatLng createTargetFor(int range) {
        double[] cartesian = new double[2];
        // Convert degree to radian
        double radian = ((lastBearing + 90) % 360) * (Math.PI / 180);

        // Latitude: Y
        cartesian[0] = lastLatitude + (range * Math.sin(radian) * METRE2DEG);
        // Longitude: X
        cartesian[1] = lastLongitude + (range * Math.cos(radian) * METRE2DEG);

        // Return instantiated LatLng object
        return new LatLng(cartesian[0], cartesian[1]);
    }

    // Set marker from user distance
    private void setUserMarker(LatLng target) {
        // Remove all markers
        mMap.clear();
        // Add new marker
        mMap.addMarker(new MarkerOptions().position(target).title("Target Location"));
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    // Create Activity
    public void setOnMap(View view) {
        // Location as double array
        double[] location = {target.latitude, target.longitude};

        // Create intent for finger drawing
        Intent resultIntent = new Intent(MapsActivity.this, ResultActivity.class);
        // Passing the coordinate
        resultIntent.putExtra("location", location);
        // Start the Activity
        startActivityForResult(resultIntent, RESULT_ON_MAP);
    }
}