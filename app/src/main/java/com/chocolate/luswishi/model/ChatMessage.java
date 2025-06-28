package com.chocolate.luswishi.model;

public class ChatMessage extends MessageItem {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private String status; // pending, sent, delivered, read (for sender's perspective)
    private long timestamp;

    public ChatMessage() {
        // Required no-argument constructor for Firebase
    }

    public ChatMessage(String senderId, String receiverId, String text, String status, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters
    public String getMessageId() {
        return messageId;
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

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int getItemType() {
        return TYPE_MESSAGE;
    }
}