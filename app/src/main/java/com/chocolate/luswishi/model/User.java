package com.chocolate.luswishi.model;

import com.google.firebase.auth.FirebaseAuth;

public class User {
    private String firstName;
    private String lastName;
    private String profileUrl;
    private String id;
    private String email;
    private String phoneNumber;

    public User() {
        // Required for Firestore
    }

    public User(String firstName, String lastName, String profileUrl, String id, String email, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileUrl = profileUrl;
        this.id = id;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public User(String firstName, String lastName, String profileUrl) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileUrl = profileUrl;
        this.id = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        this.email = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        this.phoneNumber = ""; // You can set default or fetch from FirebaseUser if available
    }

    // Getters and setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
