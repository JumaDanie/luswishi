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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    EditText firstName, lastName, email, password;
    Button btnSignUp;
    TextView txtGoToLogin;
    ProgressBar progressBar; // Add ProgressBar for loading spinner
    FirebaseAuth mAuth;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);
        progressBar = findViewById(R.id.progressBar); // Reference to ProgressBar

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        btnSignUp.setOnClickListener(v -> registerUser());

        txtGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Optional: close the sign-up screen so user can't return with back button
        });
    }

    private void registerUser() {
        String fName = firstName.getText().toString();
        String lName = lastName.getText().toString();
        String userEmail = email.getText().toString();
        String pass = password.getText().toString();

        // Input validation
        if (fName.isEmpty() || lName.isEmpty() || userEmail.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(userEmail, pass).addOnCompleteListener(task -> {
            // Hide progress bar once the task is complete
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                User user = new User(fName, lName, userEmail);
                userRef.child(uid).setValue(user).addOnCompleteListener(innerTask -> {
                    if (innerTask.isSuccessful()) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish(); // Close SignUpActivity
                    } else {
                        Toast.makeText(this, "Failed to save user data: " + innerTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class User {
        public String firstName, lastName, email;

        public User() {}

        public User(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }
    }
}
