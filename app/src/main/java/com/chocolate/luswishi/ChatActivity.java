package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.database.*;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import com.chocolate.luswishi.model.ChatOverview;
import com.chocolate.luswishi.model.ChatMessage;
import com.chocolate.luswishi.model.DateSeparator;
import com.chocolate.luswishi.model.MessageItem;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private String receiverId;
    private String receiverName;
    private String senderId;
    private String senderName;

    private TextView userNameTextView;
    private ImageView userImageView;
    private EditText messageInput;
    private ImageButton sendButton;
    private RecyclerView recyclerMessages;

    private MessageAdapter messageAdapter;
    private List<MessageItem> messages;

    private FirebaseFirestore db;
    private DatabaseReference statusRef;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;
    private ListenerRegistration messageListener;
    private ChildEventListener statusListener;
    private Set<String> messageIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        statusRef = FirebaseDatabase.getInstance().getReference("message_status");
        dbHelper = new DatabaseHelper(this);
        senderId = mAuth.getCurrentUser().getUid();
        messageIds = new HashSet<>();

        // Get data from intent
        Intent intent = getIntent();
        receiverId = intent.getStringExtra("userId");
        receiverName = intent.getStringExtra("userName");

        // UI setup
        userNameTextView = findViewById(R.id.userName);
        userImageView = findViewById(R.id.userImage);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        recyclerMessages = findViewById(R.id.recyclerMessages);

        userNameTextView.setText(receiverName);

        // Load receiver profile image from SQLite or Firestore
        loadReceiverImageForHeader();
        // Fetch sender's name and store in SQLite
        fetchSenderName();

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages, senderId);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(messageAdapter);

        // Load initial messages from SQLite
        loadMessagesFromSQLite();

        sendButton.setOnClickListener(v -> sendMessage());

        listenForMessages();
        resetUnreadCount();
    }

    private void loadReceiverImageForHeader() {
        // Try loading from SQLite first
        DatabaseHelper.User user = dbHelper.getUser(receiverId);
        if (user != null && user.profileImage != null && !user.profileImage.isEmpty()) {
            Glide.with(ChatActivity.this)
                    .load(user.profileImage)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .circleCrop()
                    .into(userImageView);
            userNameTextView.setText(user.userName);
            receiverName = user.userName;
            Log.d("ChatActivity", "Loaded receiver image from SQLite for userId: " + receiverId + ", profileUrl: " + user.profileImage);
            return;
        }

        // Fallback to Firestore
        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String imageUrl = documentSnapshot.getString("profileUrl");
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String name = firstName != null && !firstName.isEmpty() ?
                            firstName + (lastName != null && !lastName.isEmpty() ? " " + lastName : "") : receiverName;

                    // Store in SQLite
                    dbHelper.insertUser(receiverId, name, imageUrl);

                    // Load image
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ChatActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .circleCrop()
                                .into(userImageView);
                    } else {
                        Glide.with(ChatActivity.this)
                                .load(R.drawable.ic_user)
                                .circleCrop()
                                .into(userImageView);
                    }
                    userNameTextView.setText(name);
                    receiverName = name;
                    Log.d("ChatActivity", "Loaded receiver image from Firestore for userId: " + receiverId + ", profileUrl: " + imageUrl);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error loading receiver image: " + e.getMessage());
                    Glide.with(ChatActivity.this)
                            .load(R.drawable.ic_user)
                            .circleCrop()
                            .into(userImageView);
                    userNameTextView.setText(receiverName);
                });
    }

    private void fetchSenderName() {
        // Try loading from SQLite first
        DatabaseHelper.User user = dbHelper.getUser(senderId);
        if (user != null) {
            senderName = user.userName;
            Log.d("ChatActivity", "Loaded sender name from SQLite for userId: " + senderId);
            return;
        }

        // Fallback to Firestore
        db.collection("users").document(senderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String profileUrl = documentSnapshot.getString("profileUrl");
                        if (firstName != null && !firstName.isEmpty()) {
                            senderName = firstName + (lastName != null && !lastName.isEmpty() ? " " + lastName : "");
                        } else {
                            senderName = "You";
                        }
                        // Store in SQLite
                        dbHelper.insertUser(senderId, senderName, profileUrl);
                    } else {
                        senderName = "You";
                    }
                    Log.d("ChatActivity", "Loaded sender name from Firestore for userId: " + senderId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error fetching sender name: " + e.getMessage());
                    senderName = "You";
                });
    }

    private void loadMessagesFromSQLite() {
        List<ChatMessage> rawMessages = dbHelper.getMessages(senderId, receiverId);
        updateMessagesWithDateSeparators(rawMessages);
        messageAdapter.notifyDataSetChanged();
        if (messages.size() > 0) {
            recyclerMessages.scrollToPosition(messages.size() - 1);
        }
        Log.d("ChatActivity", "Loaded " + rawMessages.size() + " messages from SQLite");
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        long timestamp = System.currentTimeMillis();
        String messageId = db.collection("chats").document().getId();
        ChatMessage message = new ChatMessage(senderId, receiverId, text, "pending", timestamp);
        message.setMessageId(messageId);

        // Optimistic UI update and SQLite insert
        messages.add(message);
        messageIds.add(messageId);
        dbHelper.insertMessage(message);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        recyclerMessages.scrollToPosition(messages.size() - 1);
        messageInput.setText("");

        // Use a WriteBatch for atomic updates
        WriteBatch batch = db.batch();

        // 1. Store message in Firestore 'chats' collection
        DocumentReference chatRef = db.collection("chats").document(messageId);
        batch.set(chatRef, message);

        // 2. Update sender's chat overview in Firestore and SQLite
        DocumentReference senderOverviewRef = db.collection("chat_overviews")
                .document(senderId)
                .collection("conversations")
                .document(receiverId);

        ChatOverview senderChatOverview = new ChatOverview(
                receiverId,
                receiverName,
                text,
                timestamp,
                0
        );
        batch.set(senderOverviewRef, senderChatOverview);
        dbHelper.insertChatOverview(senderChatOverview);

        // 3. Update receiver's chat overview in Firestore
        DocumentReference receiverOverviewRef = db.collection("chat_overviews")
                .document(receiverId)
                .collection("conversations")
                .document(senderId);

        receiverOverviewRef.get().addOnSuccessListener(receiverSnapshot -> {
            int unreadCount = 0;
            if (receiverSnapshot.exists()) {
                Long count = receiverSnapshot.getLong("unreadCount");
                if (count != null) {
                    unreadCount = count.intValue();
                }
            }

            ChatOverview receiverChatOverview = new ChatOverview(
                    senderId,
                    senderName,
                    text,
                    timestamp,
                    unreadCount + 1
            );
            batch.set(receiverOverviewRef, receiverChatOverview);

            // Commit the batch writes
            batch.commit()
                    .addOnCompleteListener(batchTask -> {
                        if (batchTask.isSuccessful()) {
                            // Update status in Realtime Database
                            statusRef.child(messageId).setValue("sent").addOnCompleteListener(statusTask -> {
                                if (statusTask.isSuccessful()) {
                                    Log.d("ChatActivity", "Message " + messageId + " status set to sent in Realtime DB");
                                } else {
                                    Log.e("ChatActivity", "Failed to update status in Realtime DB: " + statusTask.getException().getMessage());
                                    Toast.makeText(ChatActivity.this, "Failed to update message status.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Log.e("ChatActivity", "Failed to send message or update overviews: " + batchTask.getException().getMessage());
                            Toast.makeText(ChatActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                            // Rollback SQLite and UI
                            dbHelper.deleteMessage(messageId);
                            messageIds.remove(messageId);
                            for (int i = messages.size() - 1; i >= 0; i--) {
                                if (messages.get(i) instanceof ChatMessage &&
                                        ((ChatMessage) messages.get(i)).getMessageId().equals(messageId)) {
                                    messages.remove(i);
                                    messageAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e("ChatActivity", "Error getting receiver's unread count: " + e.getMessage());
            Toast.makeText(ChatActivity.this, "Error preparing message.", Toast.LENGTH_SHORT).show();
            // Rollback SQLite and UI
            dbHelper.deleteMessage(messageId);
            messageIds.remove(messageId);
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i) instanceof ChatMessage &&
                        ((ChatMessage) messages.get(i)).getMessageId().equals(messageId)) {
                    messages.remove(i);
                    messageAdapter.notifyItemRemoved(i);
                    break;
                }
            }
        });
    }

    private void resetUnreadCount() {
        db.collection("chat_overviews").document(senderId).collection("conversations")
                .document(receiverId).update("unreadCount", 0)
                .addOnSuccessListener(aVoid -> {
                    // Update SQLite
                    ChatOverview overview = dbHelper.getChatOverviews().stream()
                            .filter(o -> o.getUserId().equals(receiverId))
                            .findFirst()
                            .orElse(null);
                    if (overview != null) {
                        overview.setUnreadCount(0);
                        dbHelper.insertChatOverview(overview);
                    }
                })
                .addOnFailureListener(e -> Log.e("ChatActivity", "Error resetting unread count: " + e.getMessage()));
    }

    private void listenForMessages() {
        Query query1 = db.collection("chats")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", receiverId)
                .orderBy("timestamp");

        Query query2 = db.collection("chats")
                .whereEqualTo("senderId", receiverId)
                .whereEqualTo("receiverId", senderId)
                .orderBy("timestamp");

        List<ChatMessage> messagesFromSender = new ArrayList<>();
        List<ChatMessage> messagesFromReceiver = new ArrayList<>();
        int[] updateCounter = new int[1];

        ListenerRegistration listener1 = query1.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("ChatActivity", "Query1 error: " + e.getMessage());
                Toast.makeText(ChatActivity.this, "Error loading messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots == null) {
                Log.w("ChatActivity", "Query1 snapshots null");
                return;
            }

            messagesFromSender.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                ChatMessage msg = doc.toObject(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(doc.getId());
                    messagesFromSender.add(msg);
                    messageIds.add(doc.getId());
                    dbHelper.insertMessage(msg);
                }
            }
            updateCounter[0]++;
            Log.d("ChatActivity", "Query1 triggered, counter: " + updateCounter[0] + ", Sender messages: " + messagesFromSender.size());
            if (updateCounter[0] >= 2) {
                updateMessagesAfterFetch(messagesFromSender, messagesFromReceiver);
                updateCounter[0] = 0;
            }
        });

        ListenerRegistration listener2 = query2.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("ChatActivity", "Query2 error: " + e.getMessage());
                Toast.makeText(ChatActivity.this, "Error loading messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots == null) {
                Log.w("ChatActivity", "Query2 snapshots null");
                return;
            }

            messagesFromReceiver.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                ChatMessage msg = doc.toObject(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(doc.getId());
                    messagesFromReceiver.add(msg);
                    messageIds.add(doc.getId());
                    dbHelper.insertMessage(msg);
                }
            }
            updateCounter[0]++;
            Log.d("ChatActivity", "Query2 triggered, counter: " + updateCounter[0] + ", Receiver messages: " + messagesFromReceiver.size());
            if (updateCounter[0] >= 2) {
                updateMessagesAfterFetch(messagesFromSender, messagesFromReceiver);
                updateCounter[0] = 0;
            }
        });

        statusListener = statusRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                handleStatusUpdate(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                handleStatusUpdate(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Status listener cancelled: " + error.getMessage());
            }
        });

        messageListener = new ListenerRegistration() {
            @Override
            public void remove() {
                listener1.remove();
                listener2.remove();
            }
        };
    }

    private void handleStatusUpdate(DataSnapshot snapshot) {
        String messageId = snapshot.getKey();
        String status = snapshot.getValue(String.class);
        if (status == null) {
            Log.w("ChatActivity", "Null status for messageId: " + messageId);
            return;
        }

        if (messageIds.contains(messageId)) {
            dbHelper.updateMessageStatus(messageId, status);
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i) instanceof ChatMessage &&
                        ((ChatMessage) messages.get(i)).getMessageId().equals(messageId)) {
                    ((ChatMessage) messages.get(i)).setStatus(status);
                    messageAdapter.notifyItemChanged(i);
                    Log.d("ChatActivity", "Status updated for message " + messageId + ": " + status);
                    break;
                }
            }
        } else {
            Log.d("ChatActivity", "Status update ignored for messageId " + messageId + " (not in this chat)");
        }
    }

    private void updateMessagesAfterFetch(List<ChatMessage> messagesFromSender, List<ChatMessage> messagesFromReceiver) {
        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.addAll(messagesFromSender);
        allMessages.addAll(messagesFromReceiver);
        allMessages.sort(Comparator.comparingLong(ChatMessage::getTimestamp));

        Log.d("ChatActivity", "Combined messages: " + allMessages.size());

        List<ChatMessage> messagesWithStatus = new ArrayList<>();
        for (ChatMessage msg : allMessages) {
            statusRef.child(msg.getMessageId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = snapshot.getValue(String.class);
                    if (status != null) {
                        msg.setStatus(status);
                        dbHelper.updateMessageStatus(msg.getMessageId(), status);
                    }
                    if (msg.getReceiverId().equals(senderId) && !msg.getStatus().equals("read")) {
                        statusRef.child(msg.getMessageId()).setValue("read");
                    }
                    messagesWithStatus.add(msg);

                    if (messagesWithStatus.size() == allMessages.size()) {
                        messagesWithStatus.sort(Comparator.comparingLong(ChatMessage::getTimestamp));
                        updateMessagesWithDateSeparators(messagesWithStatus);
                        messageAdapter.notifyDataSetChanged();
                        if (messages.size() > 0) {
                            recyclerMessages.scrollToPosition(messages.size() - 1);
                        }
                        Log.d("ChatActivity", "UI updated with " + messages.size() + " messages");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ChatActivity", "Realtime DB status fetch cancelled: " + error.getMessage());
                    messagesWithStatus.add(msg);
                    if (messagesWithStatus.size() == allMessages.size()) {
                        messagesWithStatus.sort(Comparator.comparingLong(ChatMessage::getTimestamp));
                        updateMessagesWithDateSeparators(messagesWithStatus);
                        messageAdapter.notifyDataSetChanged();
                        if (messages.size() > 0) {
                            recyclerMessages.scrollToPosition(messages.size() - 1);
                        }
                        Log.d("ChatActivity", "UI updated after cancel with " + messages.size() + " messages");
                    }
                }
            });
        }
        if (allMessages.isEmpty()) {
            messages.clear();
            messageAdapter.notifyDataSetChanged();
            Log.d("ChatActivity", "No messages, cleared UI");
        }
    }

    private void updateMessagesWithDateSeparators(List<ChatMessage> rawMessages) {
        messages.clear();
        if (rawMessages.isEmpty()) {
            messageAdapter.notifyDataSetChanged();
            return;
        }

        Calendar currentCal = Calendar.getInstance(TimeZone.getTimeZone("CAT"), Locale.getDefault());
        Calendar messageCal = Calendar.getInstance(TimeZone.getTimeZone("CAT"), Locale.getDefault());

        String lastDate = null;
        for (ChatMessage msg : rawMessages) {
            messageCal.setTimeInMillis(msg.getTimestamp());
            String messageDate = getDateString(messageCal, currentCal);
            if (!messageDate.equals(lastDate)) {
                messages.add(new DateSeparator(messageDate));
                lastDate = messageDate;
            }
            messages.add(msg);
        }
    }

    private String getDateString(Calendar messageCal, Calendar currentCal) {
        Calendar yesterdayCal = Calendar.getInstance(TimeZone.getTimeZone("CAT"), Locale.getDefault());
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        if (messageCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)) {
            return "Today";
        } else if (messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        } else {
            return dateFormat.format(messageCal.getTime());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
        if (statusListener != null) {
            statusRef.removeEventListener(statusListener);
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}