package com.chocolate.luswishi;

public class ChatMessage {
    private String messageId;  // Added messageId field
    private String senderId;
    private String receiverId;
    private String text;
    private String status;
    private long timestamp;

    // Default constructor required for Firebase
    public ChatMessage() {
        // Firebase needs an empty constructor
    }

    // Constructor to initialize the message fields
    public ChatMessage(String senderId, String receiverId, String text, String status, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getter and Setter for messageId
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    // Getters for other fields
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

    // Setter for status
    public void setStatus(String status) {
        this.status = status;
    }
}
