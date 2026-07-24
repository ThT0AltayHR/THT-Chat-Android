package com.turkhackteam.org;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMainActivity extends AppCompatActivity {
    String groupId, groupName, myRole = "member";
    TextView groupNameTv, memberCountTv;
    CircleImageView groupDp;
    ImageView settingsBtn, backBtn;
    RecyclerView channelsRV;
    ChannelListAdapter channelAdapter;
    List<GroupChannel> channelList = new ArrayList<>();
    DatabaseReference channelsRef;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_main);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        groupNameTv = findViewById(R.id.group_name);
        memberCountTv = findViewById(R.id.member_count);
        groupDp = findViewById(R.id.group_dp);
        settingsBtn = findViewById(R.id.settings_btn);
        backBtn = findViewById(R.id.back_btn);
        channelsRV = findViewById(R.id.channels_rv);

        mAuth = FirebaseAuth.getInstance();
        channelsRef = FirebaseDatabase.getInstance().getReference("GroupChannels").child(groupId);
        channelsRef.keepSynced(true);

        groupNameTv.setText(groupName);

        channelAdapter = new ChannelListAdapter(channelList);
        channelsRV.setLayoutManager(new LinearLayoutManager(this));
        channelsRV.setAdapter(channelAdapter);

        backBtn.setOnClickListener(v -> finish());

        // Load group info
        FirebaseDatabase.getInstance().getReference("Groups").child(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Group group = snapshot.getValue(Group.class);
                        if (group == null) return;
                        groupNameTv.setText(group.getName());
                        memberCountTv.setText(group.getMemberCount() + " üye");
                        if (group.getDp() != null && !group.getDp().isEmpty()) {
                            Picasso.get().load(group.getDp()).into(groupDp);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });

        // Load my role
        if (mAuth.getCurrentUser() != null) {
            String myUid = mAuth.getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference("GroupMemberData")
                    .child(groupId).child(myUid).child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String role = snapshot.getValue(String.class);
                            if (role != null) myRole = role;
                            settingsBtn.setVisibility(
                                    ("admin".equals(myRole) || "moderator".equals(myRole))
                                            ? View.VISIBLE : View.GONE);
                        }
                        @Override public void onCancelled(DatabaseError error) {}
                    });
        }

        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupSettingsActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("myRole", myRole);
            startActivity(intent);
        });

        // Load channels
        channelsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String prev) {
                GroupChannel ch = snapshot.getValue(GroupChannel.class);
                if (ch != null) { channelList.add(ch); channelAdapter.notifyDataSetChanged(); }
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String prev) {
                GroupChannel ch = snapshot.getValue(GroupChannel.class);
                if (ch == null) return;
                for (int i = 0; i < channelList.size(); i++) {
                    if (channelList.get(i).getChannelId() != null
                            && channelList.get(i).getChannelId().equals(ch.getChannelId())) {
                        channelList.set(i, ch);
                        channelAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
            @Override public void onChildRemoved(DataSnapshot s) {
                GroupChannel ch = s.getValue(GroupChannel.class);
                if (ch != null) channelList.removeIf(c -> c.getChannelId() != null && c.getChannelId().equals(ch.getChannelId()));
                channelAdapter.notifyDataSetChanged();
            }
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    class ChannelListAdapter extends RecyclerView.Adapter<ChannelListAdapter.CVH> {
        List<GroupChannel> list;
        ChannelListAdapter(List<GroupChannel> list) { this.list = list; }

        @Override
        public CVH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.group_channel_row, parent, false);
            return new CVH(v);
        }

        @Override
        public void onBindViewHolder(CVH holder, int position) {
            GroupChannel ch = list.get(position);
            holder.name.setText((ch.getType().equals("voice") ? "🔊 " : "# ") + ch.getName());

            if (!ch.isActive()) {
                holder.name.setAlpha(0.4f);
                holder.lockedIcon.setVisibility(View.VISIBLE);
            } else {
                holder.name.setAlpha(1f);
                holder.lockedIcon.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (!ch.isActive()) {
                    Toast.makeText(GroupMainActivity.this, "Bu kanal kapatılmış", Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("voice".equals(ch.getType())) {
                    Intent intent = new Intent(GroupMainActivity.this, GroupVoiceChannelActivity.class);
                    intent.putExtra("groupId", groupId);
                    intent.putExtra("channelId", ch.getChannelId());
                    intent.putExtra("channelName", ch.getName());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(GroupMainActivity.this, GroupChannelChatActivity.class);
                    intent.putExtra("groupId", groupId);
                    intent.putExtra("groupName", groupName);
                    intent.putExtra("channelId", ch.getChannelId());
                    intent.putExtra("channelName", ch.getName());
                    intent.putExtra("myRole", myRole);
                    startActivity(intent);
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class CVH extends RecyclerView.ViewHolder {
            TextView name, lockedIcon;
            CVH(View v) {
                super(v);
                name = v.findViewById(R.id.channel_name);
                lockedIcon = v.findViewById(R.id.locked_icon);
            }
        }
    }
}
