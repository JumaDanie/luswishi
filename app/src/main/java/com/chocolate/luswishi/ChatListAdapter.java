package com.chocolate.luswishi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chocolate.luswishi.model.ChatOverview;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<ChatOverview> chatList;
    private Context context;
    private DatabaseHelper dbHelper;
    private final Map<String, Boolean> profileFetchInProgress = new HashMap<>();

    public ChatListAdapter(List<ChatOverview> chatList, Context context) {
        this.context = context;
        this.chatList = chatList;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatOverview chat = chatList.get(position);

        String userName = chat.getUserName();
        holder.name.setText(userName != null && !userName.trim().isEmpty() ? userName : "Unknown User");

        String lastMessage = chat.getLastMessage() != null ? chat.getLastMessage() : "";
        holder.lastMessage.setText(lastMessage);

        int unreadCount = chat.getUnreadCount();
        if (unreadCount > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(unreadCount));
            holder.lastMessage.setTypeface(Typeface.DEFAULT_BOLD);
            holder.timestamp.setTextColor(ContextCompat.getColor(context, R.color.unread_timestamp));
        } else {
            holder.unreadCount.setVisibility(View.GONE);
            holder.lastMessage.setTypeface(Typeface.DEFAULT);
            holder.timestamp.setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_light));
        }

        if (chat.getTimestamp() > 0) {
            holder.timestamp.setText(formatTimestamp(chat.getTimestamp()));
        } else {
            holder.timestamp.setText("");
        }

        // Load profile image from SQLite or Firestore
        String userId = chat.getUserId();
        DatabaseHelper.User user = dbHelper.getUser(userId);
        if (user != null && user.profileImage != null && !user.profileImage.isEmpty()) {
            Log.d("ChatListAdapter", "Loading profile image from SQLite for userId: " + userId + ", profileUrl: " + user.profileImage);
            Glide.with(context)
                    .load(user.profileImage)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ChatListAdapter", "Glide failed to load image for userId: " + userId + ", profileUrl: " + model, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("ChatListAdapter", "Glide loaded image for userId: " + userId + ", profileUrl: " + model);
                            return false;
                        }
                    })
                    .into(holder.profileImage);
        } else if (!profileFetchInProgress.containsKey(userId)) {
            profileFetchInProgress.put(userId, true);
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener(snapshot -> {
                        String profileUrl = snapshot.getString("profileUrl");
                        String firstName = snapshot.getString("firstName");
                        String lastName = snapshot.getString("lastName");
                        String name = firstName != null && !firstName.isEmpty() ?
                                firstName + (lastName != null && !lastName.isEmpty() ? " " + lastName : "") : userName;
                        Log.d("ChatListAdapter", "Fetched profile from Firestore for userId: " + userId + ", profileUrl: " + profileUrl);
                        dbHelper.insertUser(userId, name, profileUrl);
                        Glide.with(context)
                                .load(profileUrl != null && !profileUrl.isEmpty() ? profileUrl : R.drawable.ic_user)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        Log.e("ChatListAdapter", "Glide failed to load image for userId: " + userId + ", profileUrl: " + model, e);
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        Log.d("ChatListAdapter", "Glide loaded image for userId: " + userId + ", profileUrl: " + model);
                                        return false;
                                    }
                                })
                                .into(holder.profileImage);
                        profileFetchInProgress.remove(userId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ChatListAdapter", "Error fetching profile image from Firestore for userId: " + userId, e);
                        Glide.with(context)
                                .load(R.drawable.ic_user)
                                .into(holder.profileImage);
                        profileFetchInProgress.remove(userId);
                    });
        }

        // Add click listener with logging
        holder.itemView.setOnClickListener(v -> {
            Log.d("ChatListAdapter", "Chat clicked for userId: " + userId + ", userName: " + userName);
            try {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("userName", userName);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("ChatListAdapter", "Error starting ChatActivity for userId: " + userId, e);
                Toast.makeText(context, "Failed to open chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private String formatTimestamp(long timestamp) {
        Calendar messageCal = Calendar.getInstance();
        messageCal.setTimeInMillis(timestamp);

        Calendar todayCal = Calendar.getInstance();
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (messageCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            return timeFormat.format(new Date(timestamp));
        } else if (messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        } else {
            return dateFormat.format(new Date(timestamp));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView name, lastMessage, timestamp, unreadCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.imageProfile);
            name = itemView.findViewById(R.id.textName);
            lastMessage = itemView.findViewById(R.id.textLastMessage);
            timestamp = itemView.findViewById(R.id.textTimestamp);
            unreadCount = itemView.findViewById(R.id.textUnreadCount);
        }
    }
}