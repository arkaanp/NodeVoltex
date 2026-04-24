package com.nodevoltex.game.patterns;

public class GameArchitecture {

    // Decouples the input detection from the score counting
    public interface HitObserver {
        void onHit(float diffMs);
        void onMiss();
    }

    // Encapsulates the math for timing windows so we can swap it later (eg. Easy vs Hard mode)
    public interface JudgmentStrategy {
        String evaluateJudgment(float diffMs);
    }

    public static class StrictJudgment implements JudgmentStrategy {
        @Override
        public String evaluateJudgment(float diffMs) {
            if (diffMs <= 20.8f) return "S-CRITICAL";
            if (diffMs <= 41.6f) return "CRITICAL";
            if (diffMs <= 150.0f) return "NEAR";
            return "MISS";
        }
    }
}
