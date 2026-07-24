package com.turkhackteam.org;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupWelcomeActivity extends AppCompatActivity {
    CircleImageView groupDp;
    TextView groupNameTv, groupDescTv, rulesTv, welcomeTitle;
    Button acceptButton;
    ScrollView scrollView;
    String groupId, groupName;
    boolean scrolledToBottom = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_welcome);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        groupDp = findViewById(R.id.group_dp);
        groupNameTv = findViewById(R.id.group_name);
        groupDescTv = findViewById(R.id.group_description);
        welcomeTitle = findViewById(R.id.welcome_title);
        rulesTv = findViewById(R.id.rules_text);
        acceptButton = findViewById(R.id.accept_button);
        scrollView = findViewById(R.id.scroll_view);

        // Accept button hidden until scrolled to bottom
        acceptButton.setEnabled(false);
        acceptButton.setAlpha(0.5f);

        loadGroupInfo();

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View lastChild = scrollView.getChildAt(scrollView.getChildCount() - 1);
            int diff = (lastChild.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
            if (diff <= 20 && !scrolledToBottom) {
                scrolledToBottom = true;
                acceptButton.setEnabled(true);
                acceptButton.setAlpha(1.0f);
                acceptButton.setText("Kabul Ediyorum ✓");
            }
        });

        acceptButton.setOnClickListener(v -> {
            if (!scrolledToBottom) {
                Toast.makeText(this, "Kuralları okumak için aşağı kaydırın", Toast.LENGTH_SHORT).show();
                return;
            }
            acceptRulesAndJoin();
        });
    }

    private void loadGroupInfo() {
        if (groupId == null) { finish(); return; }
        FirebaseDatabase.getInstance().getReference("Groups").child(groupId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Group group = snapshot.getValue(Group.class);
                        if (group == null) { finish(); return; }

                        groupNameTv.setText(group.getName());
                        welcomeTitle.setText(group.getName() + " grubuna hoş geldiniz!");
                        groupDescTv.setText(group.getDescription() != null ? group.getDescription() : "");
                        rulesTv.setText(group.getRules() != null ? group.getRules() : "Kural belirtilmemiş.");

                        if (group.getDp() != null && !group.getDp().isEmpty()) {
                            Picasso.get().load(group.getDp()).into(groupDp);
                        }

                        // If rules are short enough, scroll check won't fire — enable directly
                        rulesTv.post(() -> {
                            if (rulesTv.getLineCount() <= 5) {
                                scrolledToBottom = true;
                                acceptButton.setEnabled(true);
                                acceptButton.setAlpha(1.0f);
                                acceptButton.setText("Kabul Ediyorum ✓");
                            }
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void acceptRulesAndJoin() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();
        String joinedAt = new SimpleDateFormat("MMM dd, yyyy HH:mm").format(Calendar.getInstance().getTime());

        // Mark rules as accepted and add member if not yet
        DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference("GroupMembers");
        DatabaseReference memberDataRef = FirebaseDatabase.getInstance().getReference("GroupMemberData");

        membersRef.child(myUid).child(groupId).setValue(true);

        FirebaseDatabase.getInstance().getReference("Accounts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Account account = snapshot.getValue(Account.class);
                        String displayName = account != null
                                ? (account.getFirstName() + " " + account.getLastName()).trim()
                                : "Üye";
                        String dp = account != null ? account.getDp() : "";

                        GroupMember member = new GroupMember(myUid, displayName, dp, "member", joinedAt);
                        member.setAcceptedRules(true);
                        memberDataRef.child(groupId).child(myUid).setValue(member);

                        // Update member count
                        FirebaseDatabase.getInstance().getReference("Groups")
                                .child(groupId).child("memberCount")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot s) {
                                        Integer count = s.getValue(Integer.class);
                                        FirebaseDatabase.getInstance().getReference("Groups")
                                                .child(groupId).child("memberCount")
                                                .setValue(count != null ? count + 1 : 1);
                                    }
                                    @Override public void onCancelled(DatabaseError e) {}
                                });

                        // Go to group main
                        Intent intent = new Intent(GroupWelcomeActivity.this, GroupMainActivity.class);
                        intent.putExtra("groupId", groupId);
                        intent.putExtra("groupName", groupName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}
