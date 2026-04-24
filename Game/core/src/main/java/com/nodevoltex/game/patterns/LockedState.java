package com.nodevoltex.game.patterns;

import com.nodevoltex.game.entities.LaserCursor;

public class LockedState implements CursorState {
    @Override
    public void update(LaserCursor cursor, float delta) {
        cursor.x = cursor.targetLaserX;

        // If it's a diagonal/snap and the player lets go, detach immediately
        if (cursor.requiresInput && !cursor.isHoldingCorrectKey) {
            cursor.setState(new FreeState());
            cursor.isMissed = true;
        }
    }
}
