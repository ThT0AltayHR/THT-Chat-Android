package com.turkhackteam.org;

public class GroupMember {
    // role: "admin", "moderator", "member"
    private String userId, displayName, dp, role, joinedAt;
    private boolean acceptedRules, isMuted;

    public GroupMember() {}

    public GroupMember(String userId, String displayName, String dp, String role, String joinedAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.dp = dp;
        this.role = role;
        this.joinedAt = joinedAt;
        this.acceptedRules = false;
        this.isMuted = false;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDp() { return dp; }
    public void setDp(String dp) { this.dp = dp; }

    public String getRole() { return role != null ? role : "member"; }
    public void setRole(String role) { this.role = role; }

    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

    public boolean isAcceptedRules() { return acceptedRules; }
    public void setAcceptedRules(boolean acceptedRules) { this.acceptedRules = acceptedRules; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }
}
