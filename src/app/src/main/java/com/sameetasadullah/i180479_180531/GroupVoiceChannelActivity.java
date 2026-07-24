package com.turkhackteam.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupVoiceChannelActivity extends AppCompatActivity implements WebRTCManager.CallStateListener {
    String groupId, channelId, channelName, myUid, myName;
    TextView channelNameTv, statusTv;
    ImageView backBtn, muteBtn;
    RecyclerView participantsRV;
    ParticipantsAdapter adapter;
    List<GroupMember> participants = new ArrayList<>();
    DatabaseReference voiceRoomRef;
    FirebaseAuth mAuth;
    WebRTCManager webRTCManager;
    boolean isMuted = false;
    Map<String, WebRTCManager> peerManagers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_voice_channel);

        groupId = getIntent().getStringExtra("groupId");
        channelId = getIntent().getStringExtra("channelId");
        channelName = getIntent().getStringExtra("channelName");

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) { finish(); return; }
        myUid = mAuth.getCurrentUser().getUid();

        voiceRoomRef = FirebaseDatabase.getInstance()
                .getReference("VoiceRooms").child(groupId).child(channelId);

        channelNameTv = findViewById(R.id.channel_name);
        statusTv = findViewById(R.id.status_tv);
        backBtn = findViewById(R.id.back_btn);
        muteBtn = findViewById(R.id.mute_btn);
        participantsRV = findViewById(R.id.participants_rv);

        channelNameTv.setText("🔊 " + channelName);

        adapter = new ParticipantsAdapter(participants);
        participantsRV.setLayoutManager(new LinearLayoutManager(this));
        participantsRV.setAdapter(adapter);

        backBtn.setOnClickListener(v -> leaveChannel());

        muteBtn.setOnClickListener(v -> {
            isMuted = !isMuted;
            muteBtn.setImageResource(isMuted
                    ? android.R.drawable.ic_lock_silent_mode
                    : android.R.drawable.ic_btn_speak_now);
            for (WebRTCManager mgr : peerManagers.values()) {
                mgr.setMicEnabled(!isMuted);
            }
            Toast.makeText(this, isMuted ? "Mikrofon kapatıldı" : "Mikrofon açıldı", Toast.LENGTH_SHORT).show();
        });

        // Get my display name
        FirebaseDatabase.getInstance().getReference("Accounts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Account acc = snapshot.getValue(Account.class);
                        if (acc != null) {
                            myName = acc.getFirstName() + " " + acc.getLastName();
                        }
                        joinChannel();
                    }
                    @Override public void onCancelled(DatabaseError error) { joinChannel(); }
                });
    }

    private void joinChannel() {
        // Add myself to voice room
        Map<String, String> myEntry = new HashMap<>();
        myEntry.put("userId", myUid);
        myEntry.put("displayName", myName != null ? myName : "Üye");
        voiceRoomRef.child(myUid).setValue(myEntry);

        statusTv.setText("Bağlandı — " + channelName);

        // Listen for other participants
        voiceRoomRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String prev) {
                String userId = snapshot.getKey();
                if (userId == null || userId.equals(myUid)) return;

                // Get their profile
                FirebaseDatabase.getInstance().getReference("GroupMemberData")
                        .child(groupId).child(userId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot ds) {
                                GroupMember member = ds.getValue(GroupMember.class);
                                if (member == null) {
                                    member = new GroupMember(userId,
                                            snapshot.child("displayName").getValue(String.class),
                                            "", "member", "");
                                }
                                member.setUserId(userId);
                                participants.add(member);
                                adapter.notifyItemInserted(participants.size() - 1);

                                // Initiate WebRTC with this peer
                                initiateWebRTCWithPeer(userId);
                            }
                            @Override public void onCancelled(DatabaseError error) {}
                        });
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String userId = snapshot.getKey();
                if (userId == null) return;
                participants.removeIf(m -> userId.equals(m.getUserId()));
                adapter.notifyDataSetChanged();
                WebRTCManager mgr = peerManagers.remove(userId);
                if (mgr != null) mgr.release();
            }

            @Override public void onChildChanged(DataSnapshot s, String p) {}
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void initiateWebRTCWithPeer(String peerId) {
        // Simple: user with lexicographically smaller ID initiates
        if (myUid.compareTo(peerId) < 0) {
            WebRTCManager mgr = new WebRTCManager(this, this);
            peerManagers.put(peerId, mgr);
            mgr.startCall(myUid, peerId);
        }
        // Else: wait for their offer (handled via Firebase signaling in WebRTCManager)
    }

    private void leaveChannel() {
        voiceRoomRef.child(myUid).removeValue();
        for (WebRTCManager mgr : peerManagers.values()) mgr.release();
        peerManagers.clear();
        finish();
    }

    @Override public void onCallConnected() {}
    @Override public void onCallEnded() {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
    }

    @Override
    public void onBackPressed() {
        leaveChannel();
    }

    class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.PVH> {
        List<GroupMember> list;
        ParticipantsAdapter(List<GroupMember> l) { this.list = l; }

        @Override
        public PVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.voice_participant_row, parent, false);
            return new PVH(v);
        }

        @Override
        public void onBindViewHolder(PVH holder, int position) {
            GroupMember m = list.get(position);
            holder.name.setText(m.getDisplayName());
            if (m.getDp() != null && !m.getDp().isEmpty()) {
                Picasso.get().load(m.getDp()).into(holder.dp);
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class PVH extends RecyclerView.ViewHolder {
            TextView name;
            CircleImageView dp;
            PVH(View v) {
                super(v);
                name = v.findViewById(R.id.participant_name);
                dp = v.findViewById(R.id.participant_dp);
            }
        }
    }
}
