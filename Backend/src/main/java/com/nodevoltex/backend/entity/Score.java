package com.nodevoltex.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scores")
@EntityListeners(AuditingEntityListener.class)
public class Score {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatmap_id", nullable = false)
    private Beatmap beatmap;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
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

    private Double volforce = 0.0;

    @Column(columnDefinition = "TEXT")
    private String replayDataJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Score() {
    }

    public Score(UUID id, User user, Beatmap beatmap, Integer score, String grade, Integer maxCombo, Integer sCriticals, Integer criticals, Integer nears, Integer mids, Integer fars, Integer misses, Integer laserTicks, Integer laserMisses, Integer early, Integer late, String replayDataJson, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.beatmap = beatmap;
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
        this.volforce = 0.0;
        this.replayDataJson = replayDataJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Beatmap getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(Beatmap beatmap) {
        this.beatmap = beatmap;
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

    public String getReplayDataJson() {
        return replayDataJson;
    }

    public void setReplayDataJson(String replayDataJson) {
        this.replayDataJson = replayDataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getVolforce() {
        return volforce != null ? volforce : 0.0;
    }

    public void setVolforce(Double volforce) {
        this.volforce = volforce;
    }
}
