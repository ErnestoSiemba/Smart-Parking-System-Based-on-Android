package com.glowingsoft.carplaterecognizer.ui.Profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class PersonalDetails extends AppCompatActivity {

    BasicUtils utils = new BasicUtils();
    TextInputEditText fullNameEditText, ninNumberEditText, phoneNumberEditText;
    TextInputEditText currentEmailID, newEmailID, currentPassword;

    TextView messageText;
    ImageView backImageView, settingsImageView, toastIcon;
    Button updateDetailsButton, updateEmailButton;
    FirebaseAuth mAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;

    String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            Toast.makeText(PersonalDetails.this, "No Network Available!", Toast.LENGTH_SHORT).show();
        }

    }

    private void initComponents() {


        fullNameEditText = findViewById(R.id.fullname_edit_text);
        ninNumberEditText = findViewById(R.id.nin_edit_text);
        phoneNumberEditText = findViewById(R.id.phone_edit_text);
        updateDetailsButton = findViewById(R.id.update_details_button);


        currentEmailID = findViewById(R.id.current_email_edit_text);
        newEmailID = findViewById(R.id.new_email_edit_text);
        currentPassword = findViewById(R.id.password_edit_text);
        updateEmailButton = findViewById(R.id.update_email_button);

        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);


        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        email = firebaseUser.getEmail();

        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseUser.getUid());

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String ninNumber = snapshot.child("ninNumber").getValue(String.class);
                    String phoneNumber = snapshot.child("phoneNumber").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    fullNameEditText.setText(fullName);
                    ninNumberEditText.setText(ninNumber);
                    phoneNumberEditText.setText(phoneNumber);
                    currentEmailID.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



    }
    private void attachListeners() {
        updateDetailsButton.setOnClickListener(view-> {
            String newFullName = fullNameEditText.getText().toString().trim();
            String newNinNumber = ninNumberEditText.getText().toString().trim();
            String newPhoneNumber = phoneNumberEditText.getText().toString().trim();

            if (TextUtils.isEmpty(newFullName)) {
                showToast("Please fill in your full name");
            } else if (TextUtils.isEmpty(newNinNumber)) {
                showToast("Please fill in your NIN Number");
            } else if (TextUtils.isEmpty(newPhoneNumber)) {
                showToast("Please fill in your phone number");
            } else if (!utils.isFullNameValid(newFullName)) {
                showToast("Full name Invalid");
            } else if (!utils.isPhoneNoValid(newPhoneNumber)) {
                showToast("Phone Number Invalid");
            } else if (newNinNumber.length() < 14) {
                showToast("NIN Number cannot be less than 14 characters");
            } else {
                updateUserDetails(newFullName, newNinNumber, newPhoneNumber);
            }



        });

        updateEmailButton.setOnClickListener(view-> {
            String currentEmail = currentEmailID.getText().toString().trim();
            String newEmail = newEmailID.getText().toString().trim();
            String userPassword = currentPassword.getText().toString().trim();

            // validate email fields
            if (TextUtils.isEmpty(currentEmail)) {
                showToast("Please fill in your current email");
                currentEmailID.requestFocus();
            } else if (TextUtils.isEmpty(newEmail)) {
                showToast("Please fill in your new email");
                newEmailID.requestFocus();
            } else if (TextUtils.isEmpty(userPassword)) {
                showToast("Please fill in your password");
                currentPassword.requestFocus();
            } else if (currentEmail.equals(newEmail)) {
                showToast("Please enter an email different from your current one");
            } else if (!utils.isEmailValid(currentEmail)) {
                showToast("Current Email ID is Invalid");
            } else if (!utils.isEmailValid(newEmail)) {
                showToast("New Email ID is Invalid ");
            } else {
                firebaseUser = mAuth.getCurrentUser();
                AuthCredential authCredential = EmailAuthProvider
                        .getCredential(currentEmail, userPassword);

                firebaseUser.reauthenticate(authCredential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d(String.valueOf(PersonalDetails.this.getClass()), "User re-authenticated.");
                                firebaseUser.updateEmail(newEmail)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    showToast("Email Updated Successfully. Please login again");
                                                    mAuth.signOut();
                                                    startActivity(new Intent(PersonalDetails.this, Login.class));
                                                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                                    finish();
                                                    Log.d(String.valueOf(PersonalDetails.this.getClass()), "User email address updated.");
                                                } else {
                                                    showToast("Failed to update your email. Try again later");
                                                }
                                            }
                                        });

                            }
                        });




            }
        });

        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

    }

    private void updateUserDetails(String newFullName, String newNinNumber, String newPhoneNumber) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseUser.getUid());

        if (firebaseUser != null) {
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String existingFullName = snapshot.child("fullName").getValue(String.class);
                        String existingNinNumber = snapshot.child("ninNumber").getValue(String.class);
                        String existingPhoneNumber = snapshot.child("phoneNumber").getValue(String.class);

                        // check if any changes have been made
                        if (newFullName.equals(existingFullName) && newNinNumber.equals(existingNinNumber) && newPhoneNumber.equals(existingPhoneNumber)) {
                            // show a message to the user
                            showToast("You didn't change anything");
                        } else {
                            // user has made some changes to the data
                            // create a HashMap to hold the updated details
                            HashMap<String, Object> updatedUserDetails = new HashMap<>();
                            updatedUserDetails.put("fullName", newFullName);
                            updatedUserDetails.put("ninNumber", newNinNumber);
                            updatedUserDetails.put("phoneNumber", newPhoneNumber);

                            // update user details
                            databaseReference.updateChildren(updatedUserDetails)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            // show success message to the user
                                            showToast("Profile Updated Successfully");
                                            finish();
                                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // show failure message to the user
                                            showToast("Failed to Update Profile");
                                        }
                                    });


                        }

                    } else {
                        showToast("User does not exist");
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
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
        Toast toast = new Toast(PersonalDetails.this);
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