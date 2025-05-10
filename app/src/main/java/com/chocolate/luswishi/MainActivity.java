package com.chocolate.luswishi;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav_menu);
        // Load Chat fragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatListFragment())
                    .commit();
        }

        nav.setOnNavigationItemSelectedListener(item -> {
            Fragment selected = null;

            // Using 'if' condition instead of 'switch'
            if (item.getItemId() == R.id.nav_chat) {
                selected = new ChatListFragment();
            } else if (item.getItemId() == R.id.nav_discover) {
                selected = new DiscoverFragment();
            } else if (item.getItemId() == R.id.nav_me) {
                selected = new MeFragment();
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selected)
                    .commit();
            return true;
        });
    }
}