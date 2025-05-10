package com.chocolate.luswishi;

import android.app.Activity;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class UsersListAdapter extends ArrayAdapter<String> {

    Activity context;
    List<String> users;

    public UsersListAdapter(Activity context, List<String> users) {
        super(context, R.layout.user_list_item, users);
        this.context = context;
        this.users = users;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View listItem = inflater.inflate(R.layout.user_list_item, null, true);
        TextView name = listItem.findViewById(R.id.userName);
        name.setText(users.get(position));
        return listItem;
    }
}
