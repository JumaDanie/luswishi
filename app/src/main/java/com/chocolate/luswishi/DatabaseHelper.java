package com.chocolate.luswishi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.chocolate.luswishi.model.ChatMessage;
import com.chocolate.luswishi.model.ChatOverview;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_db";
    private static final int DATABASE_VERSION = 3; // Incremented to 3 for schema change

    // Messages table
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_MESSAGE_ID = "messageId";
    private static final String COLUMN_SENDER_ID = "senderId";
    private static final String COLUMN_RECEIVER_ID = "receiverId";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "userId";
    private static final String COLUMN_USER_NAME = "userName";
    private static final String COLUMN_PROFILE_URL = "profileUrl"; // Changed to profileUrl

    // Chat overviews table
    private static final String TABLE_CHAT_OVERVIEWS = "chat_overviews";
    private static final String COLUMN_OVERVIEW_USER_ID = "userId";
    private static final String COLUMN_OVERVIEW_USER_NAME = "userName";
    private static final String COLUMN_LAST_MESSAGE = "lastMessage";
    private static final String COLUMN_OVERVIEW_TIMESTAMP = "timestamp";
    private static final String COLUMN_UNREAD_COUNT = "unreadCount";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Messages table
        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                COLUMN_MESSAGE_ID + " TEXT PRIMARY KEY, " +
                COLUMN_SENDER_ID + " TEXT NOT NULL, " +
                COLUMN_RECEIVER_ID + " TEXT NOT NULL, " +
                COLUMN_TEXT + " TEXT NOT NULL, " +
                COLUMN_STATUS + " TEXT NOT NULL, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL)";
        db.execSQL(createMessagesTable);

        // Users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER_NAME + " TEXT NOT NULL, " +
                COLUMN_PROFILE_URL + " TEXT)"; // Changed to profileUrl
        db.execSQL(createUsersTable);

        // Chat overviews table
        String createChatOverviewsTable = "CREATE TABLE " + TABLE_CHAT_OVERVIEWS + " (" +
                COLUMN_OVERVIEW_USER_ID + " TEXT PRIMARY KEY, " +
                COLUMN_OVERVIEW_USER_NAME + " TEXT NOT NULL, " +
                COLUMN_LAST_MESSAGE + " TEXT, " +
                COLUMN_OVERVIEW_TIMESTAMP + " INTEGER NOT NULL, " +
                COLUMN_UNREAD_COUNT + " INTEGER NOT NULL)";
        db.execSQL(createChatOverviewsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_OVERVIEWS);
            onCreate(db);
        } else if (oldVersion == 2) {
            // Rename profileImage to profileUrl
            db.execSQL("ALTER TABLE " + TABLE_USERS + " RENAME COLUMN profileImage TO profileUrl");
        }
    }

    // Messages methods
    public void insertMessage(ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_ID, message.getMessageId());
        values.put(COLUMN_SENDER_ID, message.getSenderId());
        values.put(COLUMN_RECEIVER_ID, message.getReceiverId());
        values.put(COLUMN_TEXT, message.getText());
        values.put(COLUMN_STATUS, message.getStatus());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public void updateMessageStatus(String messageId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status);
        db.update(TABLE_MESSAGES, values, COLUMN_MESSAGE_ID + " = ?", new String[]{messageId});
        db.close();
    }

    public void deleteMessage(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_MESSAGE_ID + " = ?", new String[]{messageId});
        db.close();
    }

    public List<ChatMessage> getMessages(String senderId, String receiverId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = "(" + COLUMN_SENDER_ID + " = ? AND " + COLUMN_RECEIVER_ID + " = ?) OR " +
                "(" + COLUMN_SENDER_ID + " = ? AND " + COLUMN_RECEIVER_ID + " = ?)";
        String[] selectionArgs = new String[]{senderId, receiverId, receiverId, senderId};
        Cursor cursor = db.query(TABLE_MESSAGES, null, selection, selectionArgs, null, null, COLUMN_TIMESTAMP + " ASC");

        if (cursor.moveToFirst()) {
            do {
                ChatMessage message = new ChatMessage();
                message.setMessageId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)));
                message.setSenderId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID)));
                message.setReceiverId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECEIVER_ID)));
                message.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)));
                message.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messages;
    }

    // Users methods
    public void insertUser(String userId, String userName, String profileUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_USER_NAME, userName);
        values.put(COLUMN_PROFILE_URL, profileUrl); // Changed to profileUrl
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public User getUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USER_ID + " = ?", new String[]{userId}, null, null, null);
        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            user.userName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME));
            user.profileImage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_URL)); // Changed to profileUrl
        }
        cursor.close();
        db.close();
        return user;
    }

    // Chat overviews methods
    public void insertChatOverview(ChatOverview overview) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_OVERVIEW_USER_ID, overview.getUserId());
        values.put(COLUMN_OVERVIEW_USER_NAME, overview.getUserName());
        values.put(COLUMN_LAST_MESSAGE, overview.getLastMessage());
        values.put(COLUMN_OVERVIEW_TIMESTAMP, overview.getTimestamp());
        values.put(COLUMN_UNREAD_COUNT, overview.getUnreadCount());
        db.insertWithOnConflict(TABLE_CHAT_OVERVIEWS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<ChatOverview> getChatOverviews() {
        List<ChatOverview> overviews = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHAT_OVERVIEWS, null, null, null, null, null, COLUMN_OVERVIEW_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                ChatOverview overview = new ChatOverview();
                overview.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OVERVIEW_USER_ID)));
                overview.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OVERVIEW_USER_NAME)));
                overview.setLastMessage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_MESSAGE)));
                overview.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_OVERVIEW_TIMESTAMP)));
                overview.setUnreadCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_UNREAD_COUNT)));
                overviews.add(overview);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return overviews;
    }

    // Helper class for user data
    public static class User {
        public String userId;
        public String userName;
        public String profileImage; // Note: This is mapped to profileUrl in SQLite
    }
}