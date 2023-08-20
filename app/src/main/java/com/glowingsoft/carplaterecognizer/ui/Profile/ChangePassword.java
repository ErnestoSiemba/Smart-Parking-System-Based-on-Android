package com.glowingsoft.carplaterecognizer.ui.Profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class ChangePassword extends AppCompatActivity {

    TextInputEditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    TextView fullNameText, messageText;
    ImageView backImageView, settingsImageView, toastIcon;
    Button changePasswordButton;

    FirebaseAuth mAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;

    String email;
    BasicUtils utils=new BasicUtils();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        
        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            showToast("No network available. Turn on mobile data or WIFI");
        }
    }

    private void initComponents() {
        currentPasswordEditText = findViewById(R.id.current_password_edit_text);
        newPasswordEditText = findViewById(R.id.new_password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);

        changePasswordButton = findViewById(R.id.change_password_button);

        fullNameText = findViewById(R.id.full_name_text);

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
                    fullNameText.append(fullName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void attachListeners() {
        changePasswordButton.setOnClickListener(view-> {
            String currentPassword = currentPasswordEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // validate the fields
            if (TextUtils.isEmpty(currentPassword)) {
                showToast("Please fill in your current password");
                currentPasswordEditText.requestFocus();
            } else if (TextUtils.isEmpty(newPassword)) {
                showToast("Please fill in your new password");
                newPasswordEditText.requestFocus();
            } else if (TextUtils.isEmpty(confirmPassword)) {
                showToast("Please fill confirm your new password");
                confirmPasswordEditText.requestFocus();
            } else if (currentPassword.equals(newPassword)) {
                showToast("Your current password is the same as new password");
            } else if (newPassword.equals(confirmPassword)) {
                AuthCredential credential = EmailAuthProvider
                        .getCredential(email, currentPassword);

                firebaseUser.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    firebaseUser.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                showToast("Password Changed Successfully \n You will be redirected to login");
                                                finish();
                                                startActivity(new Intent(ChangePassword.this, Login.class));
                                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
                                            } else {
                                                showToast("Password not changed");
                                            }
                                        }
                                    });
                                } else {
                                    showToast("Authentication Failure");
                                }
                            }
                        });

            } else {
                showToast("Passwords do not match");
            }
        });

        backImageView.setOnClickListener(view-> {
            onBackPressed();
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
        Toast toast = new Toast(ChangePassword.this);
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