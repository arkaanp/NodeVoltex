package com.nodevoltex.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Pool;

public class Note implements Pool.Poolable {
    public int lane;
    public float startTime;
    public float endTime;
    public boolean isHold;
    public boolean isActive;

    public float y;

    public Note() {
        isActive = false;
    }

    public void init(int lane, float startTime, float endTime, boolean isHold) {
        this.lane = lane;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isHold = isHold;
        this.isActive = true;
    }

    // The rendering logic moved directly into the entity
    public void updateAndDraw(ShapeRenderer shapeRenderer, float currentAudioTime, float speed, float mult, float trackX, float laneWidth, float hitLineY) {
        if (!isActive) return;

        this.y = (this.startTime - currentAudioTime) * speed * mult + hitLineY;

        float noteWidth = laneWidth * 0.8f;
        float padding = (laneWidth - noteWidth) / 2f;
        float xPosition = trackX + ((this.lane - 1) * laneWidth) + padding;

        if (this.y > -100 && this.y < 800) {
            shapeRenderer.setColor(this.isHold ? Color.YELLOW : Color.WHITE);
            // If it's a hold note, we'll draw it taller based on duration later, for now just standard height
            float height = this.isHold ? (this.endTime - this.startTime) * speed * mult : 20f;
            shapeRenderer.rect(xPosition, this.y, noteWidth, height);
        }
    }

    @Override
    public void reset() {
        this.isActive = false;
        this.lane = 0;
        this.startTime = 0;
        this.endTime = 0;
        this.isHold = false;
        this.y = -1000;
    }
}
