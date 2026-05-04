package com.nodevoltex.game.patterns;

public class StrictJudgment implements GameArchitecture.JudgmentStrategy {

    // Timing windows in milliseconds (Standard arcade timing)
    private static final float S_CRITICAL_WINDOW = 20.0f; // ±20ms
    private static final float CRITICAL_WINDOW = 40.0f;   // ±40ms
    private static final float NEAR_WINDOW = 80.0f;       // ±80ms

    @Override
    public String evaluateJudgment(float diffMs) {
        // Convert the difference to an absolute value (doesn't matter if early or late)
        float absDiff = Math.abs(diffMs);

        if (absDiff <= S_CRITICAL_WINDOW) {
            return "S-CRITICAL";
        } else if (absDiff <= CRITICAL_WINDOW) {
            return "CRITICAL";
        } else if (absDiff <= NEAR_WINDOW) {
            return "NEAR";
        } else {
            return "MISS";
        }
    }
}
