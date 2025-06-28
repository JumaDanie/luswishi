package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log; // Import Log for debugging

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException; // For email already in use
import com.google.firebase.auth.FirebaseAuthWeakPasswordException; // For weak password
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker; // For country code picker
import com.google.i18n.phonenumbers.PhoneNumberUtil; // For phone number validation
import com.google.i18n.phonenumbers.Phonenumber; // For phone number validation


import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity"; // Tag for logging

    // Input fields
    private TextInputLayout firstNameLayout, lastNameLayout, emailLayout, phoneNumberLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText firstName, lastName, email, phoneNumber, password, confirmPassword;
    private CountryCodePicker ccp;

    // Buttons and Progress Bar
    private Button btnSignUp;
    private TextView txtGoToLogin;
    private ProgressBar progressBar;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Phone number utility
    private PhoneNumberUtil phoneNumberUtil;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Layouts
        firstNameLayout = findViewById(R.id.firstNameLayout);
        lastNameLayout = findViewById(R.id.lastNameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        phoneNumberLayout = findViewById(R.id.phoneNumberLayout); // For the TextInputLayout
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        // Initialize TextInputEditTexts
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber); // For the TextInputEditText
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);

        // Initialize CountryCodePicker
        ccp = findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(phoneNumber); // Link CCP to phoneNumber EditText

        // Initialize Buttons and ProgressBar
        btnSignUp = findViewById(R.id.btnSignUp);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);
        progressBar = findViewById(R.id.signupProgressBar);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Phone Number Util
        phoneNumberUtil = PhoneNumberUtil.getInstance();

        // Set up TextWatchers for real-time validation
        firstName.addTextChangedListener(new FieldValidationTextWatcher(firstNameLayout, this::validateFirstName));
        lastName.addTextChangedListener(new FieldValidationTextWatcher(lastNameLayout, this::validateLastName));
        email.addTextChangedListener(new FieldValidationTextWatcher(emailLayout, this::validateEmail));
        phoneNumber.addTextChangedListener(new FieldValidationTextWatcher(phoneNumberLayout, this::validatePhoneNumber));
        password.addTextChangedListener(new FieldValidationTextWatcher(passwordLayout, this::validatePassword));
        confirmPassword.addTextChangedListener(new FieldValidationTextWatcher(confirmPasswordLayout, this::validateConfirmPassword));

        // Set up focus change listeners to validate when leaving a field
        firstName.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) validateFirstName(firstName.getText().toString()); });
        lastName.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) validateLastName(lastName.getText().toString()); });
        email.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) validateEmail(email.getText().toString()); });
        phoneNumber.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) validatePhoneNumber(phoneNumber.getText().toString()); });
        password.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) validatePassword(password.getText().toString()); });
        confirmPassword.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) validateConfirmPassword(confirmPassword.getText().toString()); });


        btnSignUp.setOnClickListener(v -> registerUser());

        txtGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String fName = firstName.getText().toString().trim();
        String lName = lastName.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String fullPhoneNumber = ccp.getFullNumberWithPlus(); // Get full phone number with country code
        String pass = password.getText().toString().trim();
        String confirmPass = confirmPassword.getText().toString().trim();

        // Perform final validation on button click
        boolean isValid = validateFirstName(fName);
        isValid = validateLastName(lName) && isValid;
        isValid = validateEmail(userEmail) && isValid;
        isValid = validatePhoneNumber(phoneNumber.getText().toString().trim()) && isValid; // Pass only the number part for validation
        isValid = validatePassword(pass) && isValid;
        isValid = validateConfirmPassword(confirmPass) && isValid;


        if (!isValid) {
            Toast.makeText(this, "Please correct the errors in the form.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if passwords match (already done in validateConfirmPassword, but good to double check)
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            confirmPasswordLayout.setError("Passwords do not match.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(userEmail, pass).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();

                // Store user data in Firestore
                // Use a Map for flexibility, or your User class with proper serialization
                Map<String, Object> user = new HashMap<>();
                user.put("firstName", fName);
                user.put("lastName", lName);
                user.put("email", userEmail);
                user.put("phoneNumber", fullPhoneNumber); // Store full phone number

                db.collection("users").document(uid).set(user)
                        .addOnCompleteListener(innerTask -> {
                            if (innerTask.isSuccessful()) {
                                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                                // Navigate to LoginActivity or directly to Main if desired
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            } else {
                                Log.e(TAG, "Failed to save user data to Firestore: " + innerTask.getException().getMessage(), innerTask.getException());
                                Toast.makeText(this, "Failed to save user data. Please try again.", Toast.LENGTH_SHORT).show();
                                // Consider deleting the Firebase Auth user if Firestore write fails,
                                // to prevent orphan accounts. This is more complex and might involve
                                // a Cloud Function or careful error handling.
                            }
                        });
            } else {
                Exception exception = task.getException();
                Log.e(TAG, "Sign up failed: " + exception.getMessage(), exception); // Log the full exception

                if (exception instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "The email address is already in use by another account.", Toast.LENGTH_LONG).show();
                    emailLayout.setError("Email already registered.");
                } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                    FirebaseAuthWeakPasswordException weakPasswordException = (FirebaseAuthWeakPasswordException) exception;
                    String reason = weakPasswordException.getReason() != null ? weakPasswordException.getReason() : "Password is too weak.";
                    Toast.makeText(this, "Sign up failed: " + reason, Toast.LENGTH_LONG).show();
                    passwordLayout.setError(reason);
                } else {
                    Toast.makeText(this, "Sign up failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // --- Validation Methods ---

    private boolean validateFirstName(String text) {
        if (text.isEmpty()) {
            firstNameLayout.setError("First name is required.");
            return false;
        } else {
            firstNameLayout.setError(null);
            firstNameLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateLastName(String text) {
        if (text.isEmpty()) {
            lastNameLayout.setError("Last name is required.");
            return false;
        } else {
            lastNameLayout.setError(null);
            lastNameLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateEmail(String text) {
        if (text.isEmpty()) {
            emailLayout.setError("Email is required.");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            emailLayout.setError("Enter a valid email address.");
            return false;
        } else {
            emailLayout.setError(null);
            emailLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePhoneNumber(String text) {
        if (text.isEmpty()) {
            phoneNumberLayout.setError("Phone number is required.");
            return false;
        }

        // Get full international number from CCP for robust validation
        String fullNumber = ccp.getFullNumberWithPlus();
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(fullNumber, ccp.getSelectedCountryNameCode());
            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                phoneNumberLayout.setError("Enter a valid phone number for the selected country.");
                return false;
            }
        } catch (com.google.i18n.phonenumbers.NumberParseException e) {
            Log.e(TAG, "Phone number parsing error: " + e.getMessage());
            phoneNumberLayout.setError("Invalid phone number format.");
            return false;
        }

        phoneNumberLayout.setError(null);
        phoneNumberLayout.setErrorEnabled(false);
        return true;
    }


    private boolean validatePassword(String text) {
        if (text.isEmpty()) {
            passwordLayout.setError("Password is required.");
            return false;
        } else if (text.length() < 6) { // Firebase requires min 6 characters
            passwordLayout.setError("Password must be at least 6 characters.");
            return false;
        } else {
            passwordLayout.setError(null);
            passwordLayout.setErrorEnabled(false);
            // Re-validate confirm password if password changes and it's not empty
            if (!confirmPassword.getText().toString().isEmpty()) {
                validateConfirmPassword(confirmPassword.getText().toString());
            }
            return true;
        }
    }

    private boolean validateConfirmPassword(String text) {
        String originalPassword = password.getText().toString();
        if (text.isEmpty()) {
            confirmPasswordLayout.setError("Confirm password is required.");
            return false;
        } else if (!text.equals(originalPassword)) {
            confirmPasswordLayout.setError("Passwords do not match.");
            return false;
        } else {
            confirmPasswordLayout.setError(null);
            confirmPasswordLayout.setErrorEnabled(false);
            return true;
        }
    }

    // --- Helper TextWatcher Class for Validation ---
    private class FieldValidationTextWatcher implements TextWatcher {
        private TextInputLayout textInputLayout;
        private ValidationFunction validationFunction;

        public FieldValidationTextWatcher(TextInputLayout layout, ValidationFunction function) {
            this.textInputLayout = layout;
            this.validationFunction = function;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No action needed before text changes
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No action needed during text changes (validation happens after)
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Clear error when user starts typing again
            if (textInputLayout.isErrorEnabled()) {
                textInputLayout.setError(null);
                textInputLayout.setErrorEnabled(false);
            }
            // You can optionally call validationFunction here for immediate feedback while typing
            // validationFunction.validate(s.toString()); // Uncomment for more aggressive real-time validation
        }
    }

    // Functional interface for validation methods
    @FunctionalInterface
    private interface ValidationFunction {
        boolean validate(String text);
    }


    // User class (can be nested or in its own file)
    // Note: When using a Map<String, Object> for Firestore, this class is less critical
    // but good for defining the structure. If you used `db.collection("users").document(uid).set(userObject)`
    // then it would need to match fields exactly.
    public static class User {
        public String firstName;
        public String lastName;
        public String email;
        public String phoneNumber; // Added phone number

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String firstName, String lastName, String email, String phoneNumber) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }
    }
}