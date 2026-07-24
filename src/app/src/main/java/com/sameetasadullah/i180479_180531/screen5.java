package com.turkhackteam.org;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

public class screen5 extends ScreenshotDetectionActivity {
    private static final int PICK_IMAGE = 1;
    private static final int REQ_AUDIO_PERM = 200;

    ImageView backButton, makeCall, cameraImage, voiceMsgBtn, sendImageBtn;
    RecyclerView recyclerView;
    screen5RVAdaptor adaptor;
    List<message> messageList;
    TextView name, onlineStatus;
    EditText message;
    Chronometer chronometer;

    FirebaseDatabase database;
    DatabaseReference myRef, myRef1;
    String receiverID;
    FirebaseAuth mAuth;
    StorageReference storageReference;
    Account receiverAccount;
    boolean minimized = true;
    AudioRecorderHelper audioRecorder;
    boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen5);

        backButton = findViewById(R.id.back_button);
        makeCall = findViewById(R.id.make_call);
        recyclerView = findViewById(R.id.rv_messages);
        name = findViewById(R.id.name);
        onlineStatus = findViewById(R.id.online_status);
        message = findViewById(R.id.message);
        cameraImage = findViewById(R.id.camera_image);
        voiceMsgBtn = findViewById(R.id.voice_msg_btn);
        sendImageBtn = cameraImage;
        chronometer = findViewById(R.id.chronometer);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Messages");
        myRef.keepSynced(true);
        myRef1 = database.getReference("Accounts");
        myRef1.keepSynced(true);
        mAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("Images");
        audioRecorder = new AudioRecorderHelper(this);

        messageList = new ArrayList<>();
        adaptor = new screen5RVAdaptor(screen5.this, messageList);

        receiverID = getIntent().getStringExtra("receiverID");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adaptor);

        backButton.setOnClickListener(v -> { minimized = false; finish(); });

        // Voice call button
        if (makeCall != null) {
            makeCall.setOnClickListener(v -> {
                if (receiverAccount == null) return;
                Intent intent = new Intent(screen5.this, screen11.class);
                intent.putExtra("name", receiverAccount.getFirstName() + " " + receiverAccount.getLastName());
                intent.putExtra("receiverId", receiverID);
                intent.putExtra("isCaller", true);
                startActivity(intent);
            });
        }

        // Image send
        if (cameraImage != null) {
            cameraImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Resim seç"), PICK_IMAGE);
            });
        }

        // Voice message: hold to record
        if (voiceMsgBtn != null) {
            voiceMsgBtn.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    requestAudioAndRecord();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    stopAndSendVoice();
                    return true;
                }
                return false;
            });
        }

        // Send text on IME action
        message.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendTextMessage();
                return true;
            }
            return false;
        });

        // Also handle a send button if present
        ImageView sendBtn = findViewById(R.id.send_button);
        if (sendBtn != null) sendBtn.setOnClickListener(v -> sendTextMessage());

        // Load receiver info
        myRef1.child(receiverID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                receiverAccount = snapshot.getValue(Account.class);
                if (receiverAccount == null) return;
                name.setText(receiverAccount.getFirstName() + " " + receiverAccount.getLastName());
                onlineStatus.setText("online".equals(receiverAccount.getState()) ? "Çevrimiçi"
                        : "Son görülme: " + receiverAccount.getLastSeenTime());
                adaptor.setReceiverDP(receiverAccount.getDp());
                adaptor.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load messages
        if (mAuth.getCurrentUser() != null) {
            String myUID = mAuth.getCurrentUser().getUid();
            myRef.child(myUID).child(receiverID).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                    message msg = snapshot.getValue(message.class);
                    if (msg == null) return;
                    msg.setKey(snapshot.getKey());
                    messageList.add(msg);
                    adaptor.notifyItemInserted(messageList.size() - 1);
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                    message msg = snapshot.getValue(message.class);
                    if (msg == null) return;
                    String key = snapshot.getKey();
                    msg.setKey(key);
                    for (int i = 0; i < messageList.size(); i++) {
                        if (key != null && key.equals(messageList.get(i).getKey())) {
                            messageList.set(i, msg);
                            adaptor.notifyItemChanged(i);
                            break;
                        }
                    }
                }
                @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    String key = snapshot.getKey();
                    messageList.removeIf(m -> key != null && key.equals(m.getKey()));
                    adaptor.notifyDataSetChanged();
                }
                @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        updateUserStatus("online");
    }

    private void sendTextMessage() {
        String text = message.getText().toString().trim();
        if (text.isEmpty()) return;
        if (mAuth.getCurrentUser() == null) return;

        String myUID = mAuth.getCurrentUser().getUid();
        String timestamp = new SimpleDateFormat("hh:mm a").format(Calendar.getInstance().getTime());
        String msgKey = myRef.push().getKey();
        if (msgKey == null) return;

        // Check if message contains a URL
        boolean hasLink = LinkPreviewHelper.containsUrl(text);
        String linkUrl = hasLink ? LinkPreviewHelper.extractUrl(text) : null;

        com.turkhackteam.org.message newMsg = new com.turkhackteam.org.message(
                text, timestamp, "", msgKey, receiverID, myUID, "");
        newMsg.setType(hasLink ? "link" : "text");
        if (linkUrl != null) newMsg.setLinkUrl(linkUrl);

        // Save to both sender and receiver paths
        myRef.child(myUID).child(receiverID).child(msgKey).setValue(newMsg);
        myRef.child(receiverID).child(myUID).child(msgKey).setValue(newMsg);
        message.setText("");

        // Fetch link preview async if URL detected
        if (hasLink && linkUrl != null) {
            LinkPreviewHelper.fetchPreview(linkUrl, preview -> {
                if (preview == null) return;
                myRef.child(myUID).child(receiverID).child(msgKey).child("linkTitle").setValue(preview.title);
                myRef.child(myUID).child(receiverID).child(msgKey).child("linkDescription").setValue(preview.description);
                myRef.child(myUID).child(receiverID).child(msgKey).child("linkImage").setValue(preview.imageUrl);
                myRef.child(receiverID).child(myUID).child(msgKey).child("linkTitle").setValue(preview.title);
                myRef.child(receiverID).child(myUID).child(msgKey).child("linkDescription").setValue(preview.description);
                myRef.child(receiverID).child(myUID).child(msgKey).child("linkImage").setValue(preview.imageUrl);
            });
        }
    }

    private void requestAudioAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERM);
            return;
        }
        startVoiceRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(requestCode, perms, grants);
        if (requestCode == REQ_AUDIO_PERM && grants.length > 0
                && grants[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecording();
        }
    }

    private void startVoiceRecording() {
        if (isRecording) return;
        boolean ok = audioRecorder.startRecording();
        if (ok) {
            isRecording = true;
            if (chronometer != null) {
                chronometer.setVisibility(View.VISIBLE);
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
            }
            Toast.makeText(this, "Kayıt başladı — bırakınca gönderilir", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Kayıt başlatılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAndSendVoice() {
        if (!isRecording) return;
        if (chronometer != null) { chronometer.stop(); chronometer.setVisibility(View.GONE); }
        isRecording = false;

        String filePath = audioRecorder.stopRecording();
        if (filePath == null || mAuth.getCurrentUser() == null) return;

        String myUID = mAuth.getCurrentUser().getUid();
        String audioId = UUID.randomUUID().toString();
        StorageReference audioRef = FirebaseStorage.getInstance()
                .getReference("VoiceMessages").child(audioId + ".3gp");

        audioRef.putFile(Uri.fromFile(new File(filePath)))
                .addOnSuccessListener(task -> audioRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String timestamp = new SimpleDateFormat("hh:mm a")
                                    .format(Calendar.getInstance().getTime());
                            String msgKey = myRef.push().getKey();
                            if (msgKey == null) return;

                            com.turkhackteam.org.message voiceMsg = new com.turkhackteam.org.message(
                                    "🎵 Sesli mesaj", timestamp, "", msgKey, receiverID, myUID, "");
                            voiceMsg.setType("voice");
                            voiceMsg.setVoiceUrl(uri.toString());

                            myRef.child(myUID).child(receiverID).child(msgKey).setValue(voiceMsg);
                            myRef.child(receiverID).child(myUID).child(msgKey).setValue(voiceMsg);
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Sesli mesaj gönderilemedi", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            if (mAuth.getCurrentUser() == null) return;
            String myUID = mAuth.getCurrentUser().getUid();
            String imgId = UUID.randomUUID().toString();
            StorageReference imgRef = storageReference.child(imgId + ".jpg");
            imgRef.putFile(imageUri)
                    .addOnSuccessListener(task -> imgRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String timestamp = new SimpleDateFormat("hh:mm a")
                                        .format(Calendar.getInstance().getTime());
                                String msgKey = myRef.push().getKey();
                                if (msgKey == null) return;
                                com.turkhackteam.org.message imgMsg = new com.turkhackteam.org.message(
                                        "", timestamp, imgId, msgKey, receiverID, myUID, uri.toString());
                                imgMsg.setType("image");
                                myRef.child(myUID).child(receiverID).child(msgKey).setValue(imgMsg);
                                myRef.child(receiverID).child(myUID).child(msgKey).setValue(imgMsg);
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Resim gönderilemedi", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onScreenCaptured(String path) {
        if (mAuth.getCurrentUser() == null) return;
        String myUID = mAuth.getCurrentUser().getUid();
        String timestamp = new SimpleDateFormat("hh:mm a").format(Calendar.getInstance().getTime());
        String msgKey = myRef.push().getKey();
        if (msgKey == null) return;
        com.turkhackteam.org.message ssMsg = new com.turkhackteam.org.message(
                "📸 Ekran görüntüsü alındı", timestamp, "", msgKey, receiverID, myUID, "");
        ssMsg.setType("text");
        myRef.child(myUID).child(receiverID).child(msgKey).setValue(ssMsg);
        myRef.child(receiverID).child(myUID).child(msgKey).setValue(ssMsg);
    }

    private void updateUserStatus(String state) {
        if (mAuth.getCurrentUser() == null || mAuth.getUid() == null) return;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Accounts");
        Calendar cal = Calendar.getInstance();
        String date = new SimpleDateFormat("MMM dd, yyyy").format(cal.getTime());
        String time = new SimpleDateFormat("hh:mm a").format(cal.getTime());
        reference.child(mAuth.getUid()).child("state").setValue(state);
        reference.child(mAuth.getUid()).child("lastSeenTime").setValue(time);
        reference.child(mAuth.getUid()).child("lastSeenDate").setValue(date);
    }

    @Override public void onBackPressed() { super.onBackPressed(); minimized = false; }
    @Override public void onStart()   { super.onStart();  updateUserStatus("online");  minimized = true; }
    @Override public void onResume()  { super.onResume(); updateUserStatus("online");  minimized = true; }
    @Override public void onStop()    { super.onStop();   if (minimized) updateUserStatus("offline"); }
    @Override public void onDestroy() {
        super.onDestroy();
        if (minimized) updateUserStatus("offline");
        if (audioRecorder != null) audioRecorder.release();
    }
}
