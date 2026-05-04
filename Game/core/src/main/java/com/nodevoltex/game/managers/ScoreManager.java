package com.nodevoltex.game.managers;

import com.nodevoltex.game.patterns.GameArchitecture;

public class ScoreManager implements GameArchitecture.HitObserver {
    public int combo = 0;
    public int maxCombo = 0;
    public String latestJudgment = "";

    // --- NEW: Stat Trackers ---
    public int sCriticals = 0;
    public int criticals = 0;
    public int nears = 0;
    public int misses = 0;

    // Internal score weighting (e.g., S-Crit = 2, Crit = 1, Near = 0.5)
    private float currentHitScore = 0f;
    private float maxHitScore = 0f;

    private final GameArchitecture.JudgmentStrategy judgmentStrategy;

    public ScoreManager(GameArchitecture.JudgmentStrategy strategy) {
        this.judgmentStrategy = strategy;
    }

    // Call this before the map starts to set the maximum possible points
    public void setMaxPossibleScore(int totalNotes, int totalLaserTicks) {
        // Assuming S-Criticals are worth 2 points, and laser ticks are worth 2 points
        this.maxHitScore = (totalNotes * 2f) + (totalLaserTicks * 2f);
    }

    @Override
    public void onHit(float diffMs) {
        latestJudgment = judgmentStrategy.evaluateJudgment(diffMs);
        combo++;
        if (combo > maxCombo) maxCombo = combo;

        switch (latestJudgment) {
            case "S-CRITICAL": sCriticals++; currentHitScore += 2f; break;
            case "CRITICAL":   criticals++;  currentHitScore += 2f; break;
            case "NEAR":       nears++;      currentHitScore += 1f; break;
        }
    }

    @Override
    public void onMiss() {
        latestJudgment = "MISS";
        combo = 0;
        misses++;
    }

    @Override
    public void onLaserTick() {
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        // Lasers act like perfect holds
        sCriticals++;
        currentHitScore += 2f;
    }

    // --- NEW: Calculate 10,000,000 Score ---
    public int getFinalScore() {
        if (maxHitScore == 0) return 0;
        // Formula adapted from the C++ source: (Current / Max) * 10,000,000
        double ratio = (double) currentHitScore / (double) maxHitScore;
        return (int) (ratio * 10000000.0);
    }

    // --- NEW: Calculate Letter Grade ---
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
