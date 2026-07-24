package com.turkhackteam.org;

public class GroupChannel {
    // type: "text" or "voice"
    private String channelId, name, type, groupId, createdAt;
    private boolean isActive;
    private int order;

    public GroupChannel() {}

    public GroupChannel(String channelId, String name, String type, String groupId, String createdAt, int order) {
        this.channelId = channelId;
        this.name = name;
        this.type = type;
        this.groupId = groupId;
        this.createdAt = createdAt;
        this.isActive = true;
        this.order = order;
    }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
