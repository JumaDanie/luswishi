package com.chocolate.luswishi.model;

public class DateSeparator extends MessageItem {
    private String dateText;
    private long timestamp;

    public DateSeparator(String dateText) {
        this.dateText = dateText;
        this.timestamp = System.currentTimeMillis(); // You can override this later if needed
    }

    @Override
    public int getItemType() {
        return TYPE_DATE_SEPARATOR;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public String getDateText() {
        return dateText;
    }

    public void setDateText(String dateText) {
        this.dateText = dateText;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
