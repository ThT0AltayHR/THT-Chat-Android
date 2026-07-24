package com.turkhackteam.org;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;

public class WebRTCManager {
    private static final String TAG = "WebRTCManager";

    private static final List<PeerConnection.IceServer> ICE_SERVERS = new ArrayList<PeerConnection.IceServer>() {{
        add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
        add(PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer());
    }};

    private Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private DatabaseReference callsRef;
    private String callId;
    private boolean isCaller;
    private CallStateListener listener;

    public WebRTCManager(Context context, CallStateListener listener) {
        this.context = context;
        this.listener = listener;
        this.callsRef = FirebaseDatabase.getInstance().getReference("Calls");
        initializePeerConnectionFactory();
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions options =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        PeerConnectionFactory.Options factoryOptions = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(factoryOptions)
                .createPeerConnectionFactory();
    }

    public String startCall(String callerId, String calleeId) {
        isCaller = true;
        callId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        setupPeerConnection();
        createAudioTrack();

        // Write call invitation to Firebase
        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", callerId);
        callData.put("calleeId", calleeId);
        callData.put("status", "calling");
        callData.put("callId", callId);
        callsRef.child(callId).setValue(callData);

        createOffer();
        listenForAnswer();
        return callId;
    }

    public void answerCall(String callId) {
        this.callId = callId;
        this.isCaller = false;
        setupPeerConnection();
        createAudioTrack();

        // Get offer from Firebase and create answer
        callsRef.child(callId).child("offer").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sdp = snapshot.child("sdp").getValue(String.class);
                    String type = snapshot.child("type").getValue(String.class);
                    if (sdp != null && type != null) {
                        SessionDescription remoteSdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), sdp);
                        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                            @Override
                            public void onSetSuccess() {
                                createAnswer();
                            }
                        }, remoteSdp);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        listenForIceCandidates();

        // Update status
        callsRef.child(callId).child("status").setValue("accepted");
    }

    private void setupPeerConnection() {
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(ICE_SERVERS);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        config.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

        peerConnection = peerConnectionFactory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState state) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                Log.d(TAG, "ICE state: " + state);
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    if (listener != null) listener.onCallConnected();
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED
                        || state == PeerConnection.IceConnectionState.FAILED
                        || state == PeerConnection.IceConnectionState.CLOSED) {
                    if (listener != null) listener.onCallEnded();
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                // Send candidate to remote peer via Firebase
                Map<String, Object> candidateMap = new HashMap<>();
                candidateMap.put("sdp", candidate.sdp);
                candidateMap.put("sdpMid", candidate.sdpMid);
                candidateMap.put("sdpMLineIndex", candidate.sdpMLineIndex);
                String path = isCaller ? "callerCandidates" : "calleeCandidates";
                callsRef.child(callId).child(path).push().setValue(candidateMap);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {}

            @Override
            public void onAddStream(MediaStream stream) {}

            @Override
            public void onRemoveStream(MediaStream stream) {}

            @Override
            public void onDataChannel(org.webrtc.DataChannel channel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {}
        });
    }

    private void createAudioTrack() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));

        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
        localAudioTrack.setEnabled(true);

        if (peerConnection != null) {
            peerConnection.addTrack(localAudioTrack);
        }
    }

    private void createOffer() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sdp);
                Map<String, String> offerMap = new HashMap<>();
                offerMap.put("sdp", sdp.description);
                offerMap.put("type", sdp.type.canonicalForm());
                callsRef.child(callId).child("offer").setValue(offerMap);
                listenForIceCandidates();
            }
        }, sdpConstraints);
    }

    private void createAnswer() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sdp);
                Map<String, String> answerMap = new HashMap<>();
                answerMap.put("sdp", sdp.description);
                answerMap.put("type", sdp.type.canonicalForm());
                callsRef.child(callId).child("answer").setValue(answerMap);
            }
        }, sdpConstraints);
    }

    private void listenForAnswer() {
        callsRef.child(callId).child("answer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && peerConnection != null
                        && peerConnection.getRemoteDescription() == null) {
                    String sdp = snapshot.child("sdp").getValue(String.class);
                    String type = snapshot.child("type").getValue(String.class);
                    if (sdp != null && type != null) {
                        SessionDescription remoteSdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), sdp);
                        peerConnection.setRemoteDescription(new SimpleSdpObserver(), remoteSdp);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForIceCandidates() {
        String remotePath = isCaller ? "calleeCandidates" : "callerCandidates";
        callsRef.child(callId).child(remotePath).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot candidateSnap : snapshot.getChildren()) {
                    try {
                        String sdp = candidateSnap.child("sdp").getValue(String.class);
                        String sdpMid = candidateSnap.child("sdpMid").getValue(String.class);
                        Integer sdpMLineIndex = candidateSnap.child("sdpMLineIndex").getValue(Integer.class);
                        if (sdp != null && sdpMid != null && sdpMLineIndex != null) {
                            IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                            if (peerConnection != null) peerConnection.addIceCandidate(candidate);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error adding ICE candidate: " + e.getMessage());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void endCall() {
        if (callId != null) {
            callsRef.child(callId).child("status").setValue("ended");
        }
        cleanup();
        if (listener != null) listener.onCallEnded();
    }

    public void rejectCall(String callId) {
        callsRef.child(callId).child("status").setValue("rejected");
        cleanup();
    }

    public void listenForCallStatus(String callId, CallStateListener statusListener) {
        callsRef.child(callId).child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status != null) {
                    switch (status) {
                        case "accepted": if (statusListener != null) statusListener.onCallConnected(); break;
                        case "rejected": if (statusListener != null) statusListener.onCallRejected(); break;
                        case "ended":    if (statusListener != null) statusListener.onCallEnded(); break;
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void setMicEnabled(boolean enabled) {
        if (localAudioTrack != null) localAudioTrack.setEnabled(enabled);
    }

    private void cleanup() {
        if (localAudioTrack != null) { localAudioTrack.dispose(); localAudioTrack = null; }
        if (audioSource != null) { audioSource.dispose(); audioSource = null; }
        if (peerConnection != null) { peerConnection.close(); peerConnection = null; }
    }

    public void release() {
        cleanup();
        if (peerConnectionFactory != null) { peerConnectionFactory.dispose(); peerConnectionFactory = null; }
    }

    // Simplified SdpObserver base
    private static abstract class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sdp) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) { Log.e(TAG, "SDP create failure: " + s); }
        @Override public void onSetFailure(String s) { Log.e(TAG, "SDP set failure: " + s); }
    }

    public interface CallStateListener {
        void onCallConnected();
        void onCallEnded();
        default void onCallRejected() {}
    }
}
