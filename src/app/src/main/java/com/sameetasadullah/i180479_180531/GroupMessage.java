package com.turkhackteam.org;

public class GroupMessage {
    // type: "text", "image", "voice"
    private String messageId, senderId, senderName, senderDp, message, type, imageUrl, voiceUrl, timestamp;
    private boolean edited;

    public GroupMessage() {}

    public GroupMessage(String messageId, String senderId, String senderName, String senderDp,
                        String message, String type, String timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderDp = senderDp;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.edited = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderDp() { return senderDp; }
    public void setSenderDp(String senderDp) { this.senderDp = senderDp; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type != null ? type : "text"; }
    public void setType(String type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String voiceUrl) { this.voiceUrl = voiceUrl; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }
}
