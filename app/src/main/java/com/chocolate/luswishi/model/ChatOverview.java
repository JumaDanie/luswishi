package com.chocolate.luswishi.model;

public class ChatOverview {
    private String userId; // This will be the ID of the OTHER user in the conversation
    private String userName; // This will be the name of the OTHER user
    // private String userImage; // REMOVED: We will fetch this dynamically from the 'users' collection
    private String lastMessage;
    private long timestamp;
    private int unreadCount;

    public ChatOverview() {
        // Required for Firebase
    }

    // Constructor modified to remove userImage
    public ChatOverview(String userId, String userName, String lastMessage, long timestamp, int unreadCount) {
        this.userId = userId;
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
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

    // REMOVED: public String getUserImage() and setUserImage()
    // You will fetch this based on getUserId() when displaying the overview.

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}