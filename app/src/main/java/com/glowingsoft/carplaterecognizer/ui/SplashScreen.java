package com.glowingsoft.carplaterecognizer.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;

import de.hdodenhof.circleimageview.CircleImageView;

public class SplashScreen extends AppCompatActivity {

    CircleImageView appIconImageView;
    TextView appNameTextView;
    Animation bounceAnimation;

    private static final int SPLASH_DURATION = 2500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);



        initComponents();
    }

    private void initComponents() {
        appIconImageView = findViewById(R.id.logo_image_view);
        bounceAnimation = AnimationUtils.loadAnimation(SplashScreen.this, R.anim.bounce_animation);
        appIconImageView.startAnimation(bounceAnimation);

        // start a new activity after splash screen duration
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // start the activity you want here
                startActivity(new Intent(SplashScreen.this, Login.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        }, SPLASH_DURATION);
    }
}