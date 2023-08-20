package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.ClosestDistance;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.adapters.CloseLocationAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NearByArea extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 100;
    BasicUtils utils = new BasicUtils();
    FusedLocationProviderClient client;

    private RecyclerView recyclerView;
    private CloseLocationAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    TextView emptyTextView, messageText;

    List<ClosestDistance> closestDistanceList=new ArrayList<ClosestDistance>();
    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    ImageView backImageView, settingsImageView, toastIcon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_by_area);

        initComponents();
        attachListeners();
        getPreCurrentLocation();
        if (!utils.isNetworkAvailable(getApplication())) {
            showToast("No network available. Turn on mobile data of WIFI");
        }
    }

    private void initComponents() {

        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);

        emptyTextView = findViewById(R.id.empty_view);
        recyclerView = findViewById(R.id.closest_location_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(NearByArea.this);
        recyclerView.setLayoutManager(layoutManager);

        client= LocationServices.getFusedLocationProviderClient(NearByArea.this);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
        }
        return super.onOptionsItemSelected(item);
    }

    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

    }

    private void getPreCurrentLocation() {
        if(ActivityCompat.checkSelfPermission(NearByArea.this,
                Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }else{
            ActivityCompat.requestPermissions(NearByArea.this,new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(NearByArea.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(NearByArea.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(NearByArea.this,new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        Task<Location> task= client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if(location!=null){
                    firebaseDatabase.getReference().child("ParkingAreas")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                        ParkingArea parkingData = dataSnapshot.getValue(ParkingArea.class);
                                        ClosestDistance closestDistance=new ClosestDistance(
                                                distance(location.getLatitude(), location.getLongitude(), parkingData.latitude, parkingData.longitude, "K"),
                                                parkingData,
                                                dataSnapshot.getKey());
                                        closestDistanceList.add(closestDistance);
                                    }
                                    mAdapter = new CloseLocationAdapter(closestDistanceList);
                                    mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                                        @Override public void onChanged() {
                                            super.onChanged();
                                            if(mAdapter.getItemCount()>0){
                                                recyclerView.setVisibility(View.VISIBLE);
                                                emptyTextView.setVisibility(View.GONE);
                                                Log.i("FilterSearch","not");
                                            }else{
                                                recyclerView.setVisibility(View.GONE);
                                                emptyTextView.setVisibility(View.VISIBLE);
                                                Log.i("FilterSearch","empty");
                                            }
                                            // access adapter's dataset size here or in that method
                                        }
                                    });
                                    recyclerView.setAdapter(mAdapter);
                                    if(closestDistanceList.isEmpty()){
                                        recyclerView.setVisibility(View.GONE);
                                        emptyTextView.setVisibility(View.VISIBLE);
                                    }else{
                                        recyclerView.setVisibility(View.VISIBLE);
                                        emptyTextView.setVisibility(View.GONE);
                                    }
                                    Log.d(String.valueOf(NearByArea.this.getClass()),"recyclerview pass: "+String.valueOf(closestDistanceList));
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
        });

    }
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nearby_menu,menu);
        MenuItem item=menu.findItem(R.id.action_search);
        SearchView searchView=(SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(mAdapter!=null){
                    mAdapter.getFilter().filter(newText);
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
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
        Toast toast = new Toast(NearByArea.this);
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
    }
}