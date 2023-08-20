package com.glowingsoft.carplaterecognizer.OwnerUser.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.glowingsoft.carplaterecognizer.OwnerUser.Fragments.OwnerAdd;
import com.glowingsoft.carplaterecognizer.OwnerUser.Fragments.OwnerDashboard;
import com.glowingsoft.carplaterecognizer.OwnerUser.Fragments.OwnerProfile;
import com.glowingsoft.carplaterecognizer.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class OwnerContainer extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private int currentFragmentPosition = -1;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_container);

        bottomNavigationView = findViewById(R.id.owner_bottom_nav);




        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
            int newPosition;
            Fragment fragment;
            switch(menuItem.getItemId()) {
                case R.id.owner_dashboard:
                    newPosition = 0;
                    fragment = new OwnerDashboard();
                    break;


                case R.id.owner_add:
                    newPosition = 1;
                    fragment = new OwnerAdd();
                    break;


                case R.id.owner_profile:
                    newPosition = 2;
                    fragment = new OwnerProfile();
                    break;

                default:
                    return false;

            }
            if (newPosition != currentFragmentPosition) {
                if (newPosition > currentFragmentPosition) {
                    // slide to the right
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.owner_container, fragment)
                            .commit();
                } else {
                    // slide to the left
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.owner_container, fragment)
                            .commit();
                }

                currentFragmentPosition = newPosition;
            }

            return true;
        });

        // set the initial fragment as dashboard
        Fragment initialFragment = new OwnerDashboard();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.owner_container, initialFragment)
                .commit();

        currentFragmentPosition = 0;

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        currentFragmentPosition = getSupportFragmentManager().getBackStackEntryCount() -1;
    }
}