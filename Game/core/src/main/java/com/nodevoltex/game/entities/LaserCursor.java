package com.nodevoltex.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.nodevoltex.game.patterns.CursorState;
import com.nodevoltex.game.patterns.FreeState;

public class LaserCursor {
    public boolean isLeftLaser;
    private CursorState currentState;

    // Position (0.0f = Far Left of track, 1.0f = Far Right)
    public float x = 0.0f;

    // Data fed by the game loop every frame
    public float targetLaserX = 0.0f;
    public boolean requiresInput = false;

    // Input Flags
    public boolean isMovingLeft = false;
    public boolean isMovingRight = false;
    public boolean isHoldingCorrectKey = false;
    public boolean isMissed = false;
    public boolean wasAutoSnapped = false;

    // Assigned Keyboard Keys (LibGDX Input.Keys integers)
    private final int keyLeft;
    private final int keyRight;

    public LaserCursor(boolean isLeftLaser, int keyLeft, int keyRight) {
        this.isLeftLaser = isLeftLaser;
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
        this.currentState = new FreeState(); // Starts wild until a laser appears
    }

    public void setState(CursorState state) {
        this.currentState = state;
    }

    // Called by PlayScreen before updating the state
    public void pollInputs(float expectedDirection) {
        isMovingLeft = Gdx.input.isKeyPressed(keyLeft);
        isMovingRight = Gdx.input.isKeyPressed(keyRight);

        // Determine if player is holding the correct direction relative to the laser's slope
        if (expectedDirection < 0) {
            isHoldingCorrectKey = isMovingLeft; // Laser moving left, player must hold left
        } else if (expectedDirection > 0) {
            isHoldingCorrectKey = isMovingRight; // Laser moving right, player must hold right
        } else {
            // Straight vertical line: ignore input
            isHoldingCorrectKey = true;
        }
    }

    public void update(float delta) {
        currentState.update(this, delta);
    }

    public void draw(ShapeRenderer renderer, float trackX, float trackWidth, float hitLineY) {
        // Convert the 0.0-1.0 ratio to actual screen pixels
        float screenX = trackX + (this.x * trackWidth);

        // Color changes based on state (Missed = Gray, Left = Cyan, Right = Pink)
        if (isMissed) renderer.setColor(Color.DARK_GRAY);
        else renderer.setColor(isLeftLaser ? Color.CYAN : Color.MAGENTA);

        // Draw the cursor on the judgment line
        renderer.rect(screenX - 15, hitLineY - 10, 30, 20);
    }
}
