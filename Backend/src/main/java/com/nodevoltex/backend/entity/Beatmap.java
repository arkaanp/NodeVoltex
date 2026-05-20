package com.nodevoltex.backend.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "beatmaps")
public class Beatmap {

    @Id
    @Column(nullable = false, unique = true)
    private String id; // e.g., "SongName_Difficulty"

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private Integer level;

    @OneToMany(mappedBy = "beatmap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Score> scores;

    public Beatmap() {
    }

    public Beatmap(String id, String title, String artist, String difficulty, Integer level, List<Score> scores) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.difficulty = difficulty;
        this.level = level;
        this.scores = scores;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public List<Score> getScores() {
        return scores;
    }

    public void setScores(List<Score> scores) {
        this.scores = scores;
    }
}
