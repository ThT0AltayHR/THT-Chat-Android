package com.turkhackteam.org;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class screen5RVAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // ViewType constants
    private static final int TYPE_SEND_TEXT  = R.layout.send_message_row;
    private static final int TYPE_RECV_TEXT  = R.layout.receive_message_row;
    private static final int TYPE_SEND_IMAGE = R.layout.send_image_row;
    private static final int TYPE_RECV_IMAGE = R.layout.receive_image_row;
    private static final int TYPE_SEND_VOICE = R.layout.send_voice_message_row;
    private static final int TYPE_RECV_VOICE = R.layout.receive_voice_message_row;

    Context context;
    List<message> messageList;
    FirebaseAuth mAuth;
    String receiverDP;
    DatabaseReference reference;
    AudioRecorderHelper audioHelper;

    public screen5RVAdaptor(Context context, List<message> messageList) {
        this.context = context;
        this.messageList = messageList;
        mAuth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("Messages");
        reference.keepSynced(true);
        audioHelper = new AudioRecorderHelper(context);
    }

    public String getReceiverDP() { return receiverDP; }
    public void setReceiverDP(String receiverDP) { this.receiverDP = receiverDP; }

    @Override
    public int getItemViewType(int position) {
        message msg = messageList.get(position);
        boolean isMine = mAuth.getCurrentUser() != null &&
                mAuth.getCurrentUser().getUid().equals(msg.getSenderID());
        String type = msg.getType();
        if ("voice".equals(type)) return isMine ? TYPE_SEND_VOICE : TYPE_RECV_VOICE;
        if ("image".equals(type) || (msg.getImage() != null && !msg.getImage().isEmpty()))
            return isMine ? TYPE_SEND_IMAGE : TYPE_RECV_IMAGE;
        return isMine ? TYPE_SEND_TEXT : TYPE_RECV_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == TYPE_SEND_TEXT) {
            itemView = LayoutInflater.from(context).inflate(R.layout.send_message_row, null, false);
            return new SendMsgVH(itemView);
        } else if (viewType == TYPE_RECV_TEXT) {
            itemView = LayoutInflater.from(context).inflate(R.layout.receive_message_row, null, false);
            return new RecvMsgVH(itemView);
        } else if (viewType == TYPE_SEND_IMAGE) {
            itemView = LayoutInflater.from(context).inflate(R.layout.send_image_row, null, false);
            return new SendImgVH(itemView);
        } else if (viewType == TYPE_RECV_IMAGE) {
            itemView = LayoutInflater.from(context).inflate(R.layout.receive_image_row, null, false);
            return new RecvImgVH(itemView);
        } else if (viewType == TYPE_SEND_VOICE) {
            itemView = LayoutInflater.from(context).inflate(R.layout.send_voice_message_row, null, false);
            return new VoiceVH(itemView, true);
        } else {
            itemView = LayoutInflater.from(context).inflate(R.layout.receive_voice_message_row, null, false);
            return new VoiceVH(itemView, false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        message msg = messageList.get(position);

        if (holder instanceof SendMsgVH) {
            SendMsgVH h = (SendMsgVH) holder;
            String text = msg.getMessage();
            if (msg.isEdited()) text += " (düzenlendi)";
            h.message.setText(text);
            h.time.setText(msg.getTime());
            // Link preview check
            if ("link".equals(msg.getType()) && msg.getLinkUrl() != null) {
                h.linkPreview.setVisibility(View.VISIBLE);
                h.linkTitle.setText(msg.getLinkTitle() != null ? msg.getLinkTitle() : msg.getLinkUrl());
                h.linkDesc.setText(msg.getLinkDescription() != null ? msg.getLinkDescription() : "");
                if (msg.getLinkImage() != null && !msg.getLinkImage().isEmpty()) {
                    Picasso.get().load(msg.getLinkImage()).into(h.linkImg);
                }
                h.linkPreview.setOnClickListener(v -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getLinkUrl()));
                        context.startActivity(i);
                    } catch (Exception ignored) {}
                });
            } else {
                h.linkPreview.setVisibility(View.GONE);
            }
            h.relativeLayout.setOnLongClickListener(v -> { showEditDeleteDialog(msg, position); return true; });
        } else if (holder instanceof RecvMsgVH) {
            RecvMsgVH h = (RecvMsgVH) holder;
            h.message.setText(msg.getMessage());
            h.time.setText(msg.getTime());
            if (receiverDP != null && !receiverDP.isEmpty() && h.dp != null) {
                Picasso.get().load(receiverDP).placeholder(R.drawable.chat_bubbles).into(h.dp);
            }
            // Link preview
            if ("link".equals(msg.getType()) && msg.getLinkUrl() != null) {
                h.linkPreview.setVisibility(View.VISIBLE);
                h.linkTitle.setText(msg.getLinkTitle() != null ? msg.getLinkTitle() : msg.getLinkUrl());
                h.linkDesc.setText(msg.getLinkDescription() != null ? msg.getLinkDescription() : "");
                if (msg.getLinkImage() != null && !msg.getLinkImage().isEmpty()) {
                    Picasso.get().load(msg.getLinkImage()).into(h.linkImg);
                }
                h.linkPreview.setOnClickListener(v -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(msg.getLinkUrl()));
                        context.startActivity(i);
                    } catch (Exception ignored) {}
                });
            } else {
                h.linkPreview.setVisibility(View.GONE);
            }
        } else if (holder instanceof SendImgVH) {
            SendImgVH h = (SendImgVH) holder;
            h.time.setText(msg.getTime());
            if (msg.getImage() != null && !msg.getImage().isEmpty()) {
                Picasso.get().load(msg.getImage()).into(h.image);
            }
        } else if (holder instanceof RecvImgVH) {
            RecvImgVH h = (RecvImgVH) holder;
            h.time.setText(msg.getTime());
            if (msg.getImage() != null && !msg.getImage().isEmpty()) {
                Picasso.get().load(msg.getImage()).into(h.image);
            }
            if (receiverDP != null && !receiverDP.isEmpty() && h.dp != null) {
                Picasso.get().load(receiverDP).into(h.dp);
            }
        } else if (holder instanceof VoiceVH) {
            VoiceVH h = (VoiceVH) holder;
            h.time.setText(msg.getTime());
            if (receiverDP != null && !receiverDP.isEmpty() && h.dp != null) {
                Picasso.get().load(receiverDP).into(h.dp);
            }
            h.playBtn.setOnClickListener(v -> {
                if (msg.getVoiceUrl() != null && !msg.getVoiceUrl().isEmpty()) {
                    h.playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    audioHelper.playAudioFromUrl(msg.getVoiceUrl(), () ->
                            h.playBtn.setImageResource(android.R.drawable.ic_media_play));
                }
            });
        }
    }

    private void showEditDeleteDialog(message msg, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Mesaj Seçenekleri");
        builder.setItems(new String[]{"Düzenle", "Sil"}, (dialog, which) -> {
            if (which == 0) {
                android.widget.EditText input = new android.widget.EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(msg.getMessage());
                new AlertDialog.Builder(context)
                        .setTitle("Mesajı Düzenle")
                        .setView(input)
                        .setPositiveButton("Kaydet", (d, w) -> {
                            String newText = input.getText().toString().trim();
                            if (newText.isEmpty()) return;
                            String senderId = msg.getSenderID();
                            String receiverId = msg.getReceiverID();
                            if (senderId == null || receiverId == null || msg.getKey() == null) return;
                            reference.child(senderId).child(receiverId).child(msg.getKey()).child("message").setValue(newText);
                            reference.child(senderId).child(receiverId).child(msg.getKey()).child("edited").setValue(true);
                            reference.child(receiverId).child(senderId).child(msg.getKey()).child("message").setValue(newText);
                            reference.child(receiverId).child(senderId).child(msg.getKey()).child("edited").setValue(true);
                        })
                        .setNegativeButton("İptal", null).show();
            } else {
                String senderId = msg.getSenderID();
                String receiverId = msg.getReceiverID();
                if (senderId == null || receiverId == null || msg.getKey() == null) return;
                reference.child(senderId).child(receiverId).child(msg.getKey()).removeValue();
                reference.child(receiverId).child(senderId).child(msg.getKey()).removeValue();
            }
        });
        builder.show();
    }

    @Override public int getItemCount() { return messageList.size(); }

    // ViewHolders
    static class SendMsgVH extends RecyclerView.ViewHolder {
        TextView message, time;
        RelativeLayout relativeLayout, linkPreview;
        TextView linkTitle, linkDesc;
        ImageView linkImg;
        SendMsgVH(View v) {
            super(v);
            relativeLayout = v.findViewById(R.id.rl_send_message);
            message = v.findViewById(R.id.message);
            time = v.findViewById(R.id.time);
            linkPreview = v.findViewById(R.id.link_preview);
            linkTitle = v.findViewById(R.id.link_title);
            linkDesc = v.findViewById(R.id.link_desc);
            linkImg = v.findViewById(R.id.link_img);
        }
    }

    static class RecvMsgVH extends RecyclerView.ViewHolder {
        TextView message, time;
        RelativeLayout linkPreview;
        TextView linkTitle, linkDesc;
        ImageView linkImg;
        CircleImageView dp;
        RecvMsgVH(View v) {
            super(v);
            message = v.findViewById(R.id.message);
            time = v.findViewById(R.id.time);
            dp = v.findViewById(R.id.dp);
            linkPreview = v.findViewById(R.id.link_preview);
            linkTitle = v.findViewById(R.id.link_title);
            linkDesc = v.findViewById(R.id.link_desc);
            linkImg = v.findViewById(R.id.link_img);
        }
    }

    static class SendImgVH extends RecyclerView.ViewHolder {
        TextView time;
        ImageView image;
        SendImgVH(View v) { super(v); time = v.findViewById(R.id.time); image = v.findViewById(R.id.image); }
    }

    static class RecvImgVH extends RecyclerView.ViewHolder {
        TextView time;
        ImageView image;
        CircleImageView dp;
        RecvImgVH(View v) { super(v); time = v.findViewById(R.id.time); image = v.findViewById(R.id.image); dp = v.findViewById(R.id.dp); }
    }

    static class VoiceVH extends RecyclerView.ViewHolder {
        ImageView playBtn;
        TextView time;
        CircleImageView dp;
        VoiceVH(View v, boolean isSend) {
            super(v);
            playBtn = v.findViewById(R.id.play_btn);
            time = v.findViewById(R.id.time);
            if (!isSend) dp = v.findViewById(R.id.dp);
        }
    }
}
