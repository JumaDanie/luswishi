package com.chocolate.luswishi.model;

public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String profileUrl;

    public User() {
        // Needed for Firebase
    }

    public User(String id, String firstName, String lastName, String profileUrl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileUrl = profileUrl;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }
}
