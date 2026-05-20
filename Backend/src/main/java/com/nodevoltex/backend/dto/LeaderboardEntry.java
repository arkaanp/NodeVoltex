package com.nodevoltex.backend.dto;

public class LeaderboardEntry {
    private String username;
    private Integer score;
    private String grade;
    private Integer maxCombo;
    private Integer sCriticals;
    private Integer criticals;
    private Integer nears;
    private Integer mids;
    private Integer fars;
    private Integer misses;
    private Integer laserTicks;
    private Integer laserMisses;
    private Integer early;
    private Integer late;
    private String profilePictureUrl;
    private Long timestamp;
    private String replayDataJson;

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(String username, Integer score, String grade, Integer maxCombo, Integer sCriticals, Integer criticals, Integer nears, Integer mids, Integer fars, Integer misses, Integer laserTicks, Integer laserMisses, Integer early, Integer late, String profilePictureUrl, Long timestamp, String replayDataJson) {
        this.username = username;
        this.score = score;
        this.grade = grade;
        this.maxCombo = maxCombo;
        this.sCriticals = sCriticals;
        this.criticals = criticals;
        this.nears = nears;
        this.mids = mids;
        this.fars = fars;
        this.misses = misses;
        this.laserTicks = laserTicks;
        this.laserMisses = laserMisses;
        this.early = early;
        this.late = late;
        this.profilePictureUrl = profilePictureUrl;
        this.timestamp = timestamp;
        this.replayDataJson = replayDataJson;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Integer getsCriticals() {
        return sCriticals;
    }

    public void setsCriticals(Integer sCriticals) {
        this.sCriticals = sCriticals;
    }

    public Integer getCriticals() {
        return criticals;
    }

    public void setCriticals(Integer criticals) {
        this.criticals = criticals;
    }

    public Integer getNears() {
        return nears;
    }

    public void setNears(Integer nears) {
        this.nears = nears;
    }

    public Integer getMids() {
        return mids;
    }

    public void setMids(Integer mids) {
        this.mids = mids;
    }

    public Integer getFars() {
        return fars;
    }

    public void setFars(Integer fars) {
        this.fars = fars;
    }

    public Integer getMisses() {
        return misses;
    }

    public void setMisses(Integer misses) {
        this.misses = misses;
    }

    public Integer getLaserTicks() {
        return laserTicks;
    }

    public void setLaserTicks(Integer laserTicks) {
        this.laserTicks = laserTicks;
    }

    public Integer getLaserMisses() {
        return laserMisses;
    }

    public void setLaserMisses(Integer laserMisses) {
        this.laserMisses = laserMisses;
    }

    public Integer getEarly() {
        return early;
    }

    public void setEarly(Integer early) {
        this.early = early;
    }

    public Integer getLate() {
        return late;
    }

    public void setLate(Integer late) {
        this.late = late;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReplayDataJson() {
        return replayDataJson;
    }

    public void setReplayDataJson(String replayDataJson) {
        this.replayDataJson = replayDataJson;
    }
}
