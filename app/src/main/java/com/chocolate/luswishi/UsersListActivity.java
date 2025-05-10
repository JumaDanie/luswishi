package com.chocolate.luswishi;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class UsersListActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> userNames = new ArrayList<>();
    ArrayList<String> userIds = new ArrayList<>();
    FirebaseAuth mAuth;
    DatabaseReference userRef;
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        listView = findViewById(R.id.listView);
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        fetchUsers();
    }

    private void fetchUsers() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userNames.clear();
                userIds.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    if (!uid.equals(currentUserId)) {
                        String name = userSnap.child("firstName").getValue(String.class) + " " +
                                userSnap.child("lastName").getValue(String.class);
                        userNames.add(name);
                        userIds.add(uid);
                    }
                }
                UsersListAdapter adapter = new UsersListAdapter(UsersListActivity.this, userNames);
                listView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UsersListActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
