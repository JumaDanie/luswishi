package com.chocolate.luswishi;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import com.chocolate.luswishi.model.User;

public class DiscoverFragment extends Fragment {
    private RecyclerView recyclerView;
    private DiscoverAdapter adapter;
    private List<User> users = new ArrayList<>();
    private String currentUid;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);
        recyclerView = view.findViewById(R.id.recyclerDiscover);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DiscoverAdapter(users, u -> {
            // onClick → start ChatActivity
            Intent i = new Intent(getContext(), ChatActivity.class);
            i.putExtra("userId", u.getId());
            i.putExtra("userName", u.getFirstName()+" "+u.getLastName());
            i.putExtra("userImage", u.getProfileUrl());
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                users.clear();
                for (DataSnapshot s : snap.getChildren()) {
                    if (!s.getKey().equals(currentUid)) {
                        User u = s.getValue(User.class);
                        u.setId(s.getKey());
                        users.add(u);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError err) {}
        });

        return view;
    }
}
