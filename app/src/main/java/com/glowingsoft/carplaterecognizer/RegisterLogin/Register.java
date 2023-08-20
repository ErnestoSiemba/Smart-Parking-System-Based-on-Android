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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.utils.AppConstants;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


public class Register extends AppCompatActivity {

    ImageView backImageView, settingsImageView, toastIcon;
    TextView loginLinkText, messageText;
    Button registerButton;
    TextInputEditText fullNameEditText, emailEditText, ninNumberEditText, phoneNumberEditText;
    TextInputEditText passwordEditText, confirmPasswordEditText;
    RadioGroup userTypes;


    // invoke the BasicUtils class
    BasicUtils utils=new BasicUtils();
    ProgressDialog progressDialog;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth mAuth;

    // declare String fields as global
    String fullName,email, ninNumber, phoneNumber, password, confirmPassword;

    int checkedId, userType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // invoke the initComponents() method
        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            showToast("No internet connection. Turn on mobile data or WIFI");
        }



    }

    // create a method to initialize all the UI controls
    private void initComponents() {
        Intent in = getIntent();
        String prevEmail = in.getStringExtra("EMAIL");
        String prevNumberPlate = in.getStringExtra("NUMBER PLATE");
//        settingsImageView.setVisibility(View.GONE);

        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        loginLinkText = findViewById(R.id.login_link_text);
        fullNameEditText = findViewById(R.id.fullname_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        ninNumberEditText = findViewById(R.id.nin_edit_text);
        phoneNumberEditText = findViewById(R.id.phone_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);

        registerButton = findViewById(R.id.register_button);

        settingsImageView.setVisibility(View.GONE);

        emailEditText.setText(prevEmail);
        emailEditText.setSelection(emailEditText.getText().length());

        // fetching the number plate detected from the MainActivity
//        numberPlateEditText.setText(prevNumberPlate);
//        numberPlateEditText.setSelection(numberPlateEditText.getText().length());

        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();


    }

    // create a method to add listeners to our UI controls
    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

        loginLinkText.setOnClickListener(view-> {
            String passEmail=emailEditText.getText().toString();
            Intent intent=new Intent(Register.this, Login.class);
            if(!passEmail.isEmpty()){
                intent.putExtra("EMAIL",passEmail);
                startActivity(intent);
            }else{
                startActivity(intent);
            }
//            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        registerButton.setOnClickListener(view-> {
            fullName = fullNameEditText.getText().toString().trim();
            email = emailEditText.getText().toString().trim();
            ninNumber = ninNumberEditText.getText().toString().trim();
            phoneNumber = phoneNumberEditText.getText().toString().trim();
            password = passwordEditText.getText().toString().trim();
            confirmPassword = confirmPasswordEditText.getText().toString().trim();
            userTypes=findViewById(R.id.userTypes);

            checkedId=userTypes.getCheckedRadioButtonId();
            userType=findRadioButton(checkedId);

            if (TextUtils.isEmpty(fullName)) {
                showToast("Please fill in your full name");
                fullNameEditText.requestFocus();
            } else if (TextUtils.isEmpty(email)) {
                showToast("Please fill in your email");
                emailEditText.requestFocus();
            } else if (TextUtils.isEmpty(ninNumber)) {
                showToast("Please fill in your NIN Number");
                ninNumberEditText.requestFocus();
            } else if (TextUtils.isEmpty(phoneNumber)) {
                showToast("Please fill in your phone number");
                phoneNumberEditText.requestFocus();
            } else if (TextUtils.isEmpty(password)) {
                showToast("Please fill in your password");
                passwordEditText.requestFocus();
            } else if (TextUtils.isEmpty(confirmPassword)) {
                showToast("Please confirm your password");
                confirmPasswordEditText.requestFocus();
            } else if (password.length() < 6) {
                showToast("Password must be at least 6 characters");
            } else if (ninNumber.length() < 14) {
                showToast("NIN Number cannot be less than 14 characters");
            } else if (!password.equals(confirmPassword)) {
                showToast("Passwords do not match");
            } else if (!utils.isFullNameValid(fullName)) {
                showToast("Full name is Invalid.");
            } else if (!utils.isEmailValid(email)) {
                showToast("Email is Invalid.");
            } else if (!utils.isPhoneNoValid(phoneNumber)) {
                showToast("Phone Number is Invalid.");
            } else if (utils.isNetworkAvailable(getApplication())) {
                progressDialog = new ProgressDialog(Register.this);
                progressDialog.setMessage("Signing Up...");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setCancelable(false);
                progressDialog.show();

                // save the user in the database
                registerUser(fullName, email, ninNumber, phoneNumber, userType);
            }

        });

    }

    private void registerUser(String fullName, String email, String ninNumber, String phoneNumber, int userType) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // save the user additional fields in firebase
                            User user = new User(
                                    fullName,
                                    email,
                                    ninNumber,
                                    phoneNumber,
                                    userType,
                                    0
                            );
                            firebaseDatabase.getReference("Users")
                                    .child(mAuth.getCurrentUser().getUid())
                                    .setValue(user)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                showToast("Registration Successful");
                                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                                firebaseUser.sendEmailVerification()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    showToast("Verification Email Sent to " + firebaseUser.getEmail());
                                                                    mAuth.signOut();
                                                                } else {
                                                                    showToast("Failed to send verification email");
                                                                    mAuth.signOut();
                                                                }
                                                            }
                                                        });
                                                try{
                                                    progressDialog.dismiss();
                                                }catch (Exception e){
                                                    e.printStackTrace();
                                                }
                                                startActivity(new Intent(Register.this, Login.class));
                                                finish();
                                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                            } else {
                                                showToast("Failed to Register User");
                                            }
                                        }
                                    });

                        } else {
                            try{
                                throw task.getException(); // if user enters wrong email.
                            } catch (FirebaseAuthWeakPasswordException weakPassword) {
                                showToast("Too Weak Password!");
                                Log.d(String.valueOf(Register.this.getClass()), "onComplete: weak_password");
                            } catch (FirebaseAuthInvalidCredentialsException malformedEmail) {
                                showToast("Malformed Email!");
                                Log.d(String.valueOf(Register.this.getClass()), "onComplete: malformed_email");
                            } catch (FirebaseAuthUserCollisionException existEmail) {
                                showToast("Email Already Exists");
                                Log.d(String.valueOf(Register.this.getClass()), "onComplete: exist_email");
                            } catch (Exception e) {
                                Log.d(String.valueOf(Register.this.getClass()), "onComplete: " + e.getMessage());
                                e.printStackTrace();
                                // TODO: some work
                            }
                        }
                    }
                });
    }

    private int findRadioButton(int checkedId) {
        int user;
        switch(checkedId){
            case R.id.normalType:
                user = 3;
                break;
            case R.id.ownerType:
                user = 2;
                break;
            default:
                user = 2;
        }
        return user;
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
        Toast toast = new Toast(Register.this);
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