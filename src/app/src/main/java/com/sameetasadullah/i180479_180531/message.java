package com.turkhackteam.org;

public class message {
    // type: "text", "image", "voice", "link"
    private String message, time, location, key, receiverID, senderID, image, voiceUrl, type, linkUrl, linkTitle, linkDescription, linkImage;
    private boolean edited;

    public message() {}

    public message(String message, String time, String location, String key, String receiverID, String senderID, String image) {
        this.message = message;
        this.time = time;
        this.location = location;
        this.key = key;
        this.receiverID = receiverID;
        this.senderID = senderID;
        this.image = image;
        this.type = (image != null && !image.isEmpty()) ? "image" : "text";
        this.edited = false;
    }

    public String getReceiverID() { return receiverID; }
    public void setReceiverID(String receiverID) { this.receiverID = receiverID; }

    public String getSenderID() { return senderID; }
    public void setSenderID(String senderID) { this.senderID = senderID; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String voiceUrl) { this.voiceUrl = voiceUrl; }

    public String getType() { return type != null ? type : (image != null && !image.isEmpty() ? "image" : "text"); }
    public void setType(String type) { this.type = type; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public String getLinkTitle() { return linkTitle; }
    public void setLinkTitle(String linkTitle) { this.linkTitle = linkTitle; }

    public String getLinkDescription() { return linkDescription; }
    public void setLinkDescription(String linkDescription) { this.linkDescription = linkDescription; }

    public String getLinkImage() { return linkImage; }
    public void setLinkImage(String linkImage) { this.linkImage = linkImage; }
}
