package com.turkhackteam.org;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class GroupListFragment extends Fragment {
    RecyclerView recyclerView;
    GroupListAdapter adapter;
    List<Group> groupList, filteredList;
    ImageView newGroup;
    EditText searchEditText;
    DatabaseReference groupsRef;
    FirebaseAuth mAuth;
    TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_list, container, false);

        recyclerView = view.findViewById(R.id.rv_groups);
        newGroup = view.findViewById(R.id.new_group);
        searchEditText = view.findViewById(R.id.search_edit_text);
        emptyText = view.findViewById(R.id.empty_text);

        mAuth = FirebaseAuth.getInstance();
        groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
        groupsRef.keepSynced(true);

        groupList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new GroupListAdapter(filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        newGroup.setOnClickListener(v -> startActivity(new Intent(getContext(), createGroup.class)));

        loadMyGroups();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString().trim());
            }
        });

        return view;
    }

    private void loadMyGroups() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        // Listen to all groups where I'm a member
        FirebaseDatabase.getInstance().getReference("GroupMembers")
                .child(myUid)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                        String groupId = snapshot.getKey();
                        if (groupId == null) return;
                        groupsRef.child(groupId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot gSnap) {
                                Group g = gSnap.getValue(Group.class);
                                if (g != null) {
                                    g.setGroupId(groupId);
                                    // avoid duplicates
                                    boolean exists = false;
                                    for (Group existing : groupList) {
                                        if (groupId.equals(existing.getGroupId())) { exists = true; break; }
                                    }
                                    if (!exists) {
                                        groupList.add(g);
                                        filterGroups(searchEditText != null ? searchEditText.getText().toString() : "");
                                    }
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        String groupId = snapshot.getKey();
                        groupList.removeIf(g -> groupId != null && groupId.equals(g.getGroupId()));
                        filterGroups(searchEditText != null ? searchEditText.getText().toString() : "");
                    }
                    @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void filterGroups(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(groupList);
        } else {
            for (Group g : groupList) {
                if (g.getName() != null && g.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(g);
                }
            }
        }
        if (emptyText != null) {
            emptyText.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    // Inner adapter for group list
    class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupVH> {
        List<Group> list;
        GroupListAdapter(List<Group> list) { this.list = list; }

        @NonNull
        @Override
        public GroupVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_row, parent, false);
            return new GroupVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupVH holder, int position) {
            Group group = list.get(position);
            holder.name.setText(group.getName());
            holder.memberCount.setText(group.getMemberCount() + " üye");
            if (group.getDp() != null && !group.getDp().isEmpty()) {
                Picasso.get().load(group.getDp()).placeholder(R.drawable.logo).into(holder.dp);
            }
            holder.itemView.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() == null) return;
                String myUid = mAuth.getCurrentUser().getUid();
                // Check if member has accepted rules
                FirebaseDatabase.getInstance().getReference("GroupMembers")
                        .child(myUid).child(group.getGroupId()).child("acceptedRules")
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Boolean accepted = snapshot.getValue(Boolean.class);
                                Intent intent;
                                if (accepted == null || !accepted) {
                                    intent = new Intent(getContext(), GroupWelcomeActivity.class);
                                } else {
                                    intent = new Intent(getContext(), GroupMainActivity.class);
                                }
                                intent.putExtra("groupId", group.getGroupId());
                                intent.putExtra("groupName", group.getName());
                                startActivity(intent);
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class GroupVH extends RecyclerView.ViewHolder {
            TextView name, memberCount;
            CircleImageView dp;
            GroupVH(View v) {
                super(v);
                name = v.findViewById(R.id.group_name);
                memberCount = v.findViewById(R.id.member_count);
                dp = v.findViewById(R.id.group_dp);
            }
        }
    }
}
