package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chocolate.luswishi.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class DiscoverFragment extends Fragment {
    private RecyclerView recyclerView;
    private DiscoverAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();

    private EditText searchBar;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private ImageView emptyStateImage;

    private String currentUid;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        recyclerView = view.findViewById(R.id.recyclerDiscover);
        searchBar = view.findViewById(R.id.searchBar);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        emptyStateText = view.findViewById(R.id.emptyStateTextView);
        emptyStateImage = view.findViewById(R.id.emptyStateImage);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DiscoverAdapter(filteredUsers, user -> {
            Intent i = new Intent(getContext(), ChatActivity.class);
            i.putExtra("userId", user.getId());
            i.putExtra("userName", user.getFirstName() + " " + user.getLastName());
            i.putExtra("userImage", user.getProfileUrl());
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUsersFromFirestore();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadUsersFromFirestore() {
        showLoading(true);
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (!doc.getId().equals(currentUid)) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setId(doc.getId());
                                allUsers.add(user);
                            }
                        }
                    }
                    showLoading(false);
                    showEmptyState(true); // Show art until user types
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to load users.", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();

        if (query.isEmpty()) {
            adapter.notifyDataSetChanged();
            showEmptyState(true);
            emptyStateText.setText("Look for a friend...");
            return;
        }

        showLoading(true);

        for (User user : allUsers) {
            String q = query.toLowerCase();
            if (
                    user.getFirstName().toLowerCase().contains(q) ||
                            user.getLastName().toLowerCase().contains(q) ||
                            user.getEmail().toLowerCase().contains(q) ||
                            user.getPhoneNumber().toLowerCase().contains(q)
            ) {
                filteredUsers.add(user);
            }
        }

        showLoading(false);
        adapter.notifyDataSetChanged();

        if (filteredUsers.isEmpty()) {
            showEmptyState(true);
            emptyStateText.setText("No results found");
        } else {
            showEmptyState(false);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
