package com.turkhackteam.org;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorderHelper {
    private static final String TAG = "AudioRecorderHelper";
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String currentFilePath;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Context context;

    public AudioRecorderHelper(Context context) {
        this.context = context;
    }

    public boolean startRecording() {
        try {
            File audioDir = new File(context.getCacheDir(), "VoiceMessages");
            if (!audioDir.exists()) audioDir.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            currentFilePath = audioDir.getAbsolutePath() + "/VoiceMsg_" + timeStamp + ".3gp";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed: " + e.getMessage());
            releaseRecorder();
            return false;
        }
    }

    public String stopRecording() {
        if (!isRecording) return null;
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            isRecording = false;
            releaseRecorder();
            return currentFilePath;
        } catch (Exception e) {
            Log.e(TAG, "stopRecording failed: " + e.getMessage());
            isRecording = false;
            releaseRecorder();
            return null;
        }
    }

    public void playAudio(String filePath, OnPlaybackCompleteListener listener) {
        if (isPlaying) stopPlaying();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                releasePlayer();
                if (listener != null) listener.onComplete();
            });
        } catch (IOException e) {
            Log.e(TAG, "playAudio failed: " + e.getMessage());
            releasePlayer();
        }
    }

    public void playAudioFromUrl(String url, OnPlaybackCompleteListener listener) {
        if (isPlaying) stopPlaying();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            isPlaying = true;
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                releasePlayer();
                if (listener != null) listener.onComplete();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                releasePlayer();
                return true;
            });
        } catch (IOException e) {
            Log.e(TAG, "playAudioFromUrl failed: " + e.getMessage());
        }
    }

    public void stopPlaying() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            isPlaying = false;
            releasePlayer();
        }
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception e) { /* ignore */ }
            mediaRecorder = null;
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception e) { /* ignore */ }
            mediaPlayer = null;
        }
    }

    public boolean isRecording() { return isRecording; }
    public boolean isPlaying() { return isPlaying; }
    public String getCurrentFilePath() { return currentFilePath; }

    public void release() {
        releaseRecorder();
        releasePlayer();
    }

    public interface OnPlaybackCompleteListener {
        void onComplete();
    }
}
