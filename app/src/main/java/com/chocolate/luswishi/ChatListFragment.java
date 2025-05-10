package com.chocolate.luswishi;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import android.util.Log;
import com.chocolate.luswishi.model.ChatOverview;

public class ChatListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<ChatOverview> chats = new ArrayList<>();
    private DatabaseReference overviewRef;
    private String currentUserId;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatListAdapter(chats, getContext());
        recyclerView.setAdapter(adapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        overviewRef = FirebaseDatabase.getInstance()
                .getReference("chat_overviews")
                .child(currentUserId);

        loadChats();

        return view;
    }

    private void loadChats() {
        Log.d("ChatListFragment", "Loading chat overviews for user: " + currentUserId);

        overviewRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear existing chat list and reload
                chats.clear();
                Log.d("ChatListFragment", "Found " + snapshot.getChildrenCount() + " chat overviews");

                for (DataSnapshot snap : snapshot.getChildren()) {
                    ChatOverview chat = snap.getValue(ChatOverview.class);
                    if (chat != null) {
                        chats.add(chat);
                        Log.d("ChatListFragment", "Loaded chat with user: " + chat.getUserId() + ", last message: " + chat.getLastMessage());
                    } else {
                        Log.w("ChatListFragment", "Skipped a null chat overview node");
                    }
                }

                Collections.reverse(chats); // Optional: newest chats first
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(0); // Optionally scroll to the top

                Log.d("ChatListFragment", "Adapter updated with " + chats.size() + " items");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatListFragment", "Failed to load chats: " + error.getMessage());
            }
        });

        // Listen for new messages and update the UI
        overviewRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // A new chat message was added
                ChatOverview newChat = dataSnapshot.getValue(ChatOverview.class);
                if (newChat != null) {
                    chats.add(0, newChat); // Add the new chat at the top of the list
                    adapter.notifyItemInserted(0); // Notify the adapter about the new item
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // A chat overview has been updated (e.g., new message in an existing chat)
                String updatedUserId = dataSnapshot.getKey();
                if (updatedUserId != null) {
                    // Find the chat overview in the list and update it
                    for (int i = 0; i < chats.size(); i++) {
                        ChatOverview chat = chats.get(i);
                        if (chat.getUserId().equals(updatedUserId)) {
                            ChatOverview updatedChat = dataSnapshot.getValue(ChatOverview.class);
                            if (updatedChat != null) {
                                chats.set(i, updatedChat); // Update the existing chat overview
                                adapter.notifyItemChanged(i); // Notify the adapter about the updated item
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // A chat was removed (optional: handle chat removal)
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // A chat overview was moved (optional: handle move)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ChatListFragment", "Error while listening for chat updates: " + databaseError.getMessage());
            }
        });
    }
}
