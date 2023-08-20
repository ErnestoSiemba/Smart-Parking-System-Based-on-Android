package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class GPSMap extends AppCompatActivity implements OnMapReadyCallback {

    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    Button getLocationButton;
    ImageView backImageView, settingsImageView, toastIcon;
    TextView messageText;

    GoogleMap gMap;
    LatLng globalLatLng;
    BasicUtils utils=new BasicUtils();

    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;

    LatLng globalLatLngIntent=null;
    MarkerOptions optionsIntent;

    HashMap<String, ParkingArea> parkingAreasList = new HashMap<String, ParkingArea>();

    String nameIntent;
    double latitudeIntent,longitudeIntent;

    private static final int LOCATION_REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_map);

        initComponents();
        attachListeners();
        getPreCurrentLocation();
        if(!utils.isNetworkAvailable(getApplication())){
            showToast("No network available. Turn on mobile data or WIFI");
        }
    }



    private void initComponents() {
        getLocationButton = findViewById(R.id.get_location_button);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);

        Intent intent=getIntent();
        nameIntent=intent.getStringExtra("LOCATION_NAME");
        latitudeIntent=intent.getDoubleExtra("LOCATION_LATITUDE",-1);
        longitudeIntent= intent.getDoubleExtra("LOCATION_LONGITUDE",-1);

        if(latitudeIntent != -1){
            globalLatLngIntent=new LatLng(latitudeIntent,longitudeIntent);
            optionsIntent=new MarkerOptions().position(globalLatLngIntent)
                    .title(nameIntent);
        }

        supportMapFragment=(SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);
        client= LocationServices.getFusedLocationProviderClient(GPSMap.this);

    }
    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });
        firebaseDatabase.getReference().child("ParkingAreas")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ParkingArea parkingArea = dataSnapshot.getValue(ParkingArea.class);
                            parkingAreasList.put(dataSnapshot.getKey(),parkingArea);
                            Log.d(GPSMap.this.getComponentName().getClassName(), "Fetch Parking Area: "+parkingArea.name);
                        }
                        attachMarkerOnMap();
                        Log.d(GPSMap.this.getComponentName().getClassName(), "Fetch Parking Area list: "+String.valueOf(parkingAreasList));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        gMap = googleMap;
                        gMap.clear();
                        getPreCurrentLocation();
                        attachMarkerOnMap();
                    }
                });
            }
        });

    }

    private void attachMarkerOnMap() {
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap=googleMap;
                if(globalLatLngIntent != null){
                    MarkerOptions options=new MarkerOptions().position(globalLatLngIntent)
                            .title(nameIntent);
                    gMap.addMarker(options);
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(globalLatLngIntent,30));
                }else{
                    for (Map.Entry<String, ParkingArea> stringParkingAreaEntry : parkingAreasList.entrySet()) {
                        Map.Entry mapElement = (Map.Entry) stringParkingAreaEntry;
                        ParkingArea parking = (ParkingArea) mapElement.getValue();
                        Log.e(GPSMap.this.getComponentName().getClassName(), "Add Marker: "+parking.name);
                        LatLng latLngParking = new LatLng(parking.latitude,
                                parking.longitude);
                        MarkerOptions option = new MarkerOptions().position(latLngParking)
                                .title(mapElement.getKey().toString())
                                .snippet(parking.name);
                        gMap.addMarker(option);
                    }
                }
            }
        });
    }

    private void getPreCurrentLocation() {

        if(ActivityCompat.checkSelfPermission(GPSMap.this,
                Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            getCurrentLocation(true);
        }else{
            ActivityCompat.requestPermissions(GPSMap.this,new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }


    }

    private void getCurrentLocation(final Boolean zoom) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        Task<Location> task= client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if(location!=null){
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            gMap=googleMap;
                            globalLatLng=new LatLng(location.getLatitude(),
                                    location.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(globalLatLng).title("I am here"));
                            if(zoom){
                                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(globalLatLng,30));
                            }
                        }
                    });
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== LOCATION_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation(true);
            }
        }
    }

    private void showToast(String message) {
        // inflate the custom toast layout
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast_layout, null);

        // set the message text
        messageText = toastView.findViewById(R.id.toast_text);
        messageText.setText(message);

        // set the app icon
        toastIcon = toastView.findViewById(R.id.toast_icon);
        toastIcon.setImageResource(R.drawable.app_logo);

        // create the toast and set its custom layout
        Toast toast = new Toast(GPSMap.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            public void onInfoWindowClick(final Marker marker) {
                LatLng position = marker.getPosition();
                Log.d(String.valueOf(GPSMap.this.getClass()),"Compare Location: "+globalLatLng.latitude+","+globalLatLng.longitude+" | "+position.latitude+","+position.longitude);
                if (position.equals(globalLatLng)) {
                    return;
                }
                String[] items={"Book Place","More Info"};
                AlertDialog.Builder itemDilog = new AlertDialog.Builder(GPSMap.this);
                itemDilog.setTitle("");
                itemDilog.setCancelable(true);
                itemDilog.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which){
                            case 0:{
                                Log.d(String.valueOf(GPSMap.this.getClass()),"Book Place");
                                String UUID = marker.getTitle();
                                ParkingArea val = (ParkingArea)parkingAreasList.get(UUID);
                                Intent intent = new Intent(GPSMap.this, BookParkingArea.class);
                                intent.putExtra("UUID", UUID);
                                intent.putExtra("ParkingArea", val);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                Log.d(String.valueOf(GPSMap.this.getClass()), "Value of UUID: "+UUID);
                            }break;
                            case 1:{
                                Log.d(String.valueOf(GPSMap.this.getClass()),"More Info");
                            }break;
                        }

                    }
                });
                itemDilog.show();

            }
        });

    }
}