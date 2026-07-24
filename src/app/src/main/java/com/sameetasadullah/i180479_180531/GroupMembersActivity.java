package com.turkhackteam.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMembersActivity extends AppCompatActivity {
    String groupId, myRole;
    RecyclerView recyclerView;
    MembersAdapter adapter;
    List<GroupMember> memberList = new ArrayList<>();
    DatabaseReference memberDataRef;
    FirebaseAuth mAuth;
    ImageView backBtn;
    TextView titleTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);

        groupId = getIntent().getStringExtra("groupId");
        myRole = getIntent().getStringExtra("myRole");

        mAuth = FirebaseAuth.getInstance();
        memberDataRef = FirebaseDatabase.getInstance()
                .getReference("GroupMemberData").child(groupId);
        memberDataRef.keepSynced(true);

        backBtn = findViewById(R.id.back_btn);
        titleTv = findViewById(R.id.title);
        recyclerView = findViewById(R.id.rv_members);

        titleTv.setText("Grup Üyeleri");
        backBtn.setOnClickListener(v -> finish());

        adapter = new MembersAdapter(memberList, myRole);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        memberDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String prev) {
                GroupMember member = snapshot.getValue(GroupMember.class);
                if (member != null) {
                    member.setUserId(snapshot.getKey());
                    memberList.add(member);
                    adapter.notifyItemInserted(memberList.size() - 1);
                }
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String prev) {
                GroupMember m = snapshot.getValue(GroupMember.class);
                if (m == null) return;
                String key = snapshot.getKey();
                for (int i = 0; i < memberList.size(); i++) {
                    if (key != null && key.equals(memberList.get(i).getUserId())) {
                        m.setUserId(key);
                        memberList.set(i, m);
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
            @Override public void onChildRemoved(DataSnapshot s) {
                String key = s.getKey();
                memberList.removeIf(m -> key != null && key.equals(m.getUserId()));
                adapter.notifyDataSetChanged();
            }
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MVH> {
        List<GroupMember> list;
        String actorRole;
        MembersAdapter(List<GroupMember> list, String actorRole) {
            this.list = list; this.actorRole = actorRole;
        }

        @Override
        public MVH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.group_member_manage_row, parent, false);
            return new MVH(v);
        }

        @Override
        public void onBindViewHolder(MVH holder, int position) {
            GroupMember member = list.get(position);
            holder.name.setText(member.getDisplayName());
            holder.role.setText(member.getRole());
            if (member.getDp() != null && !member.getDp().isEmpty()) {
                Picasso.get().load(member.getDp()).into(holder.dp);
            }

            // Role badge color
            switch (member.getRole()) {
                case "admin": holder.role.setTextColor(0xFFE53935); break;
                case "moderator": holder.role.setTextColor(0xFF1E88E5); break;
                default: holder.role.setTextColor(0xFF757575);
            }

            // Long click for admin actions
            if ("admin".equals(actorRole) && mAuth.getCurrentUser() != null
                    && !mAuth.getCurrentUser().getUid().equals(member.getUserId())) {
                holder.itemView.setOnLongClickListener(v -> {
                    showMemberActions(member, position);
                    return true;
                });
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class MVH extends RecyclerView.ViewHolder {
            TextView name, role;
            CircleImageView dp;
            MVH(View v) {
                super(v);
                name = v.findViewById(R.id.member_name);
                role = v.findViewById(R.id.member_role);
                dp = v.findViewById(R.id.member_dp);
            }
        }
    }

    private void showMemberActions(GroupMember member, int position) {
        String[] options;
        if ("member".equals(member.getRole())) {
            options = new String[]{"Moderatör Yap", "Admin'liği Devret", "Gruptan Çıkar"};
        } else if ("moderator".equals(member.getRole())) {
            options = new String[]{"Üye Yap", "Admin'liği Devret", "Gruptan Çıkar"};
        } else {
            options = new String[]{"Kapat"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(member.getDisplayName());
        builder.setItems(options, (dialog, which) -> {
            String action = options[which];
            if (action.equals("Moderatör Yap")) {
                changeRole(member, "moderator");
            } else if (action.equals("Üye Yap")) {
                changeRole(member, "member");
            } else if (action.equals("Admin'liği Devret")) {
                confirmTransferAdmin(member);
            } else if (action.equals("Gruptan Çıkar")) {
                removeMember(member, position);
            }
        });
        builder.show();
    }

    private void changeRole(GroupMember member, String newRole) {
        memberDataRef.child(member.getUserId()).child("role").setValue(newRole)
                .addOnSuccessListener(u -> Toast.makeText(this, "Rol güncellendi", Toast.LENGTH_SHORT).show());
    }

    private void confirmTransferAdmin(GroupMember member) {
        new AlertDialog.Builder(this)
                .setTitle("Admin'liği Devret")
                .setMessage(member.getDisplayName() + " kişisine admin'liği devretmek istediğinize emin misiniz? Siz moderatör olacaksınız.")
                .setPositiveButton("Evet, Devret", (d, w) -> {
                    // New member becomes admin
                    memberDataRef.child(member.getUserId()).child("role").setValue("admin");
                    // Current user becomes moderator
                    if (mAuth.getCurrentUser() != null) {
                        memberDataRef.child(mAuth.getCurrentUser().getUid()).child("role").setValue("moderator");
                    }
                    // Update group's createdBy
                    FirebaseDatabase.getInstance().getReference("Groups")
                            .child(groupId).child("createdBy").setValue(member.getUserId());
                    Toast.makeText(this, "Admin'lik devredildi", Toast.LENGTH_SHORT).show();
                    myRole = "moderator";
                    adapter.actorRole = "moderator";
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void removeMember(GroupMember member, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Gruptan Çıkar")
                .setMessage(member.getDisplayName() + " kişisini gruptan çıkarmak istediğinize emin misiniz?")
                .setPositiveButton("Çıkar", (d, w) -> {
                    memberDataRef.child(member.getUserId()).removeValue();
                    FirebaseDatabase.getInstance().getReference("GroupMembers")
                            .child(member.getUserId()).child(groupId).removeValue();
                    memberList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Üye çıkarıldı", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("İptal", null)
                .show();
    }
}
