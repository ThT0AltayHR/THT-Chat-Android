package com.turkhackteam.org;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SEND_TEXT = 0;
    private static final int TYPE_RECV_TEXT = 1;
    private static final int TYPE_SEND_IMAGE = 2;
    private static final int TYPE_RECV_IMAGE = 3;
    private static final int TYPE_SEND_VOICE = 4;
    private static final int TYPE_RECV_VOICE = 5;

    Context context;
    List<GroupMessage> messages;
    String myUid, groupId, channelId;
    AudioRecorderHelper audioHelper;

    public GroupChannelAdapter(Context context, List<GroupMessage> messages, String myUid, String groupId, String channelId) {
        this.context = context;
        this.messages = messages;
        this.myUid = myUid;
        this.groupId = groupId;
        this.channelId = channelId;
        this.audioHelper = new AudioRecorderHelper(context);
    }

    @Override
    public int getItemViewType(int position) {
        GroupMessage msg = messages.get(position);
        boolean isMine = myUid.equals(msg.getSenderId());
        String type = msg.getType();
        if ("image".equals(type)) return isMine ? TYPE_SEND_IMAGE : TYPE_RECV_IMAGE;
        if ("voice".equals(type)) return isMine ? TYPE_SEND_VOICE : TYPE_RECV_VOICE;
        return isMine ? TYPE_SEND_TEXT : TYPE_RECV_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_SEND_TEXT:   return new TextVH(inf.inflate(R.layout.send_message_row, parent, false), true);
            case TYPE_RECV_TEXT:   return new TextVH(inf.inflate(R.layout.receive_message_row, parent, false), false);
            case TYPE_SEND_IMAGE:  return new ImageVH(inf.inflate(R.layout.send_image_row, parent, false), true);
            case TYPE_RECV_IMAGE:  return new ImageVH(inf.inflate(R.layout.receive_image_row, parent, false), false);
            case TYPE_SEND_VOICE:  return new VoiceVH(inf.inflate(R.layout.send_voice_message_row, parent, false), true);
            case TYPE_RECV_VOICE:  return new VoiceVH(inf.inflate(R.layout.receive_voice_message_row, parent, false), false);
            default:               return new TextVH(inf.inflate(R.layout.receive_message_row, parent, false), false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GroupMessage msg = messages.get(position);
        boolean isMine = myUid.equals(msg.getSenderId());

        if (holder instanceof TextVH) {
            TextVH h = (TextVH) holder;
            h.message.setText(msg.getMessage());
            h.time.setText(msg.getTimestamp());
            if (!isMine && h.senderName != null) h.senderName.setText(msg.getSenderName());
            if (!isMine && h.dp != null && msg.getSenderDp() != null && !msg.getSenderDp().isEmpty()) {
                Picasso.get().load(msg.getSenderDp()).into(h.dp);
            }
            if (isMine) {
                h.message.setOnLongClickListener(v -> {
                    showMessageOptions(msg, position);
                    return true;
                });
            }
        } else if (holder instanceof ImageVH) {
            ImageVH h = (ImageVH) holder;
            h.time.setText(msg.getTimestamp());
            if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                Picasso.get().load(msg.getImageUrl()).into(h.image);
            }
            if (!isMine && h.dp != null && msg.getSenderDp() != null && !msg.getSenderDp().isEmpty()) {
                Picasso.get().load(msg.getSenderDp()).into(h.dp);
            }
        } else if (holder instanceof VoiceVH) {
            VoiceVH h = (VoiceVH) holder;
            h.time.setText(msg.getTimestamp());
            if (!isMine && h.senderName != null) h.senderName.setText(msg.getSenderName());
            h.playBtn.setOnClickListener(v -> {
                if (msg.getVoiceUrl() != null && !msg.getVoiceUrl().isEmpty()) {
                    h.playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    audioHelper.playAudioFromUrl(msg.getVoiceUrl(), () ->
                            h.playBtn.setImageResource(android.R.drawable.ic_media_play));
                }
            });
        }
    }

    private void showMessageOptions(GroupMessage msg, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Mesaj");
        builder.setItems(new String[]{"Düzenle", "Sil"}, (dialog, which) -> {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("GroupMessages").child(groupId).child(channelId).child(msg.getMessageId());
            if (which == 0) {
                android.widget.EditText input = new android.widget.EditText(context);
                input.setText(msg.getMessage());
                new AlertDialog.Builder(context)
                        .setTitle("Mesajı Düzenle")
                        .setView(input)
                        .setPositiveButton("Kaydet", (d, w) -> {
                            String newText = input.getText().toString().trim();
                            if (!newText.isEmpty()) {
                                ref.child("message").setValue(newText);
                                ref.child("edited").setValue(true);
                            }
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            } else {
                ref.removeValue();
            }
        });
        builder.show();
    }

    @Override public int getItemCount() { return messages.size(); }

    static class TextVH extends RecyclerView.ViewHolder {
        TextView message, time, senderName;
        CircleImageView dp;
        TextVH(View v, boolean isMine) {
            super(v);
            message = v.findViewById(R.id.message);
            time = v.findViewById(R.id.time);
            if (!isMine) {
                dp = v.findViewById(R.id.dp);
                senderName = v.findViewById(R.id.sender_name);
            }
        }
    }

    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView time;
        CircleImageView dp;
        ImageVH(View v, boolean isMine) {
            super(v);
            image = v.findViewById(R.id.image);
            time = v.findViewById(R.id.time);
            if (!isMine) dp = v.findViewById(R.id.dp);
        }
    }

    static class VoiceVH extends RecyclerView.ViewHolder {
        ImageView playBtn;
        TextView time, senderName;
        CircleImageView dp;
        VoiceVH(View v, boolean isMine) {
            super(v);
            playBtn = v.findViewById(R.id.play_btn);
            time = v.findViewById(R.id.time);
            if (!isMine) {
                dp = v.findViewById(R.id.dp);
                senderName = v.findViewById(R.id.sender_name);
            }
        }
    }
}
