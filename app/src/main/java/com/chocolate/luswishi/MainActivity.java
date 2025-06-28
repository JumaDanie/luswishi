package com.chocolate.luswishi;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.top_app_bar); // Get reference to Toolbar
        BottomNavigationView nav = findViewById(R.id.bottom_nav_menu); // Bottom nav view

        // Load Chat fragment by default
        if (savedInstanceState == null) {
            loadFragment(new ChatListFragment(), "Chats");
        }

        // Example: Add a badge to the Chats tab
        BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_chat);
        badge.setVisible(true);
        badge.setNumber(3); // Example: 3 unread chats

        nav.setOnNavigationItemSelectedListener(item -> {
            Fragment selected = null;
            String title = "";

            if (item.getItemId() == R.id.nav_chat) {
                selected = new ChatListFragment();
                title = "Chats";
            } else if (item.getItemId() == R.id.nav_discover) {
                selected = new DiscoverFragment();
                title = "Discover";
            } else if (item.getItemId() == R.id.nav_me) {
                selected = new MeFragment();
                title = "Me";
            }

            if (selected != null) {
                loadFragment(selected, title);
            }

            return true;
        });
    }

    // Updated to include animation and title setting
    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in, android.R.anim.fade_out // You can change this to slide
                )
                .replace(R.id.fragment_container, fragment)
                .commit();
        toolbar.setTitle(title); // Update app bar title
    }
}
