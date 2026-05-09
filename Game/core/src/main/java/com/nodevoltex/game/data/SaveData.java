package com.nodevoltex.game.data;

public class SaveData {
    public int score = 0;
    public String grade = "-";
    public long timestamp = 0;

    // --- NEW: Arcade Statistics ---
    public int maxCombo = 0;

    // Note & Release Aggregates
    public int sCriticals = 0;
    public int criticals = 0;
    public int nears = 0;
    public int mids = 0;
    public int fars = 0;
    public int misses = 0;

    // Laser Stats
    public int laserTicks = 0;
    public int laserMisses = 0;

    // Timing Stats
    public int early = 0;
    public int late = 0;
}
