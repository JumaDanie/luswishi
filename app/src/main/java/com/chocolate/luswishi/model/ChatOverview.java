package com.chocolate.luswishi.model;

public class ChatOverview {
    private String userId;
    private String userName;      // Full name directly from database
    private String lastMessage;
    private int unreadCount;
    private String status;
    private long timestamp;

    // Default constructor required for Firebase
    public ChatOverview() {
    }

    // Constructor using userName directly
    public ChatOverview(String userId, String userName, String lastMessage,
                        int unreadCount, String status, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
