package com.turkhackteam.org;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupSettingsActivity extends AppCompatActivity {
    String groupId, myRole;
    Switch linksSwitch, videosSwitch, filesSwitch, messagingSwitch;
    Button addChannelBtn, manageMembersBtn, addVoiceChannelBtn;
    ImageView backBtn;
    TextView groupNameTv;
    DatabaseReference groupRef, channelsRef;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);

        groupId = getIntent().getStringExtra("groupId");
        myRole = getIntent().getStringExtra("myRole");

        mAuth = FirebaseAuth.getInstance();
        groupRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId);
        channelsRef = FirebaseDatabase.getInstance().getReference("GroupChannels").child(groupId);

        backBtn = findViewById(R.id.back_btn);
        groupNameTv = findViewById(R.id.group_name);
        linksSwitch = findViewById(R.id.links_switch);
        videosSwitch = findViewById(R.id.videos_switch);
        filesSwitch = findViewById(R.id.files_switch);
        messagingSwitch = findViewById(R.id.messaging_switch);
        addChannelBtn = findViewById(R.id.add_channel_btn);
        addVoiceChannelBtn = findViewById(R.id.add_voice_channel_btn);
        manageMembersBtn = findViewById(R.id.manage_members_btn);

        backBtn.setOnClickListener(v -> finish());

        boolean isAdmin = "admin".equals(myRole);

        // Load group settings
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null) return;
                groupNameTv.setText(group.getName());
                linksSwitch.setChecked(group.isLinksBlocked());
                videosSwitch.setChecked(group.isVideosBlocked());
                filesSwitch.setChecked(group.isFilesBlocked());
                messagingSwitch.setChecked(group.isMessagingBlocked());
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        // Toggle listeners — only admin/moderator can change
        if (isAdmin || "moderator".equals(myRole)) {
            linksSwitch.setOnCheckedChangeListener((b, checked) ->
                    groupRef.child("linksBlocked").setValue(checked));
            videosSwitch.setOnCheckedChangeListener((b, checked) ->
                    groupRef.child("videosBlocked").setValue(checked));
            filesSwitch.setOnCheckedChangeListener((b, checked) ->
                    groupRef.child("filesBlocked").setValue(checked));
            messagingSwitch.setOnCheckedChangeListener((b, checked) ->
                    groupRef.child("messagingBlocked").setValue(checked));
        } else {
            linksSwitch.setEnabled(false);
            videosSwitch.setEnabled(false);
            filesSwitch.setEnabled(false);
            messagingSwitch.setEnabled(false);
        }

        addChannelBtn.setOnClickListener(v -> showAddChannelDialog("text"));
        addVoiceChannelBtn.setOnClickListener(v -> showAddChannelDialog("voice"));

        manageMembersBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupMembersActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("myRole", myRole);
            startActivity(intent);
        });
    }

    private void showAddChannelDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle((type.equals("voice") ? "Sesli" : "Metin") + " Kanal Ekle");
        EditText input = new EditText(this);
        input.setHint("Kanal adı");
        builder.setView(input);
        builder.setPositiveButton("Ekle", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;
            String channelId = channelsRef.push().getKey();
            if (channelId == null) return;
            String createdAt = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm")
                    .format(java.util.Calendar.getInstance().getTime());
            GroupChannel ch = new GroupChannel(channelId, name, type, groupId, createdAt,
                    type.equals("text") ? 1 : 10);
            channelsRef.child(channelId).setValue(ch);
            Toast.makeText(this, "Kanal eklendi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }
}
