package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable; // Import for TextWatcher
import android.text.TextWatcher; // Import for TextWatcher
import android.util.Log;
import android.util.Patterns; // Import for email pattern
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // Import for TextInputLayout
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {

    // Declare TextInputLayouts along with TextInputEditTexts
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText emailEditText, passwordEditText; // Renamed to avoid ID conflict
    private Button btnLogin;
    private TextView txtGoToSignup;
    private ProgressBar progressBar; // This will now refer to the one inside the button container
    private FirebaseAuth mAuth;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize TextInputLayouts
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        // Initialize TextInputEditTexts (using new IDs from XML)
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        btnLogin = findViewById(R.id.btnLogin);
        txtGoToSignup = findViewById(R.id.txtGoToSignup);
        progressBar = findViewById(R.id.progressBar); // Reference the progress bar inside the FrameLayout

        mAuth = FirebaseAuth.getInstance();

        // Set up TextWatchers for real-time validation feedback
        emailEditText.addTextChangedListener(new FieldValidationTextWatcher(emailInputLayout, this::validateEmail));
        passwordEditText.addTextChangedListener(new FieldValidationTextWatcher(passwordInputLayout, this::validatePassword));

        // Set up OnFocusChangeListeners to validate when a field loses focus
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateEmail(emailEditText.getText().toString().trim());
        });
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validatePassword(passwordEditText.getText().toString().trim());
        });


        btnLogin.setOnClickListener(v -> loginUser());

        txtGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            // No finish() here, so user can go back to login if they change their mind
        });
    }

    private void loginUser() {
        String userEmail = emailEditText.getText().toString().trim();
        String userPass = passwordEditText.getText().toString().trim();

        // Perform validation before attempting login
        boolean isValid = validateEmail(userEmail);
        isValid = validatePassword(userPass) && isValid; // Ensure previous validation result is carried over

        if (!isValid) {
            Toast.makeText(this, "Please correct the errors in the form.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false); // Disable button to prevent multiple clicks

        mAuth.signInWithEmailAndPassword(userEmail, userPass).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true); // Re-enable button

            if (task.isSuccessful()) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish(); // Finish LoginActivity after successful login
            } else {
                Exception e = task.getException();
                Log.e(TAG, "Login failed with exception: " + e.getClass().getSimpleName() + " - Message: " + e.getMessage());

                if (e instanceof FirebaseAuthInvalidUserException) {
                    String errorCode = ((FirebaseAuthInvalidUserException) e).getErrorCode();
                    Log.e(TAG, "FirebaseAuthInvalidUserException - Error Code: " + errorCode);

                    if ("ERROR_USER_NOT_FOUND".equalsIgnoreCase(errorCode)) {
                        Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show();
                        emailInputLayout.setError("No account found with this email."); // Set error on email field
                    } else if ("ERROR_USER_DISABLED".equalsIgnoreCase(errorCode)) {
                        Toast.makeText(this, "This account has been disabled. Please contact support.", Toast.LENGTH_LONG).show();
                        emailInputLayout.setError("Account disabled."); // Set error on email field
                    } else {
                        Toast.makeText(this, "Login failed: User issue. Please try again.", Toast.LENGTH_SHORT).show();
                    }

                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    String errorCode = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
                    Log.e(TAG, "FirebaseAuthInvalidCredentialsException - Error Code: " + errorCode);

                    if ("ERROR_INVALID_EMAIL".equalsIgnoreCase(errorCode)) {
                        Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                        emailInputLayout.setError("Invalid email format."); // Set error on email field
                    } else if ("ERROR_WRONG_PASSWORD".equalsIgnoreCase(errorCode) || "ERROR_INVALID_CREDENTIAL".equalsIgnoreCase(errorCode)) {
                        // This handles both wrong password and unregistered email due to enumeration protection
                        Toast.makeText(this, "Incorrect email or password.", Toast.LENGTH_SHORT).show();
                        // Set error on both fields, or just password for a general "incorrect credentials" feel
                        passwordInputLayout.setError("Incorrect password.");
                    } else {
                        Toast.makeText(this, "Login failed: Incorrect credentials. Please try again.", Toast.LENGTH_SHORT).show();
                    }

                } else if (e instanceof FirebaseAuthException) {
                    String errorCode = ((FirebaseAuthException) e).getErrorCode();
                    Log.e(TAG, "FirebaseAuthException - Error Code: " + errorCode);

                    if ("ERROR_OPERATION_NOT_ALLOWED".equalsIgnoreCase(errorCode)) {
                        Toast.makeText(this, "Email/password login is not enabled. Contact support.", Toast.LENGTH_LONG).show();
                    } else if ("ERROR_TOO_MANY_REQUESTS".equalsIgnoreCase(errorCode)) {
                        Toast.makeText(this, "Too many login attempts. Try again later.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Login failed: Firebase Auth issue. Please try again.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Log.e(TAG, "Unexpected login error type or message: ", e);
                    Toast.makeText(this, "Login failed. An unexpected error occurred.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // --- Validation Methods ---

    private boolean validateEmail(String text) {
        if (text.isEmpty()) {
            emailInputLayout.setError("Email is required.");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            emailInputLayout.setError("Enter a valid email address.");
            return false;
        } else {
            emailInputLayout.setError(null); // Clear error
            emailInputLayout.setErrorEnabled(false); // Disable error state
            return true;
        }
    }

    private boolean validatePassword(String text) {
        if (text.isEmpty()) {
            passwordInputLayout.setError("Password is required.");
            return false;
        } else {
            passwordInputLayout.setError(null); // Clear error
            passwordInputLayout.setErrorEnabled(false); // Disable error state
            return true;
        }
    }


    // --- Helper TextWatcher Class for Validation ---
    // (This class can be reused from SignUpActivity, or put into a common utility file)
    private class FieldValidationTextWatcher implements TextWatcher {
        private TextInputLayout textInputLayout;
        private ValidationFunction validationFunction;

        public FieldValidationTextWatcher(TextInputLayout layout, ValidationFunction function) {
            this.textInputLayout = layout;
            this.validationFunction = function;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No action needed
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No action needed
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Clear error when user starts typing again
            if (textInputLayout.isErrorEnabled()) {
                textInputLayout.setError(null);
                textInputLayout.setErrorEnabled(false);
            }
            // Optional: call validationFunction here for more aggressive real-time validation while typing
            // validationFunction.validate(s.toString());
        }
    }

    // Functional interface for validation methods
    @FunctionalInterface
    private interface ValidationFunction {
        boolean validate(String text);
    }
}