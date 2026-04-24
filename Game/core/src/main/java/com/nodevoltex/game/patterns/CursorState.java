package com.nodevoltex.game.patterns;

import com.nodevoltex.game.entities.LaserCursor;

public interface CursorState {
    void update(LaserCursor cursor, float delta);
}

