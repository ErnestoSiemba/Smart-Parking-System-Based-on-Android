package com.glowingsoft.carplaterecognizer.RegisterLogin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
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

import com.glowingsoft.carplaterecognizer.DriverUser.Activities.DriverContainer;
import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.OwnerUser.Activities.OwnerContainer;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    TextView registerLinkText, forgotPasswordText, messageText;
    ImageView settingsImageView, backImageView, toastIcon;

    TextInputEditText emailEditText, passwordEditText;
    Button loginButton;

    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    ProgressDialog progressDialog;
    BasicUtils utils = new BasicUtils();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            showToast("No internet connection. Turn on mobile data or WIFI");
        }


    }

    private void initComponents() {
        Intent in = getIntent();
        String prevEmail = in.getStringExtra("EMAIL");

        registerLinkText = findViewById(R.id.register_link_text);
        settingsImageView = findViewById(R.id.settings_button);
        backImageView = findViewById(R.id.back_button);

        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);

        forgotPasswordText = findViewById(R.id.forgot_password_text);

        backImageView.setVisibility(View.GONE);
        settingsImageView.setVisibility(View.GONE);

        emailEditText.setText(prevEmail);
        emailEditText.setSelection(emailEditText.getText().length());

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

    }
    private void attachListeners() {
//        backImageView.setOnClickListener(view-> {
//            onBackPressed();
//        });

        forgotPasswordText.setOnClickListener(view-> {
            String passEmail=emailEditText.getText().toString();
            Intent intent=new Intent(Login.this, ForgotPassword.class);
            if(!passEmail.isEmpty()){
                intent.putExtra("EMAIL",passEmail);
                startActivity(intent);
            }else{
                startActivity(intent);
            }
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        registerLinkText.setOnClickListener(view-> {
            String passEmail = emailEditText.getText().toString().trim();
            Intent intent=new Intent(Login.this, Register.class);
            if(!passEmail.isEmpty()){
                intent.putExtra("EMAIL",passEmail);
                startActivity(intent);
            }else{
                startActivity(intent);
            }
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        loginButton.setOnClickListener(view-> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                showToast("Please fill in your email");
                emailEditText.requestFocus();
            } else if (TextUtils.isEmpty(password)) {
                showToast("Please fill in your password");
                passwordEditText.requestFocus();
            } else if (utils.isNetworkAvailable(getApplication())) {
                progressDialog = new ProgressDialog(Login.this);
                progressDialog.setMessage("Signing In...");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setCancelable(false);
                progressDialog.show();

                loginUser(email, password);
            } else {
                showToast("No network connection. Turn on mobile data or WIFI");
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    if(!mAuth.getCurrentUser().isEmailVerified()){
                        showToast("Please verify your email");
                        mAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(Login.this, new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                        if (task.isSuccessful()) {
                                            showToast("Verification email sent to " + mAuth.getCurrentUser().getEmail());
                                            mAuth.signOut();
                                            try{ progressDialog.dismiss();
                                            }catch (Exception e){ e.printStackTrace();}
                                        } else {
                                            showToast("Failed to send verification email!");
                                            mAuth.signOut();
                                            try{ progressDialog.dismiss();
                                            }catch (Exception e){ e.printStackTrace();}
                                        }
                                    }
                                });
                    }else{
                        firebaseDatabase.getReference("Users").child(mAuth.getCurrentUser().getUid()).child("isVerified").setValue(1);
                        firebaseDatabase.getReference("Users").child(mAuth.getCurrentUser().getUid()).child("email").setValue(mAuth.getCurrentUser().getEmail());
                        firebaseDatabase.getReference().child("Users").child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    User user = snapshot.getValue(User.class);
//                                    showToast("Login Successful");
                                    Intent intent;
                                    if(user.userType==2) {
                                        // Owner Login
                                        showToast("Owner Login Successful");
                                        startActivity(new Intent(Login.this, OwnerContainer.class));
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                        finish();

                                    } else {
                                        // Driver Login
                                        showToast("Driver Login Successful");
                                        startActivity(new Intent(Login.this, DriverContainer.class));
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                        finish();
                                    }
                                    try{
                                        progressDialog.dismiss();
                                    }catch (Exception e){ e.printStackTrace();}
                                } else {
                                    showToast("User does not exist");
                                }


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                try{ progressDialog.dismiss();
                                }catch (Exception e){ e.printStackTrace();}
                            }
                        });
                    }
                }else{
                    try{
                        progressDialog.dismiss();
                    }catch (Exception e){ e.printStackTrace();}
                    try {
                        throw task.getException(); // if user enters wrong email.
                    }catch (FirebaseAuthInvalidCredentialsException invalid) {
                        showToast("Invalid Credentials!");
                        Log.d(String.valueOf(Login.this.getClass()), "onComplete: Invalid Credentials");
                    } catch (Exception e) {
                        Log.d(String.valueOf(Login.this.getClass()), "onComplete: " + e.getMessage());
                        e.printStackTrace();
                        // TODO: some work
                    }
                }
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
        Toast toast = new Toast(Login.this);
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