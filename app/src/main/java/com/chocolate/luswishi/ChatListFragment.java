package com.chocolate.luswishi;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.chocolate.luswishi.model.ChatOverview;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {
    private RecyclerView recyclerChats;
    private ChatListAdapter adapter;
    private List<ChatOverview> chats = new ArrayList<>();
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateTextView;
    private ListenerRegistration chatListener;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        dbHelper = new DatabaseHelper(getContext());
        recyclerChats = view.findViewById(R.id.recyclerChats);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);

        recyclerChats.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatListAdapter(chats, getContext());
        recyclerChats.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateUIState(true); // Show progress initially
        loadChatOverviewsFromSQLite(); // Load initial data from SQLite
        setupChatListener(); // Sync with Firestore
    }

    private void loadChatOverviewsFromSQLite() {
        try {
            chats.clear();
            chats.addAll(dbHelper.getChatOverviews());
            Log.d("ChatListFragment", "Loaded " + chats.size() + " chat overviews from SQLite");
        } catch (Exception e) {
            Log.e("ChatListFragment", "Error loading chat overviews from SQLite: " + e.getMessage());
            chats.clear(); // Ensure UI doesn't show stale data
        }
        updateUIState(false);
    }

    private void setupChatListener() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatListener = FirebaseFirestore.getInstance()
                .collection("chat_overviews").document(userId).collection("conversations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Error loading chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        Log.e("ChatListFragment", "Error loading chats", e);
                        updateUIState(false);
                        return;
                    }

                    chats.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatOverview chat = doc.toObject(ChatOverview.class);
                            if (chat != null) {
                                if (chat.getTimestamp() == -1L) {
                                    Log.w("ChatListFragment", "Chat with ID " + doc.getId() + " has null timestamp. Assigning current time.");
                                    chat.setTimestamp(System.currentTimeMillis());
                                }
                                chat.setUserId(doc.getId());
                                chats.add(chat);
                                dbHelper.insertChatOverview(chat); // Sync to SQLite
                                // Fetch and store user profile data
                                fetchUserProfileForChat(doc.getId());
                            } else {
                                Log.w("ChatListFragment", "Skipping invalid chat data from document: " + doc.getId());
                            }
                        }
                    }
                    Log.d("ChatListFragment", "Chats loaded from Firestore: " + chats.size());
                    updateUIState(false);
                });
    }

    private void fetchUserProfileForChat(String userId) {
        DatabaseHelper.User user = dbHelper.getUser(userId);
        if (user != null) {
            Log.d("ChatListFragment", "User profile loaded from SQLite for userId: " + userId + ", profileImage: " + user.profileImage);
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String profileUrl = documentSnapshot.getString("profileUrl"); // Changed to profileUrl
                        String name = firstName != null && !firstName.isEmpty() ?
                                firstName + (lastName != null && !lastName.isEmpty() ? " " + lastName : "") : "Unknown";
                        dbHelper.insertUser(userId, name, profileUrl);
                        Log.d("ChatListFragment", "Stored user profile in SQLite for userId: " + userId + ", profileUrl: " + profileUrl);
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ChatListFragment", "Error fetching user profile for userId: " + userId + ", " + e.getMessage()));
    }

    private void updateUIState(boolean showProgress) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (showProgress) {
                    progressBar.setVisibility(View.VISIBLE);
                    recyclerChats.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    if (chats.isEmpty()) {
                        recyclerChats.setVisibility(View.GONE);
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        emptyStateTextView.setText("Start a new chat in Discover");
                    } else {
                        recyclerChats.setVisibility(View.VISIBLE);
                        emptyStateLayout.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListener != null) {
            chatListener.remove();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}