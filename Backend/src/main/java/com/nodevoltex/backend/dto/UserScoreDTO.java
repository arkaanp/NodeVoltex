package com.nodevoltex.backend.dto;

public class UserScoreDTO {
    private String mapId;
    private String title;
    private String artist;
    private String difficulty;
    private Integer level;

    private Integer score;
    private String grade;
    private Double volforce;
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
    private Long timestamp;

    public UserScoreDTO() {
    }

    public UserScoreDTO(String mapId, String title, String artist, String difficulty, Integer level, Integer score, String grade, Double volforce, Integer maxCombo, Integer sCriticals, Integer criticals, Integer nears, Integer mids, Integer fars, Integer misses, Integer laserTicks, Integer laserMisses, Integer early, Integer late, Long timestamp) {
        this.mapId = mapId;
        this.title = title;
        this.artist = artist;
        this.difficulty = difficulty;
        this.level = level;
        this.score = score;
        this.grade = grade;
        this.volforce = volforce;
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
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMapId() { return mapId; }
    public void setMapId(String mapId) { this.mapId = mapId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Double getVolforce() { return volforce; }
    public void setVolforce(Double volforce) { this.volforce = volforce; }

    public Integer getMaxCombo() { return maxCombo; }
    public void setMaxCombo(Integer maxCombo) { this.maxCombo = maxCombo; }

    public Integer getsCriticals() { return sCriticals; }
    public void setsCriticals(Integer sCriticals) { this.sCriticals = sCriticals; }

    public Integer getCriticals() { return criticals; }
    public void setCriticals(Integer criticals) { this.criticals = criticals; }

    public Integer getNears() { return nears; }
    public void setNears(Integer nears) { this.nears = nears; }

    public Integer getMids() { return mids; }
    public void setMids(Integer mids) { this.mids = mids; }

    public Integer getFars() { return fars; }
    public void setFars(Integer fars) { this.fars = fars; }

    public Integer getMisses() { return misses; }
    public void setMisses(Integer misses) { this.misses = misses; }

    public Integer getLaserTicks() { return laserTicks; }
    public void setLaserTicks(Integer laserTicks) { this.laserTicks = laserTicks; }

    public Integer getLaserMisses() { return laserMisses; }
    public void setLaserMisses(Integer laserMisses) { this.laserMisses = laserMisses; }

    public Integer getEarly() { return early; }
    public void setEarly(Integer early) { this.early = early; }

    public Integer getLate() { return late; }
    public void setLate(Integer late) { this.late = late; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
