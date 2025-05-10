package com.chocolate.luswishi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chocolate.luswishi.model.ChatOverview;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<ChatOverview> chatList;
    private Context context;

    public ChatListAdapter(List<ChatOverview> chatList, Context context) {
        this.chatList = chatList;
        this.context = context;
    }

    @NonNull
    @Override
    public ChatListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.ViewHolder holder, int position) {
        ChatOverview chat = chatList.get(position);

        // Always use provided userName or fallback to "Unknown User"
        String userName = chat.getUserName();
        if (userName != null && !userName.trim().isEmpty()) {
            holder.name.setText(userName);
        } else {
            holder.name.setText("Unknown User");
        }

        // Last message
        holder.lastMessage.setText(chat.getLastMessage());

        // Unread count with "Ka X"
        if (chat.getUnreadCount() > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText("Ka " + chat.getUnreadCount());
            holder.lastMessage.setTypeface(null, Typeface.BOLD);
        } else {
            holder.unreadCount.setVisibility(View.GONE);
            holder.lastMessage.setTypeface(null, Typeface.NORMAL);
        }

        // Timestamp
        if (chat.getTimestamp() > 0) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(chat.getTimestamp()));
            holder.timestamp.setText(time);
        } else {
            holder.timestamp.setText("");
        }

        // Always load default profile image
        Glide.with(context)
                .load(R.drawable.ic_user)
                .into(holder.profileImage);

        // On click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("userId", chat.getUserId());
            intent.putExtra("userName", userName); // Might be null or empty
            intent.putExtra("userImage", ""); // No image URL
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
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
