package com.glowingsoft.carplaterecognizer.DriverUser.Fragments;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.DriverUser.Activities.BookParkingArea;
import com.glowingsoft.carplaterecognizer.DriverUser.Activities.GPSMap;
import com.glowingsoft.carplaterecognizer.DriverUser.Activities.NearByArea;
import com.glowingsoft.carplaterecognizer.DriverUser.Activities.UserHistory;
import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlots;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.ui.MainActivity;
import com.glowingsoft.carplaterecognizer.utils.AppConstants;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.services.MyParkingService;
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

public class DriverDashboard extends Fragment implements OnMapReadyCallback {

    TextView messageText;
    ImageView toastIcon;
    LinearLayout openMapsButton, nearByButton, myBookingsButton, scanPlateButton;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    GoogleMap gMap;
    LatLng globalLatLng;

    HashMap<String, ParkingArea> parkingAreasList = new HashMap<String,ParkingArea>();
    ImageButton getLocationButton;
    BasicUtils utils = new BasicUtils();

    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    private static final int LOCATION_REQUEST_CODE = 100;

    Button checkServiceButton;

    AppConstants globalClass;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View root = inflater.inflate(R.layout.fragment_driver_dashboard, container, false);




        initComponents(root);
        attachListeners();
        if(!utils.isNetworkAvailable(getActivity().getApplication())){
            showToast("No network available");
        }
        return root;

    }

    private void initComponents(View root) {
        openMapsButton = root.findViewById(R.id.open_maps_button);
        nearByButton = root.findViewById(R.id.near_by_button);
        myBookingsButton = root.findViewById(R.id.my_bookings_button);
        checkServiceButton = root.findViewById(R.id.check_service_button);
        getLocationButton = root.findViewById(R.id.get_location_button);
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();



        supportMapFragment=(SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);
        client= LocationServices.getFusedLocationProviderClient(getActivity());

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
        Toast toast = new Toast(getActivity());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }

    private void attachListeners() {
        firebaseDatabase.getReference().child("ParkingAreas")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ParkingArea parkingArea = dataSnapshot.getValue(ParkingArea.class);
                            parkingAreasList.put(dataSnapshot.getKey(),parkingArea);
//                            Log.d(String.valueOf(getActivity().getClass()),"GPS Map: "+parkingArea.name);
                        }
                        attachMarkerOnMap();
                        Log.d(String.valueOf(getActivity().getClass()),"GPS Map list"+ String.valueOf(parkingAreasList));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        getLocationButton.setOnClickListener(view -> supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap = googleMap;
                gMap.clear();
                getPreCurrentLocation();
                attachMarkerOnMap();
            }
        }));


        openMapsButton.setOnClickListener(view-> {
            startActivity(new Intent(getActivity(), GPSMap.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        nearByButton.setOnClickListener(view-> {
            startActivity(new Intent(getActivity(), NearByArea.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        myBookingsButton.setOnClickListener(view-> {
            startActivity(new Intent(getActivity(), UserHistory.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        checkServiceButton.setOnClickListener(view -> {
            if(!isMyServiceRunning(MyParkingService.class))
                showToast("Service is not running");
            else
                showToast("Service is running");
        });

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void attachMarkerOnMap() {
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap=googleMap;
                for (Map.Entry<String, ParkingArea> stringParkingAreaEntry : parkingAreasList.entrySet()) {
                    Map.Entry mapElement = (Map.Entry) stringParkingAreaEntry;
                    ParkingArea parking = (ParkingArea) mapElement.getValue();
                    Log.e(String.valueOf(getActivity().getClass()),"Add marker on map: "+parking.name);
                    LatLng latLngParking = new LatLng(parking.latitude,
                            parking.longitude);
                    MarkerOptions option = new MarkerOptions().position(latLngParking)
                            .title(mapElement.getKey().toString())
                            .snippet(parking.name);
                    gMap.addMarker(option);
                }
            }
        });
    }

    private void getPreCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            getCurrentLocation(true);
        }else{
            ActivityCompat.requestPermissions(getActivity(),new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }
    private void getCurrentLocation(final Boolean zoom) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),new String[]
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
                                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(globalLatLng,18));
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode== LOCATION_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation(true);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        gMap = googleMap;

        gMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            public void onInfoWindowClick(final Marker marker) {
                LatLng position = marker.getPosition();
                Log.d(String.valueOf(getActivity().getClass()),"Compare Location: "+globalLatLng.latitude+","+globalLatLng.longitude+" | "+position.latitude+","+position.longitude);
                if (position.equals(globalLatLng)) {
                    return;
                }
                String[] items={"Book Place","More Info"};
                androidx.appcompat.app.AlertDialog.Builder itemDilog = new AlertDialog.Builder(getActivity());
                itemDilog.setTitle("");
                itemDilog.setCancelable(true);
                itemDilog.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which){
                            case 0:{
                                Log.d(getActivity().getComponentName().getClassName(),"Book Place");
                                String UUID = marker.getTitle();
                                ParkingArea val = (ParkingArea)parkingAreasList.get(UUID);
                                Intent intent = new Intent(getActivity(), BookParkingArea.class);
                                intent.putExtra("UUID", UUID);
                                intent.putExtra("ParkingArea", val);
                                startActivity(intent);
                                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                Log.d(String.valueOf(getActivity().getClass()), "Value of UUID: "+UUID);
                            }break;
                            case 1:{
                                Log.d(String.valueOf(getActivity().getClass()),"More Info");
                            }break;
                        }

                    }
                });
                itemDilog.show();

            }
        });

    }
}