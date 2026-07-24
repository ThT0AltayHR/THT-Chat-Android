package com.turkhackteam.org;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

public class screen11 extends AppCompatActivity implements WebRTCManager.CallStateListener {
    RelativeLayout rl_end_call;
    ImageView muteBtn, speakerBtn;
    TextView tv_name, callStatus;
    CircleImageView callerDp;
    Chronometer chronometer;

    WebRTCManager webRTCManager;
    boolean isCaller;
    boolean isMuted = false;
    boolean isSpeaker = false;
    boolean minimized = true;
    String callId, receiverName, receiverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen11);

        rl_end_call = findViewById(R.id.rl_end_call);
        tv_name = findViewById(R.id.tv_name);
        callStatus = findViewById(R.id.call_status);
        chronometer = findViewById(R.id.chronometer);
        muteBtn = findViewById(R.id.mute_btn);
        speakerBtn = findViewById(R.id.speaker_btn);
        callerDp = findViewById(R.id.caller_dp);

        receiverName = getIntent().getStringExtra("name");
        receiverId = getIntent().getStringExtra("receiverId");
        callId = getIntent().getStringExtra("callId");
        isCaller = getIntent().getBooleanExtra("isCaller", true);

        tv_name.setText(receiverName != null ? receiverName : "Aranan");
        callStatus.setText("Aranıyor...");

        // Load receiver's profile picture
        if (receiverId != null) {
            FirebaseDatabase.getInstance().getReference("Accounts").child(receiverId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Account acc = snapshot.getValue(Account.class);
                            if (acc != null && acc.getDp() != null && !acc.getDp().isEmpty() && callerDp != null) {
                                Picasso.get().load(acc.getDp()).into(callerDp);
                            }
                        }
                        @Override public void onCancelled(DatabaseError error) {}
                    });
        }

        webRTCManager = new WebRTCManager(this, this);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String myUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (isCaller && myUid != null && receiverId != null) {
            // We're making the call
            callId = webRTCManager.startCall(myUid, receiverId);
            // Listen for answer/rejection
            webRTCManager.listenForCallStatus(callId, this);
        } else if (callId != null) {
            // We're answering
            callStatus.setText("Bağlanıyor...");
            webRTCManager.answerCall(callId);
        }

        rl_end_call.setOnClickListener(v -> {
            minimized = false;
            webRTCManager.endCall();
            finish();
        });

        muteBtn.setOnClickListener(v -> {
            isMuted = !isMuted;
            webRTCManager.setMicEnabled(!isMuted);
            muteBtn.setImageResource(isMuted
                    ? android.R.drawable.ic_lock_silent_mode
                    : android.R.drawable.ic_btn_speak_now);
        });

        speakerBtn.setOnClickListener(v -> {
            isSpeaker = !isSpeaker;
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) am.setSpeakerphoneOn(isSpeaker);
        });

        updateUserStatus("online");
    }

    @Override
    public void onCallConnected() {
        runOnUiThread(() -> {
            callStatus.setText("Bağlandı");
            if (chronometer != null) {
                chronometer.setVisibility(View.VISIBLE);
                chronometer.start();
            }
        });
    }

    @Override
    public void onCallEnded() {
        runOnUiThread(() -> {
            minimized = false;
            if (!isFinishing()) finish();
        });
    }

    @Override
    public void onCallRejected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Çağrı reddedildi", Toast.LENGTH_SHORT).show();
            minimized = false;
            if (!isFinishing()) finish();
        });
    }

    private void updateUserStatus(String state) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Accounts");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || auth.getUid() == null) return;

        Calendar cal = Calendar.getInstance();
        String date = new SimpleDateFormat("MMM dd, yyyy").format(cal.getTime());
        String time = new SimpleDateFormat("hh:mm a").format(cal.getTime());

        reference.child(auth.getUid()).child("state").setValue(state);
        reference.child(auth.getUid()).child("lastSeenTime").setValue(time);
        reference.child(auth.getUid()).child("lastSeenDate").setValue(date);
    }

    @Override public void onBackPressed() { super.onBackPressed(); minimized = false; }
    @Override public void onStart() { super.onStart(); updateUserStatus("online"); minimized = true; }
    @Override public void onResume() { super.onResume(); updateUserStatus("online"); minimized = true; }
    @Override public void onStop() { super.onStop(); if (minimized) updateUserStatus("offline"); }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (minimized) updateUserStatus("offline");
        if (webRTCManager != null) webRTCManager.release();
        if (chronometer != null) chronometer.stop();
    }
}
