package com.glowingsoft.carplaterecognizer.OwnerUser.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.HelperClasses.SlotNoInfo;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.AppConstants;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.terminator.ChipTerminatorHandler;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AddParkingPosition extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST_CODE = 100;
    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    GoogleMap gMap;
    FloatingActionButton addLocationButton;
    TextView messageText;
    ImageView toastIcon;
    TextInputEditText areaNameEditText, totalSlotsEditText, amount2EditText, amount3EditText, amount4EditText;
    Button loadFromFileButton;
    NachoTextView nachoTextView;
    LatLng gpsLatLng=null;
    LatLng globalLatLng=null;

    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    List<SlotNoInfo> slotNos = new ArrayList<>();
    List<String> slotNoString = new ArrayList<>();
    BasicUtils utils = new BasicUtils();

    ImageView backImageView, settingsImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_parking_position);

        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            showToast("No internet connection. Turn on mobile data or WIFI");
        }
        getPreCurrentLocation();

    }

    private void initComponents() {
        supportMapFragment=(SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);
        client= LocationServices.getFusedLocationProviderClient(AddParkingPosition.this);

        mAuth= FirebaseAuth.getInstance();
        firebaseDatabase= FirebaseDatabase.getInstance();


        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);

        areaNameEditText = findViewById(R.id.area_name_text);
        totalSlotsEditText = findViewById(R.id.total_slots_text);
        amount2EditText = findViewById(R.id.amount_wheeler2_text);
        amount3EditText = findViewById(R.id.amount_wheeler3_text);
        amount4EditText = findViewById(R.id.amount_wheeler4_text);
        addLocationButton = findViewById(R.id.add_location_button);
        nachoTextView = findViewById(R.id.et_tag);
        loadFromFileButton = findViewById(R.id.load_from_file_button);

        slotNoString.add("Slot-01");
        nachoTextView.setText(slotNoString);
        nachoTextView.addChipTerminator('\n', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL);


        // setting the color of icons in the fab
        int color = ContextCompat.getColor(AddParkingPosition.this, R.color.white);
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        // set the color state list as the tint for the FABs
        addLocationButton.setImageTintList(colorStateList);


    }

    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });
        loadFromFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myFileIntent=new Intent(Intent.ACTION_GET_CONTENT);
                myFileIntent.setType("text/plain");
                startActivityForResult(myFileIntent,3000);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        addLocationButton.setOnClickListener(view -> {
            slotNos.clear();
            for (Chip chip : nachoTextView.getAllChips()) {
                CharSequence text = chip.getText();
                slotNos.add(new SlotNoInfo((String) text,false));
            }
            String areaName = areaNameEditText.getText().toString().trim();
            String totalSlots = totalSlotsEditText.getText().toString();
            String amount2 = amount2EditText.getText().toString().trim();
            String amount3 = amount3EditText.getText().toString().trim();
            String amount4 = amount4EditText.getText().toString().trim();

            if (globalLatLng == null) {
                showToast("Please select a location");
            } else if (TextUtils.isEmpty(areaName)) {
                showToast("Please fill in your area name");
                areaNameEditText.requestFocus();
            } else if (TextUtils.isEmpty(totalSlots)) {
                showToast("Please provide the number of slots available");
                totalSlotsEditText.requestFocus();
            } else if (TextUtils.isEmpty(amount2)) {
                showToast("Please provide the amount for a 2-wheeler");
                amount2EditText.requestFocus();
            } else if (TextUtils.isEmpty(amount3)) {
                showToast("Please provide the amount for a 3-wheeler");
                amount3EditText.requestFocus();
            } else if (TextUtils.isEmpty(amount4)) {
                showToast("Please provide the amount for a 4-wheeler");
                amount4EditText.requestFocus();
            } else if (slotNos.size() > Integer.parseInt(totalSlots)) {
                showToast("Number of slot names are more than slot number");
            } else if (slotNos.size() < Integer.parseInt(totalSlots)) {
                showToast("Number of slots is more than Number of slot names");
            } else {
                final ParkingArea parkingArea = new ParkingArea(areaName,globalLatLng.latitude,globalLatLng.longitude,
                        mAuth.getCurrentUser().getUid(),Integer.parseInt(totalSlots),0,
                        Integer.parseInt(amount2),Integer.parseInt(amount3),Integer.parseInt(amount4),slotNos);

                final String key = firebaseDatabase.getReference("ParkingAreas").push().getKey();
                firebaseDatabase.getReference("ParkingAreas")
                        .child(key)
                        .setValue(parkingArea).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    showToast("Parking Area Details Saved");
                                    onBackPressed();
                                } else {
                                    showToast("An error occurred. Failed to save your parking details");
                                }

                            }
                        });


            }


        });

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
        Toast toast = new Toast(AddParkingPosition.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap=googleMap;
        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                globalLatLng=latLng;
                MarkerOptions markerOptions=new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(latLng.latitude+" : "+latLng.longitude);
                gMap.clear();
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,5));
                gMap.addMarker(markerOptions);
            }
        });

    }

    private void getPreCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(AddParkingPosition.this,
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }else{
            ActivityCompat.requestPermissions(AddParkingPosition.this,new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
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
                            gpsLatLng=new LatLng(location.getLatitude(),
                                    location.getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsLatLng,30));
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3000) {
            if (resultCode == RESULT_OK) {
                slotNoString.clear();
                Uri uri = data.getData();
                BufferedReader br;
                FileOutputStream os;
                try {
                    br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
                    os = openFileOutput("newFileName", Context.MODE_PRIVATE);
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        os.write(line.getBytes());
                        slotNoString.add(line);
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        Log.d(String.valueOf(AddParkingPosition.this.getClass()),"Read from file: "+String.valueOf(line));
                    }
                    nachoTextView.setText(slotNoString);
                    br.close();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}