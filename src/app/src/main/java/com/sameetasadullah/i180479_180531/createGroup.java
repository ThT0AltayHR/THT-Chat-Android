package com.turkhackteam.org;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class createGroup extends AppCompatActivity {
    public static final int PICK_IMAGE = 1;
    ImageView addDisplayPic;
    CircleImageView dp;
    EditText groupName, groupDescription, groupRules;
    Button create;
    Uri imageURI = null;
    FirebaseAuth mAuth;
    DatabaseReference groupsRef;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        addDisplayPic = findViewById(R.id.add_display_pic);
        groupName = findViewById(R.id.group_name);
        groupDescription = findViewById(R.id.group_description);
        groupRules = findViewById(R.id.group_rules);
        dp = findViewById(R.id.dp);
        create = findViewById(R.id.create);

        mAuth = FirebaseAuth.getInstance();
        groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
        storageReference = FirebaseStorage.getInstance().getReference("GroupDPs");

        addDisplayPic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Grup fotoğrafı seç"), PICK_IMAGE);
        });

        create.setOnClickListener(v -> createGroupAction());
    }

    private void createGroupAction() {
        String name = groupName.getText().toString().trim();
        String description = groupDescription != null ? groupDescription.getText().toString().trim() : "";
        String rules = groupRules.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Grup adı boş olamaz", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rules.isEmpty()) {
            Toast.makeText(this, "Grup kurallarını girmek zorunludur", Toast.LENGTH_SHORT).show();
            return;
        }

        create.setEnabled(false);
        create.setText("Oluşturuluyor...");

        if (imageURI != null) {
            String imageId = UUID.randomUUID().toString();
            StorageReference imageRef = storageReference.child(imageId + ".jpg");
            imageRef.putFile(imageURI)
                    .addOnSuccessListener(task -> imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> saveGroupToFirebase(name, description, rules, uri.toString()))
                            .addOnFailureListener(e -> saveGroupToFirebase(name, description, rules, "")))
                    .addOnFailureListener(e -> saveGroupToFirebase(name, description, rules, ""));
        } else {
            saveGroupToFirebase(name, description, rules, "");
        }
    }

    private void saveGroupToFirebase(String name, String description, String rules, String dpUrl) {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        String groupId = groupsRef.push().getKey();
        if (groupId == null) { create.setEnabled(true); create.setText("Oluştur"); return; }

        String createdAt = new SimpleDateFormat("MMM dd, yyyy HH:mm").format(Calendar.getInstance().getTime());

        // Get creator name first
        FirebaseDatabase.getInstance().getReference("Accounts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Account account = snapshot.getValue(Account.class);
                        String creatorName = account != null
                                ? (account.getFirstName() + " " + account.getLastName()).trim()
                                : "Bilinmeyen";

                        Group group = new Group(groupId, name, description, rules, myUid, creatorName, dpUrl, createdAt);

                        groupsRef.child(groupId).setValue(group)
                                .addOnSuccessListener(unused -> {
                                    // Add creator as admin member
                                    GroupMember adminMember = new GroupMember(myUid, creatorName,
                                            account != null ? account.getDp() : "", "admin", createdAt);
                                    adminMember.setAcceptedRules(true);

                                    DatabaseReference membersRef = FirebaseDatabase.getInstance()
                                            .getReference("GroupMembers");
                                    membersRef.child(myUid).child(groupId).setValue(true);
                                    FirebaseDatabase.getInstance().getReference("GroupMemberData")
                                            .child(groupId).child(myUid).setValue(adminMember);

                                    // Create default channels
                                    createDefaultChannels(groupId, createdAt);
                                })
                                .addOnFailureListener(e -> {
                                    create.setEnabled(true);
                                    create.setText("Oluştur");
                                    Toast.makeText(createGroup.this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        create.setEnabled(true);
                        create.setText("Oluştur");
                    }
                });
    }

    private void createDefaultChannels(String groupId, String createdAt) {
        DatabaseReference channelsRef = FirebaseDatabase.getInstance()
                .getReference("GroupChannels").child(groupId);

        // Default text channels
        String ch1Id = channelsRef.push().getKey();
        String ch2Id = channelsRef.push().getKey();
        // Default voice channels
        String vc1Id = channelsRef.push().getKey();
        String vc2Id = channelsRef.push().getKey();
        String vc3Id = channelsRef.push().getKey();

        if (ch1Id != null) channelsRef.child(ch1Id).setValue(new GroupChannel(ch1Id, "genel", "text", groupId, createdAt, 0));
        if (ch2Id != null) channelsRef.child(ch2Id).setValue(new GroupChannel(ch2Id, "duyurular", "text", groupId, createdAt, 1));
        if (vc1Id != null) channelsRef.child(vc1Id).setValue(new GroupChannel(vc1Id, "Sesli Kanal 1", "voice", groupId, createdAt, 10));
        if (vc2Id != null) channelsRef.child(vc2Id).setValue(new GroupChannel(vc2Id, "Sesli Kanal 2", "voice", groupId, createdAt, 11));
        if (vc3Id != null) channelsRef.child(vc3Id).setValue(new GroupChannel(vc3Id, "Sesli Kanal 3", "voice", groupId, createdAt, 12));

        // Go to group main
        Intent intent = new Intent(createGroup.this, GroupMainActivity.class);
        intent.putExtra("groupId", groupId);
        intent.putExtra("groupName", groupName.getText().toString().trim());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            addDisplayPic.setAlpha(0f);
            imageURI = data.getData();
            if (dp != null) dp.setImageURI(imageURI);
        }
    }
}
