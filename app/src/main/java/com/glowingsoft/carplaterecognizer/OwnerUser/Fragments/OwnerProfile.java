package com.glowingsoft.carplaterecognizer.OwnerUser.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.DriverUser.Activities.BookingDetails;
import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.ForgotPassword;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.ui.MainActivity;
import com.glowingsoft.carplaterecognizer.ui.Profile.ChangePassword;
import com.glowingsoft.carplaterecognizer.ui.Profile.PersonalDetails;
import com.glowingsoft.carplaterecognizer.utils.AppConstants;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ir.androidexception.andexalertdialog.AndExAlertDialog;
import ir.androidexception.andexalertdialog.AndExAlertDialogListener;


public class OwnerProfile extends Fragment {

    TextView nameText, messageText;
    ImageView toastIcon;
    BasicUtils utils=new BasicUtils();
    LinearLayout personalDetailsLayout, changePasswordLayout, scanNumberPlateLayout, logoutLayout;

    FirebaseAuth mAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_owner_profile, container, false);

        initComponents(root);
        attachListeners();

        if(!utils.isNetworkAvailable(getActivity().getApplication())){
            showToast("No network available. Turn on mobile data or WIFI");
        }

        return root;
    }

    private void initComponents(View root) {

        personalDetailsLayout = root.findViewById(R.id.personal_details_layout);
        changePasswordLayout = root.findViewById(R.id.change_password_layout);
        scanNumberPlateLayout = root.findViewById(R.id.scan_plate_layout);
        logoutLayout = root.findViewById(R.id.logout_layout);


        nameText = root.findViewById(R.id.nameText);

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseUser.getUid());

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    nameText.append(fullName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
    private void attachListeners() {

        logoutLayout.setOnClickListener(view-> {
            new AndExAlertDialog.Builder(requireContext())
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveBtnText("Logout")
                    .setNegativeBtnText("Cancel")
                    .setCancelableOnTouchOutside(true)
                    .OnPositiveClicked(input -> {
                        FirebaseAuth.getInstance().signOut();
                        showToast("Logout Success");
                        startActivity(new Intent(getActivity(), Login.class));
                        getActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        getActivity().finish();

                    })
                    .OnNegativeClicked(new AndExAlertDialogListener() {
                        @Override
                        public void OnClick(String input) {

                        }
                    })
                    .setButtonTextColor(ContextCompat.getColor(getActivity(), R.color.default_color))
                    .setImage(R.drawable.baseline_lock_24, 20)
                    .build();

        });
        personalDetailsLayout.setOnClickListener(view1-> {
            startActivity(new Intent(getActivity(), PersonalDetails.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        if (changePasswordLayout != null) {
            changePasswordLayout.setOnClickListener(view-> {
                startActivity(new Intent(getActivity(), ChangePassword.class));
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        } else {
            showToast("Null pointer on change password");
        }

        scanNumberPlateLayout.setOnClickListener(view-> {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
        Toast toast = new Toast(getActivity());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }



}