package com.chocolate.luswishi;

import com.chocolate.luswishi.model.MessageItem;

public class ChatMessage extends MessageItem {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private String status; // pending, sent, delivered, read
    private long timestamp;

    // Default constructor for Firebase
    public ChatMessage() {
    }

    // Constructor to initialize fields
    public ChatMessage(String senderId, String receiverId, String text, String status, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getText() {
        return text;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int getItemType() {
        return TYPE_MESSAGE;
    }
}
