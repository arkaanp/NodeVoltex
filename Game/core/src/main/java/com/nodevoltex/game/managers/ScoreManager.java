package com.nodevoltex.game.managers;

import com.nodevoltex.game.patterns.StrictJudgment;
import com.badlogic.gdx.utils.Array;

public class ScoreManager {
    public int combo = 0;
    public int maxCombo = 0;
    public String latestJudgment = "";

    // --- UR Bar Data Structure ---
    public static class HitMarker {
        public float offsetMs;
        public String tier;
        public float timer = 0f; // Used to fade out over time

        public HitMarker(float offsetMs, String tier) {
            this.offsetMs = offsetMs;
            this.tier = tier;
        }
    }
    public Array<HitMarker> recentHits = new Array<>();

    // --- Detailed Arcade Stat Trackers ---
    public static class StatCategory {
        public int sCriticals = 0, criticals = 0, nears = 0, mids = 0, fars = 0, misses = 0;
        public int early = 0, late = 0;
    }

    public StatCategory noteStats = new StatCategory();
    public StatCategory releaseStats = new StatCategory();

    public int laserTicks = 0;
    public int laserMisses = 0;

    private float currentHitScore = 0f;
    private float maxHitScore = 0f;

    private final StrictJudgment judgmentStrategy;

    public ScoreManager(StrictJudgment strategy) {
        this.judgmentStrategy = strategy;
    }

    // split Notes and Releases for the Max Score math
    public void setMaxPossibleScore(int totalNotes, int totalReleases, int totalLaserTicks) {
        this.maxHitScore = (totalNotes * 2.0f) + (totalReleases * 2.0f) + (totalLaserTicks * 1.5f);
    }

    public void onHit(float diffMs, String type) {
        StrictJudgment.JudgmentResult result = judgmentStrategy.evaluateJudgment(diffMs, type);
        StatCategory stats = type.equals("RELEASE") ? releaseStats : noteStats;

        if (!result.tier.equals("MISS")) {
            combo++;
            if (combo > maxCombo) maxCombo = combo;

            // --- Record the hit for the UR Bar (Ignore lasers) ---
            if (!type.equals("LASER")) {
                recentHits.add(new HitMarker(diffMs, result.tier));
            }
        } else {
            combo = 0;
        }

        // Only update UI text for Notes/Releases, and format it clearly (e.g., "NEAR EARLY")
        if (!result.tier.equals("S-CRITICAL") && !result.tier.equals("MISS")) {
            latestJudgment = result.tier + " " + result.timing;
        } else {
            latestJudgment = result.tier;
        }

        // Log Timing
        if (result.timing.equals("EARLY")) stats.early++;
        if (result.timing.equals("LATE"))  stats.late++;

        // Apply Points (Weights can be tweaked here)
        switch (result.tier) {
            case "S-CRITICAL": stats.sCriticals++; currentHitScore += 2.0f; break;
            case "CRITICAL":   stats.criticals++;  currentHitScore += 1.999f; break;
            case "NEAR":       stats.nears++;      currentHitScore += 1.0f; break;
            case "MID":        stats.mids++;       currentHitScore += 0.5f; break;
            case "FAR":        stats.fars++;       currentHitScore += 0.1f; break;
            case "MISS":       stats.misses++;     break;
        }
    }

    public void onMiss(String type) {
        combo = 0;
        latestJudgment = "MISS";

        if (type.equals("NOTE")) noteStats.misses++;
        else if (type.equals("RELEASE")) releaseStats.misses++;
        else if (type.equals("LASER")) laserMisses++;
    }

    // Called natively by the LaserManager
    public void onLaserTick() {
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        laserTicks++;
        currentHitScore += 1.5f;
        // we do NOT update latestJudgment here, keeping the UI clean
    }

    public int getFinalScore() {
        if (maxHitScore == 0) return 0;
        double ratio = (double) currentHitScore / (double) maxHitScore;
        return (int) (ratio * 10000000.0);
    }

    public String getGrade() {
        int score = getFinalScore();
        if (score >= 9900000) return "S";
        if (score >= 9800000) return "AAA+";
        if (score >= 9700000) return "AAA";
        if (score >= 9500000) return "AA+";
        if (score >= 9300000) return "AA";
        if (score >= 9000000) return "A+";
        if (score >= 8700000) return "A";
        if (score >= 7500000) return "B";
        if (score >= 6500000) return "C";
        return "D";
    }
}
