package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.glowingsoft.carplaterecognizer.DriverUser.Fragments.DriverDashboard;
import com.glowingsoft.carplaterecognizer.DriverUser.Fragments.DriverScanPlate;
import com.glowingsoft.carplaterecognizer.DriverUser.Fragments.DriverProfile;
import com.glowingsoft.carplaterecognizer.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DriverContainer extends AppCompatActivity {


    BottomNavigationView bottomNavigationView;
    private int currentFragmentPosition = -1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_container);

        bottomNavigationView = findViewById(R.id.driver_bottom_nav);


        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
            int newPosition;
            Fragment fragment;
            switch(menuItem.getItemId()) {
                case R.id.dashboard:
                    newPosition = 0;
                    fragment = new DriverDashboard();
                    break;


                case R.id.scan:
                    newPosition = 1;
                    fragment = new DriverScanPlate();
                    break;


                case R.id.profile:
                    newPosition = 2;
                    fragment = new DriverProfile();
                    break;

                default:
                    return false;

            }
            if (newPosition != currentFragmentPosition) {
                if (newPosition > currentFragmentPosition) {
                    // slide to the right
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.container, fragment)
                            .commit();
                } else {
                    // slide to the left
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.container, fragment)
                            .commit();
                }

                currentFragmentPosition = newPosition;
            }

            return true;
        });

        // set the initial fragment as dashboard
        Fragment initialFragment = new DriverDashboard();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, initialFragment)
                .commit();

        currentFragmentPosition = 0;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        currentFragmentPosition = getSupportFragmentManager().getBackStackEntryCount() -1;
    }
}