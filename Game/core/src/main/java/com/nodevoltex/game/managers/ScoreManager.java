package com.nodevoltex.game.managers;

import com.nodevoltex.game.patterns.GameArchitecture;

public class ScoreManager implements GameArchitecture.HitObserver {
    public int combo = 0;
    public String latestJudgment = "";

    private final GameArchitecture.JudgmentStrategy judgmentStrategy;

    public ScoreManager(GameArchitecture.JudgmentStrategy strategy) {
        this.judgmentStrategy = strategy;
    }

    @Override
    public void onHit(float diffMs) {
        latestJudgment = judgmentStrategy.evaluateJudgment(diffMs);
        combo++;
    }

    @Override
    public void onMiss() {
        latestJudgment = "MISS";
        combo = 0; // Combo break
    }
}
