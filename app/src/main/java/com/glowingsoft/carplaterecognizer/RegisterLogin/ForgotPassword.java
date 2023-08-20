package com.glowingsoft.carplaterecognizer.RegisterLogin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
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
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    ImageView backImageView, settingsImageView, toastIcon;
    TextView messageText;
    TextInputEditText emailEditText;
    Button resetPasswordButton;
    ProgressDialog progressDialog;

    BasicUtils utils = new BasicUtils();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initComponents();
        attachListeners();
        if(!utils.isNetworkAvailable(getApplication())){
            showToast("No network connection. Turn on mobile data or WIFI");
        }

        settingsImageView.setVisibility(View.GONE);

    }



    private void initComponents() {
        emailEditText = findViewById(R.id.email_edit_text);
        resetPasswordButton = findViewById(R.id.reset_password_button);
        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);

        Intent in = getIntent();
        String prevEmail = in.getStringExtra("EMAIL");
        emailEditText.setText(prevEmail);
        emailEditText.setSelection(emailEditText.getText().length());
    }

    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

        resetPasswordButton.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim();

            if(TextUtils.isEmpty(email)){
                showToast("Please fill in your email");
                emailEditText.requestFocus();
            }else{
                if (utils.isNetworkAvailable(getApplication())) {
                    if (utils.isEmailValid(email)) {
                        progressDialog = new ProgressDialog(ForgotPassword.this);
                        progressDialog.setMessage("Sending Email...");
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        resetPasswordMail(email);
                    } else {
                        showToast("The email you provided is invalid!");
                    }

                } else {
                    showToast("No network connection. Turn on mobile data or WIFI");
                }


            }
        });
    }

    private void resetPasswordMail(String email) {
        if(utils.isNetworkAvailable(getApplication())){
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                showToast("Password Reset Email has been sent to ".concat(email));
                                finish();
                            }else{
                                progressDialog.dismiss();
                                showToast("Failure. Password Reset Email not send");
                            }
                        }
                    });
        }else{
            showToast("No network connection. Turn on mobile data or WIFI");
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
        Toast toast = new Toast(ForgotPassword.this);
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
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}