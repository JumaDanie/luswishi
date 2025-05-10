package com.chocolate.luswishi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import com.chocolate.luswishi.model.User;

public class MeFragment extends Fragment {
    private ImageView profileImage;
    private EditText firstName, lastName;
    private Button btnSave, btnLogout;
    private Uri imageUri;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference storageRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fragment_me, container, false);
        profileImage = view.findViewById(R.id.profileImage);
        firstName    = view.findViewById(R.id.firstName);
        lastName     = view.findViewById(R.id.lastName);
        btnSave      = view.findViewById(R.id.btnSave);
        btnLogout    = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        storageRef = FirebaseStorage.getInstance().getReference("profileImages").child(uid);

        // Load existing profile
        userRef.get().addOnSuccessListener(snapshot -> {
            User u = snapshot.getValue(User.class);
            if (u != null) {
                firstName.setText(u.getFirstName());
                lastName.setText(u.getLastName());
                Glide.with(this).load(u.getProfileUrl()).into(profileImage);
            }
        });

        profileImage.setOnClickListener(v -> {
            // pick image
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, 101);
        });

        btnSave.setOnClickListener(v -> saveProfile());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class); // replace with your login activity class
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear back stack
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 101 && res == Activity.RESULT_OK) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }

    private void saveProfile() {
        String f = firstName.getText().toString().trim();
        String l = lastName.getText().toString().trim();
        if (!f.isEmpty() && !l.isEmpty()) {
            // upload image first if changed
            if (imageUri != null) {
                storageRef.putFile(imageUri)
                        .continueWithTask(task -> storageRef.getDownloadUrl())
                        .addOnSuccessListener(uri -> {
                            String url = uri.toString();
                            updateUser(f, l, url);
                        });
            } else {
                // no image change
                userRef.child("firstName").setValue(f);
                userRef.child("lastName").setValue(l);
                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUser(String f, String l, String url) {
        userRef.child("firstName").setValue(f);
        userRef.child("lastName").setValue(l);
        userRef.child("profileUrl").setValue(url)
                .addOnSuccessListener(a ->
                        Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                );
    }
}
