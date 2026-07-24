package com.turkhackteam.org;

public class Group {
    private String groupId, name, description, rules, createdBy, createdByName, dp, createdAt;
    private boolean linksBlocked, videosBlocked, filesBlocked, messagingBlocked;
    private int memberCount;

    public Group() {}

    public Group(String groupId, String name, String description, String rules,
                 String createdBy, String createdByName, String dp, String createdAt) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.rules = rules;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.dp = dp;
        this.createdAt = createdAt;
        this.linksBlocked = false;
        this.videosBlocked = false;
        this.filesBlocked = false;
        this.messagingBlocked = false;
        this.memberCount = 1;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getDp() { return dp; }
    public void setDp(String dp) { this.dp = dp; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isLinksBlocked() { return linksBlocked; }
    public void setLinksBlocked(boolean linksBlocked) { this.linksBlocked = linksBlocked; }

    public boolean isVideosBlocked() { return videosBlocked; }
    public void setVideosBlocked(boolean videosBlocked) { this.videosBlocked = videosBlocked; }

    public boolean isFilesBlocked() { return filesBlocked; }
    public void setFilesBlocked(boolean filesBlocked) { this.filesBlocked = filesBlocked; }

    public boolean isMessagingBlocked() { return messagingBlocked; }
    public void setMessagingBlocked(boolean messagingBlocked) { this.messagingBlocked = messagingBlocked; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
