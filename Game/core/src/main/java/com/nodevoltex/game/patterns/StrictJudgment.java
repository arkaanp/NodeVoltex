package com.nodevoltex.game.patterns;

public class StrictJudgment {

    public int x = 8; // Default timing difficulty variable

    public JudgmentResult evaluateJudgment(float diffMs, String type) {
        // Releases get exactly double the leniency window
        float mult = type.equals("RELEASE") ? 2.0f : 1.0f;

        // Apply your formulas: e.g., 64 - 3(8) = 40ms
        float sCritWindow = 16.0f * mult;
        float critWindow = (64.0f - (3.0f * x)) * mult;
        float nearWindow = (97.0f - (3.0f * x)) * mult;
        float midWindow = (127.0f - (3.0f * x)) * mult;
        float farWindow = (151.0f - (3.0f * x)) * mult;

        float absDiff = Math.abs(diffMs);
        String timing = "";

        // We only assign Early/Late if it wasn't a perfect S-Critical
        if (absDiff > sCritWindow) {
            timing = (diffMs < 0) ? "EARLY" : "LATE";
        }

        if (absDiff <= sCritWindow) return new JudgmentResult("S-CRITICAL", timing);
        if (absDiff <= critWindow)  return new JudgmentResult("CRITICAL", timing);
        if (absDiff <= nearWindow)  return new JudgmentResult("NEAR", timing);
        if (absDiff <= midWindow)   return new JudgmentResult("MID", timing);
        if (absDiff <= farWindow)   return new JudgmentResult("FAR", timing);

        // Anything outside the FAR window is a Miss
        return new JudgmentResult("MISS", timing);
    }

    // A clean helper class to return both the tier and the timing status together
    public static class JudgmentResult {
        public String tier;
        public String timing;

        public JudgmentResult(String tier, String timing) {
            this.tier = tier;
            this.timing = timing;
        }
    }
}
