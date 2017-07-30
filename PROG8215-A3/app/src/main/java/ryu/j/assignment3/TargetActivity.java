package ryu.j.assignment3;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class TargetActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static int ZOOM_LEVEL = 17;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.resultMap);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng userLoc = getLocation();

        // Add a marker from main intent and move the camera
        mMap.addMarker(new MarkerOptions().position(userLoc)
                .title(String.format("Lat: %f, Lng: %f", userLoc.latitude, userLoc.longitude)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLoc, ZOOM_LEVEL));
    }

    // Return to main activity
    public void back2Main(View view) {
        // Finish the intent
        finish();
    }

    private LatLng getLocation() {
        // Intent from MainActivity
        Intent intent = getIntent();
        double[] coordinate = intent.getDoubleArrayExtra("location");

        // Return instantiated LatLng object
        return new LatLng(coordinate[0], coordinate[1]);
    }
}
