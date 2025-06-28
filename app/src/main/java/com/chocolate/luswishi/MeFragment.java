package com.chocolate.luswishi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MeFragment extends Fragment {
    private static final String TAG = "MeFragment";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_THEME = "app_theme";

    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView userStatusTextView;
    private LinearLayout optionEditProfile;
    private LinearLayout optionAppAppearance;
    private Button logoutButton;

    private Uri imageUri;
    private FirebaseAuth auth;
    private StorageReference storageReference;

    private ActivityResultLauncher<String> permissionRequestLauncher;
    private ActivityResultLauncher<Intent> imagePickLauncher;
    private ActivityResultLauncher<Intent> imagePickLauncherForDialog;

    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_me, container, false);

        profileImageView = view.findViewById(R.id.profileImageView);
        userNameTextView = view.findViewById(R.id.userNameTextView);
        userStatusTextView = view.findViewById(R.id.userStatusTextView);
        optionEditProfile = view.findViewById(R.id.optionEditProfile);
        optionAppAppearance = view.findViewById(R.id.optionAppAppearance);
        logoutButton = view.findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(requireContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
            return view;
        }

        storageReference = FirebaseStorage.getInstance().getReference("profileImages").child(userId);

        permissionRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                pickImage();
            } else {
                Toast.makeText(requireContext(), "Permission denied to access media", Toast.LENGTH_SHORT).show();
            }
        });

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                imageUri = result.getData().getData();
                if (imageUri != null) {
                    Glide.with(requireContext())
                            .load(imageUri)
                            .apply(new RequestOptions().transform(new CircleCrop()))
                            .into(profileImageView);
                    saveProfileImage(imageUri);
                }
            }
        });

        imagePickLauncherForDialog = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                imageUri = result.getData().getData();
            }
        });

        loadUserProfile();

        profileImageView.setOnClickListener(v -> requestImagePermission());

        optionEditProfile.setOnClickListener(v -> showEditProfileDialog());

        optionAppAppearance.setOnClickListener(v -> showAppAppearanceDialog());

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
        });

        return view;
    }

    private void requestImagePermission() {
        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;
        permissionRequestLauncher.launch(permission);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickLauncher.launch(intent);
    }

    private void pickImageForDialog() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickLauncherForDialog.launch(intent);
    }

    private void loadUserProfile() {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String firstName = snapshot.getString("firstName");
                        String lastName = snapshot.getString("lastName");
                        String profileUrl = snapshot.getString("profileUrl");

                        userNameTextView.setText(String.format("%s %s", firstName, lastName));
                        userStatusTextView.setText("Hey there! I am using Luswishi.");

                        Glide.with(requireContext())
                                .load(profileUrl != null && !profileUrl.isEmpty() ? profileUrl : R.drawable.ic_user)
                                .apply(new RequestOptions().transform(new CircleCrop()))
                                .into(profileImageView);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load profile", e);
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileImage(Uri uri) {
        if (uri == null) return;

        storageReference.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .update("profileUrl", downloadUri.toString())
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show());
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show());
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText editFirstName = dialogView.findViewById(R.id.editFirstName);
        EditText editLastName = dialogView.findViewById(R.id.editLastName);
        ImageView dialogImageView = dialogView.findViewById(R.id.dialogProfileImage);

        String[] nameParts = userNameTextView.getText().toString().split(" ");
        if (nameParts.length >= 2) {
            editFirstName.setText(nameParts[0]);
            editLastName.setText(nameParts[1]);
        }

        Glide.with(requireContext())
                .load(profileImageView.getDrawable())
                .apply(new RequestOptions().transform(new CircleCrop()))
                .into(dialogImageView);

        dialogImageView.setOnClickListener(v -> pickImageForDialog());

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String firstName = editFirstName.getText().toString().trim();
                    String lastName = editLastName.getText().toString().trim();

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .update("firstName", firstName, "lastName", lastName)
                            .addOnSuccessListener(unused -> {
                                if (imageUri != null) {
                                    saveProfileImage(imageUri);
                                }
                                loadUserProfile();
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void showAppAppearanceDialog() {
        final String[] themes = {"System Default", "Light", "Dark"};
        int checkedItem = getSelectedThemeIndex();

        new AlertDialog.Builder(requireContext())
                .setTitle("Choose App Appearance")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    setAppTheme(which);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setAppTheme(int themeIndex) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME, themeIndex);
        editor.apply();

        switch (themeIndex) {
            case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
        }
    }

    private int getSelectedThemeIndex() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, 2); // Default to Dark
    }
}
