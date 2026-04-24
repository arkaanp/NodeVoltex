package com.nodevoltex.game.patterns;

import com.nodevoltex.game.entities.LaserCursor;

public class FreeState implements CursorState {
    private final float FREE_SPEED = 1.5f;

    @Override
    public void update(LaserCursor cursor, float delta) {
        if (cursor.isMovingLeft) cursor.x -= FREE_SPEED * delta;
        if (cursor.isMovingRight) cursor.x += FREE_SPEED * delta;

        if (cursor.x < 0.0f) cursor.x = 0.0f;
        if (cursor.x > 1.0f) cursor.x = 1.0f;

        // The cursor can ONLY snap back if it's within tolerance AND the player is actively pressing the right key
        if (Math.abs(cursor.x - cursor.targetLaserX) <= 0.05f && cursor.isHoldingCorrectKey) {
            cursor.setState(new LockedState());
            cursor.isMissed = false;
        }
    }
}
