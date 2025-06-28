package com.chocolate.luswishi.model;

public abstract class MessageItem {
    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_DATE_SEPARATOR = 1;

    public abstract int getItemType();
    public abstract long getTimestamp();
}