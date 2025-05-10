package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private String receiverId, receiverName, receiverImage;
    private String senderId;

    private TextView userNameTextView;
    private ImageView userImageView;
    private EditText messageInput;
    private Button sendButton;
    private RecyclerView recyclerMessages;

    private MessageAdapter messageAdapter;
    private List<ChatMessage> messages;

    private DatabaseReference chatRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        senderId = mAuth.getCurrentUser().getUid();

        // Get data from intent
        Intent intent = getIntent();
        receiverId = intent.getStringExtra("userId");
        receiverName = intent.getStringExtra("userName");
        receiverImage = intent.getStringExtra("userImage");

        // UI setup
        userNameTextView = findViewById(R.id.userName);
        userImageView = findViewById(R.id.userImage);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        recyclerMessages = findViewById(R.id.recyclerMessages);

        userNameTextView.setText(receiverName);
        Glide.with(this).load(receiverImage).placeholder(R.drawable.ic_user).into(userImageView);

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, senderId);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(messageAdapter);

        chatRef = FirebaseDatabase.getInstance().getReference("chats");

        sendButton.setOnClickListener(v -> sendMessage());

        listenForMessages();
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        long timestamp = System.currentTimeMillis();

        ChatMessage message = new ChatMessage(senderId, receiverId, text, "sent", timestamp);
        String messageId = chatRef.push().getKey();

        if (messageId != null) {
            message.setMessageId(messageId);
            chatRef.child(messageId).setValue(message);
            messageInput.setText("");

            // 1. Update chat overview for sender
            DatabaseReference senderOverviewRef = FirebaseDatabase.getInstance()
                    .getReference("chat_overviews")
                    .child(senderId)
                    .child(receiverId);

            Map<String, Object> senderData = new HashMap<>();
            senderData.put("userId", receiverId);
            senderData.put("userName", receiverName);
            senderData.put("userImage", receiverImage);
            senderData.put("lastMessage", text);
            senderData.put("timestamp", timestamp);
            senderOverviewRef.setValue(senderData);

            // 2. Fetch sender's name and update receiver's chat overview
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String senderFullName = "You";
                    if (snapshot.exists()) {
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        if (firstName != null && lastName != null) {
                            senderFullName = firstName + " " + lastName;
                        }
                    }

                    DatabaseReference receiverOverviewRef = FirebaseDatabase.getInstance()
                            .getReference("chat_overviews")
                            .child(receiverId)
                            .child(senderId);

                    Map<String, Object> receiverData = new HashMap<>();
                    receiverData.put("userId", senderId);
                    receiverData.put("userName", senderFullName);
                    receiverData.put("userImage", ""); // Set sender image here if available
                    receiverData.put("lastMessage", text);
                    receiverData.put("timestamp", timestamp);
                    receiverOverviewRef.setValue(receiverData);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatActivity.this, "Failed to update overview", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    private void listenForMessages() {
        chatRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    ChatMessage msg = snap.getValue(ChatMessage.class);
                    if (msg != null &&
                            ((msg.getSenderId().equals(senderId) && msg.getReceiverId().equals(receiverId)) ||
                                    (msg.getSenderId().equals(receiverId) && msg.getReceiverId().equals(senderId)))) {
                        messages.add(msg);

                        // Update status to "delivered" or "read"
                        if (msg.getReceiverId().equals(senderId) && !msg.getStatus().equals("read")) {
                            snap.getRef().child("status").setValue("read");
                        }
                    }
                }
                messageAdapter.notifyDataSetChanged();
                recyclerMessages.scrollToPosition(messages.size() - 1);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
