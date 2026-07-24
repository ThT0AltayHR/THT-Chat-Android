package com.turkhackteam.org;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class GroupChannelChatActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 2;

    String groupId, channelId, channelName, groupName, myRole;
    String myUid, myName, myDp;

    RecyclerView recyclerView;
    GroupChannelAdapter adapter;
    List<GroupMessage> messageList = new ArrayList<>();
    EditText messageInput;
    ImageView sendBtn, attachBtn, voiceMsgBtn, backBtn;
    TextView channelNameTv;
    Chronometer chronometer;

    DatabaseReference messagesRef;
    StorageReference storageRef;
    AudioRecorderHelper audioRecorder;
    FirebaseAuth mAuth;
    Group currentGroup;
    boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_channel_chat);

        groupId = getIntent().getStringExtra("groupId");
        channelId = getIntent().getStringExtra("channelId");
        channelName = getIntent().getStringExtra("channelName");
        groupName = getIntent().getStringExtra("groupName");
        myRole = getIntent().getStringExtra("myRole");

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) { finish(); return; }
        myUid = mAuth.getCurrentUser().getUid();

        messagesRef = FirebaseDatabase.getInstance()
                .getReference("GroupMessages").child(groupId).child(channelId);
        messagesRef.keepSynced(true);
        storageRef = FirebaseStorage.getInstance().getReference("GroupMedia").child(groupId);
        audioRecorder = new AudioRecorderHelper(this);

        recyclerView = findViewById(R.id.rv_messages);
        messageInput = findViewById(R.id.message_input);
        sendBtn = findViewById(R.id.send_btn);
        attachBtn = findViewById(R.id.attach_btn);
        voiceMsgBtn = findViewById(R.id.voice_msg_btn);
        backBtn = findViewById(R.id.back_btn);
        channelNameTv = findViewById(R.id.channel_name);
        chronometer = findViewById(R.id.chronometer);

        channelNameTv.setText("# " + channelName);

        adapter = new GroupChannelAdapter(this, messageList, myUid, groupId, channelId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backBtn.setOnClickListener(v -> finish());

        // Load my profile
        FirebaseDatabase.getInstance().getReference("Accounts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Account acc = snapshot.getValue(Account.class);
                        if (acc != null) {
                            myName = acc.getFirstName() + " " + acc.getLastName();
                            myDp = acc.getDp();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });

        // Load group settings
        FirebaseDatabase.getInstance().getReference("Groups").child(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentGroup = snapshot.getValue(Group.class);
                        updateInputState();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });

        sendBtn.setOnClickListener(v -> sendTextMessage());
        attachBtn.setOnClickListener(v -> pickImage());
        setupVoiceButton();

        // Load messages
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String prev) {
                GroupMessage msg = snapshot.getValue(GroupMessage.class);
                if (msg != null) {
                    msg.setMessageId(snapshot.getKey());
                    messageList.add(msg);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String prev) {
                GroupMessage msg = snapshot.getValue(GroupMessage.class);
                if (msg == null) return;
                String key = snapshot.getKey();
                for (int i = 0; i < messageList.size(); i++) {
                    if (key != null && key.equals(messageList.get(i).getMessageId())) {
                        messageList.set(i, msg);
                        messageList.get(i).setMessageId(key);
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
            @Override public void onChildRemoved(DataSnapshot s) {
                String key = s.getKey();
                messageList.removeIf(m -> key != null && key.equals(m.getMessageId()));
                adapter.notifyDataSetChanged();
            }
            @Override public void onChildMoved(DataSnapshot s, String p) {}
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateInputState() {
        if (currentGroup == null) return;
        boolean isAdmin = "admin".equals(myRole) || "moderator".equals(myRole);
        boolean blocked = currentGroup.isMessagingBlocked() && !isAdmin;
        messageInput.setEnabled(!blocked);
        sendBtn.setEnabled(!blocked);
        voiceMsgBtn.setEnabled(!blocked);
        attachBtn.setEnabled(!blocked && !currentGroup.isFilesBlocked());
        if (blocked) messageInput.setHint("Mesaj göndermek kısıtlandı");
        else messageInput.setHint("Mesaj yaz...");
    }

    private void sendTextMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        if (currentGroup != null && currentGroup.isLinksBlocked()
                && !"admin".equals(myRole) && !"moderator".equals(myRole)
                && LinkPreviewHelper.containsUrl(text)) {
            Toast.makeText(this, "Bu grupta link paylaşımı engellenmiş", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        GroupMessage msg = new GroupMessage(messageId, myUid,
                myName != null ? myName : "Ben", myDp != null ? myDp : "",
                text, "text", timestamp);
        messagesRef.child(messageId).setValue(msg);
        messageInput.setText("");
    }

    private void pickImage() {
        if (currentGroup != null && currentGroup.isFilesBlocked()
                && !"admin".equals(myRole) && !"moderator".equals(myRole)) {
            Toast.makeText(this, "Bu grupta dosya paylaşımı engellenmiş", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Resim seç"), PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            String imageId = UUID.randomUUID().toString();
            StorageReference imageRef = storageRef.child("images/" + imageId + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(task -> imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String timestamp = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
                                String messageId = messagesRef.push().getKey();
                                if (messageId == null) return;
                                GroupMessage msg = new GroupMessage(messageId, myUid,
                                        myName != null ? myName : "Ben", myDp != null ? myDp : "",
                                        "", "image", timestamp);
                                msg.setImageUrl(uri.toString());
                                messagesRef.child(messageId).setValue(msg);
                            }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Resim yüklenemedi", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupVoiceButton() {
        voiceMsgBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startVoiceRecording();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                stopAndSendVoiceMessage();
                return true;
            }
            return false;
        });
    }

    private void startVoiceRecording() {
        if (isRecording) return;
        boolean started = audioRecorder.startRecording();
        if (started) {
            isRecording = true;
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            voiceMsgBtn.setImageResource(android.R.drawable.ic_btn_speak_now);
            Toast.makeText(this, "Kayıt başladı...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Mikrofon izni gerekli", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAndSendVoiceMessage() {
        if (!isRecording) return;
        chronometer.stop();
        chronometer.setVisibility(View.GONE);
        isRecording = false;
        voiceMsgBtn.setImageResource(android.R.drawable.ic_btn_speak_now);

        String filePath = audioRecorder.stopRecording();
        if (filePath == null) { Toast.makeText(this, "Kayıt başarısız", Toast.LENGTH_SHORT).show(); return; }

        File audioFile = new File(filePath);
        String audioId = UUID.randomUUID().toString();
        StorageReference audioRef = storageRef.child("voice/" + audioId + ".3gp");
        audioRef.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(task -> audioRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String timestamp = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());
                            String messageId = messagesRef.push().getKey();
                            if (messageId == null) return;
                            GroupMessage msg = new GroupMessage(messageId, myUid,
                                    myName != null ? myName : "Ben", myDp != null ? myDp : "",
                                    "🎵 Sesli mesaj", "voice", timestamp);
                            msg.setVoiceUrl(uri.toString());
                            messagesRef.child(messageId).setValue(msg);
                        }))
                .addOnFailureListener(e -> Toast.makeText(this, "Sesli mesaj gönderilemedi", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecorder != null) audioRecorder.release();
    }
}
