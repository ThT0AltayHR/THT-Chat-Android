package com.turkhackteam.org;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class fragmentsContainer extends AppCompatActivity {
    fragmentAdapter fragmentAdapter;
    ViewPager2 viewPager;
    ImageView callsImage, messagesImage, contactsImage, groupsImage;
    public boolean minimized = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragments_container);

        viewPager = findViewById(R.id.view_pager);
        callsImage = findViewById(R.id.calls_image);
        messagesImage = findViewById(R.id.messages_image);
        contactsImage = findViewById(R.id.contacts_image);
        groupsImage = findViewById(R.id.groups_image);

        fragmentAdapter = new fragmentAdapter(getSupportFragmentManager(), getLifecycle());
        fragmentAdapter.addFragment(new fragment_screen4(), "Mesajlar");
        fragmentAdapter.addFragment(new fragment_screen6(), "Kişiler");
        fragmentAdapter.addFragment(new fragment_screen10(), "Aramalar");
        fragmentAdapter.addFragment(new GroupListFragment(), "Gruplar");

        viewPager.setAdapter(fragmentAdapter);
        viewPager.setUserInputEnabled(false);

        setActiveTab(messagesImage);

        messagesImage.setOnClickListener(v -> { viewPager.setCurrentItem(0); setActiveTab(messagesImage); });
        contactsImage.setOnClickListener(v -> { viewPager.setCurrentItem(1); setActiveTab(contactsImage); });
        callsImage.setOnClickListener(v -> { viewPager.setCurrentItem(2); setActiveTab(callsImage); });
        groupsImage.setOnClickListener(v -> { viewPager.setCurrentItem(3); setActiveTab(groupsImage); });

        updateUserStatus("online");
    }

    private void setActiveTab(ImageView active) {
        ImageView[] tabs = {messagesImage, contactsImage, callsImage, groupsImage};
        for (ImageView tab : tabs) {
            if (tab == null) continue;
            tab.setColorFilter(tab == active ? Color.parseColor("#1E88E5") : Color.parseColor("#9E9E9E"));
        }
    }

    public void changeViewPager(int position) {
        if (viewPager == null || fragmentAdapter == null
                || position < 0 || position >= fragmentAdapter.getItemCount()) {
            return;
        }
        viewPager.setCurrentItem(position, false);
    }

    public void changeImageColorToBlue(int position) {
        ImageView[] tabs = {messagesImage, contactsImage, callsImage, groupsImage};
        if (position >= 0 && position < tabs.length) {
            setActiveTab(tabs[position]);
        }
    }

    private void updateUserStatus(String state) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Accounts");
        reference.keepSynced(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null || auth.getUid() == null) return;

        Calendar calDate = Calendar.getInstance();
        String date = new SimpleDateFormat("MMM dd, yyyy").format(calDate.getTime());
        String time = new SimpleDateFormat("hh:mm a").format(calDate.getTime());

        reference.child(auth.getUid()).child("state").setValue(state);
        reference.child(auth.getUid()).child("lastSeenTime").setValue(time);
        reference.child(auth.getUid()).child("lastSeenDate").setValue(date);
    }

    @Override public void onStart()   { super.onStart();   updateUserStatus("online");  minimized = true; }
    @Override public void onResume()  { super.onResume();  updateUserStatus("online");  minimized = true; }
    @Override public void onStop()    { super.onStop();    if (minimized) updateUserStatus("offline"); }
    @Override public void onDestroy() { super.onDestroy(); updateUserStatus("offline"); }
}
