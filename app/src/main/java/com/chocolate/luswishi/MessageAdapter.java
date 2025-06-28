package com.chocolate.luswishi;

import android.content.Context;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.chocolate.luswishi.model.DateSeparator;
import com.chocolate.luswishi.model.MessageItem;
import com.chocolate.luswishi.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<MessageItem> messageList;
    private final String currentUserId;
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final int TYPE_DATE_SEPARATOR = 3;

    public MessageAdapter(List<MessageItem> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        MessageItem item = messageList.get(position);
        if (item.getItemType() == MessageItem.TYPE_DATE_SEPARATOR) {
            return TYPE_DATE_SEPARATOR;
        }
        ChatMessage message = (ChatMessage) item;
        return message.getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == TYPE_RECEIVED) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_separator, parent, false);
            return new DateSeparatorViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageItem item = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ChatMessage message = (ChatMessage) item;
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.msgText.setText(message.getText());

            // Format and set message time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            sentHolder.messageTime.setText(timeFormat.format(new Date(message.getTimestamp())));

            // Dynamically adjust messageTime margins based on msgText line count
            sentHolder.msgText.post(() -> {
                int lineCount = sentHolder.msgText.getLineCount();
                int marginSide = lineCount > 1 ? dpToPx(4, holder.itemView.getContext()) : dpToPx(50, holder.itemView.getContext());
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) sentHolder.messageTime.getLayoutParams();
                params.rightMargin = marginSide; // Use rightMargin instead of setMarginEnd for compatibility
                sentHolder.messageTime.setLayoutParams(params);
            });

            switch (message.getStatus()) {
                case "pending":
                    sentHolder.statusIcon.setImageResource(R.drawable.ic_clock);
                    sentHolder.statusIcon.setImageTintList(ContextCompat.getColorStateList(
                            holder.itemView.getContext(), R.color.grey_tick));
                    break;
                case "sent":
                    sentHolder.statusIcon.setImageResource(R.drawable.ic_single_tick);
                    sentHolder.statusIcon.setImageTintList(ContextCompat.getColorStateList(
                            holder.itemView.getContext(), R.color.grey_tick));
                    break;
                case "delivered":
                    sentHolder.statusIcon.setImageResource(R.drawable.ic_double_tick);
                    sentHolder.statusIcon.setImageTintList(ContextCompat.getColorStateList(
                            holder.itemView.getContext(), R.color.grey_tick));
                    break;
                case "read":
                    sentHolder.statusIcon.setImageResource(R.drawable.ic_double_tick);
                    sentHolder.statusIcon.setImageTintList(ContextCompat.getColorStateList(
                            holder.itemView.getContext(), R.color.read_tick_color));
                    break;
                default:
                    sentHolder.statusIcon.setImageDrawable(null);
                    sentHolder.statusIcon.setImageTintList(null);
            }

        } else if (holder instanceof ReceivedMessageViewHolder) {
            ChatMessage message = (ChatMessage) item;
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.msgText.setText(message.getText());

            // Format and set message time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            receivedHolder.messageTime.setText(timeFormat.format(new Date(message.getTimestamp())));

            // Dynamically adjust messageTime margins based on msgText line count
            receivedHolder.msgText.post(() -> {
                int lineCount = receivedHolder.msgText.getLineCount();
                int marginSide = lineCount > 1 ? dpToPx(4, holder.itemView.getContext()) : dpToPx(50, holder.itemView.getContext());
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) receivedHolder.messageTime.getLayoutParams();
                params.leftMargin = marginSide; // Use leftMargin instead of setMarginStart for compatibility
                receivedHolder.messageTime.setLayoutParams(params);
            });

        } else if (holder instanceof DateSeparatorViewHolder) {
            DateSeparator separator = (DateSeparator) item;
            ((DateSeparatorViewHolder) holder).dateText.setText(separator.getDateText());
        }
    }

    private int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView msgText;
        TextView messageTime;
        ImageView statusIcon;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            msgText = itemView.findViewById(R.id.msgText);
            messageTime = itemView.findViewById(R.id.messageTime);
            statusIcon = itemView.findViewById(R.id.statusIcon);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView msgText;
        TextView messageTime;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            msgText = itemView.findViewById(R.id.msgText);
            messageTime = itemView.findViewById(R.id.messageTime);
        }
    }

    static class DateSeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;

        DateSeparatorViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}