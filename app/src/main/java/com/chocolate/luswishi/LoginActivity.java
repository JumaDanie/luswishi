package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button btnLogin;
    TextView txtGoToSignup;
    ProgressBar progressBar; // Add ProgressBar for loading spinner
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToSignup = findViewById(R.id.txtGoToSignup);
        progressBar = findViewById(R.id.progressBar); // Reference to ProgressBar

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());

        txtGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        // Check if email or password is empty
        if (userEmail.isEmpty() || userPass.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar while logging in
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(userEmail, userPass).addOnCompleteListener(task -> {
            // Hide progress bar after the task completes
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                // Login successful
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class)); // Assuming UsersListActivity is the next screen
                finish();
            } else {
                // Handle different error scenarios
                String errorMessage = task.getException().getMessage();
                if (errorMessage.contains("There is no user record corresponding to this identifier")) {
                    Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show();
                } else if (errorMessage.contains("The password is invalid")) {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
