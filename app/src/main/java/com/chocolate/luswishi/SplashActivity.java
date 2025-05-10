package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        progressBar = findViewById(R.id.splashProgressBar);
        progressBar.setVisibility(View.GONE);

        // Show the logo for 2 seconds
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.VISIBLE);

            // Check if the user is already logged in after the delay
            new Handler().postDelayed(() -> {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    // User is logged in → go to main page
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    // Not logged in → go to login page
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }, 1500); // Progress bar visibility time (1.5 seconds)
        }, 2000); // Logo delay time (2 seconds)
    }
}
