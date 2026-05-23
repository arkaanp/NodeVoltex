package com.nodevoltex.backend.dto;

public class ScoreSubmitResponse {
    private Double playVolforce;
    private Double newTotalVolforce;
    private Double volforceGained;

    public ScoreSubmitResponse() {
    }

    public ScoreSubmitResponse(Double playVolforce, Double newTotalVolforce, Double volforceGained) {
        this.playVolforce = playVolforce;
        this.newTotalVolforce = newTotalVolforce;
        this.volforceGained = volforceGained;
    }

    public Double getPlayVolforce() {
        return playVolforce;
    }

    public void setPlayVolforce(Double playVolforce) {
        this.playVolforce = playVolforce;
    }

    public Double getNewTotalVolforce() {
        return newTotalVolforce;
    }

    public void setNewTotalVolforce(Double newTotalVolforce) {
        this.newTotalVolforce = newTotalVolforce;
    }

    public Double getVolforceGained() {
        return volforceGained;
    }

    public void setVolforceGained(Double volforceGained) {
        this.volforceGained = volforceGained;
    }
}
