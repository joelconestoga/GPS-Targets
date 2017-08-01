package ca.joel.mapapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class TargetActivity extends FragmentActivity implements OnMapReadyCallback {

    private LatLng lastTarget;

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
        Toast.makeText(this, "Downloading saved targets...", Toast.LENGTH_LONG).show();
        downloadTargets(googleMap);
    }

    private void downloadTargets(final GoogleMap googleMap) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child("coordinates").orderByChild("timestamp");
        query.addListenerForSingleValueEvent(createDBCallbackListener(googleMap));
    }

    @NonNull
    private ValueEventListener createDBCallbackListener(final GoogleMap googleMap) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot json : dataSnapshot.getChildren()) {
                        Coordinate coordinate = json.getValue(Coordinate.class);
                        String label = getLabelFor(coordinate);
                        setLastTarget(coordinate);
                        createMapMakerWith(label);
                    }
                    repositionCamera();
                }
            }

            @NonNull
            private String getLabelFor(Coordinate coordinate) {
                return "Target: Lat(" + coordinate.getLatitude() +
                        "), Lng(" + coordinate.getLongitude() + ")";
            }

            private void setLastTarget(Coordinate coordinate) {
                lastTarget = new LatLng(coordinate.getLatitude(), coordinate.getLongitude());
            }

            private void createMapMakerWith(String label) {
                Marker marker = googleMap.addMarker(new MarkerOptions().position(lastTarget).title(label));
                marker.showInfoWindow();
            }

            private void repositionCamera() {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastTarget, 5));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
    }

    // Return to main activity
    public void backToMain(View view) {
        finish();
    }
}
