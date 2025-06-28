package com.chocolate.luswishi;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class AppTheme extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply the saved theme before any activity starts
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        int themeIndex = prefs.getInt("app_theme", 2); // Default to Dark

        switch (themeIndex) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }
}
