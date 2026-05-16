package com.nodevoltex.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.nodevoltex.game.patterns.CursorState;
import com.nodevoltex.game.patterns.FreeState;
import com.nodevoltex.game.managers.InputController;
import com.nodevoltex.game.data.ReplayData;

public class LaserCursor {
    public boolean isLeftLaser;
    private CursorState currentState;

    public float x = 0.0f;
    public float targetLaserX = 0.0f;
    public boolean requiresInput = false;

    public boolean isMovingLeft = false;
    public boolean isMovingRight = false;
    public boolean isHoldingCorrectKey = false;
    public boolean isMissed = false;
    public boolean wasAutoSnapped = false;
    public float comboTimer = 0f;

    public float missedTimer = 0f;
    public boolean hasComboBroken = false;

    // --- Alternate Key Variables ---
    private final int keyLeftPri;
    private final int keyRightPri;
    private final int keyLeftAlt;
    private final int keyRightAlt;

    public LaserCursor(boolean isLeftLaser, int keyLeftPri, int keyRightPri, int keyLeftAlt, int keyRightAlt) {
        this.isLeftLaser = isLeftLaser;
        this.keyLeftPri = keyLeftPri;
        this.keyRightPri = keyRightPri;
        this.keyLeftAlt = keyLeftAlt;
        this.keyRightAlt = keyRightAlt;
        this.currentState = new FreeState();
    }

    public void setState(CursorState state) {
        this.currentState = state;
    }

    public void pollInputs(float expectedDirection, InputController inputController, float timeMs) {
        boolean currentLeft;
        boolean currentRight;

        String labelL = isLeftLaser ? "VOL_L_L" : "VOL_R_L";
        String labelR = isLeftLaser ? "VOL_L_R" : "VOL_R_R";

        // PLAYBACK: Read from Ghost Keyboard
        if (inputController != null && inputController.isReplayPlayback) {
            currentLeft = inputController.virtualKeyboard.getOrDefault(labelL, false);
            currentRight = inputController.virtualKeyboard.getOrDefault(labelR, false);
        } else {
            // NORMAL: Read Physical Keys
            // --- checks if EITHER the Primary or Alternate key is pressed ---
            currentLeft = Gdx.input.isKeyPressed(keyLeftPri) || Gdx.input.isKeyPressed(keyLeftAlt);
            currentRight = Gdx.input.isKeyPressed(keyRightPri) || Gdx.input.isKeyPressed(keyRightAlt);
        }

        // RECORDING: Save state changes
        if (inputController != null && inputController.isRecording) {
            if (currentLeft && !isMovingLeft) inputController.currentReplay.events.add(new ReplayData.InputEvent(timeMs, labelL, true));
            else if (!currentLeft && isMovingLeft) inputController.currentReplay.events.add(new ReplayData.InputEvent(timeMs, labelL, false));

            if (currentRight && !isMovingRight) inputController.currentReplay.events.add(new ReplayData.InputEvent(timeMs, labelR, true));
            else if (!currentRight && isMovingRight) inputController.currentReplay.events.add(new ReplayData.InputEvent(timeMs, labelR, false));
        }

        isMovingLeft = currentLeft;
        isMovingRight = currentRight;

        if (expectedDirection < 0) {
            isHoldingCorrectKey = isMovingLeft;
        } else if (expectedDirection > 0) {
            isHoldingCorrectKey = isMovingRight;
        } else {
            isHoldingCorrectKey = true;
        }
    }

    public void update(float delta) {
        currentState.update(this, delta);
    }

    public void draw(ShapeRenderer renderer, float trackX, float trackWidth, float hitLineY) {
        float screenX = trackX + (this.x * trackWidth);

        // --- Keep base color, lower opacity to 30% if missed ---
        float r = isLeftLaser ? 0f : 1f;  // Cyan: 0, Magenta: 1
        float g = isLeftLaser ? 1f : 0f;  // Cyan: 1, Magenta: 0
        float b = 1f;                     // Both use max Blue
        float a = isMissed ? 0.3f : 1.0f; // Drop opacity instead of turning gray

        renderer.setColor(r, g, b, a);

        // --- Draw an upward arrow/triangle ---
        float peakX = screenX;
        float peakY = hitLineY + 12f;  // Top point of the arrow

        float leftX = screenX - 18f;
        float leftY = hitLineY - 8f;   // Bottom left point

        float rightX = screenX + 18f;
        float rightY = hitLineY - 8f;  // Bottom right point

        renderer.triangle(leftX, leftY, peakX, peakY, rightX, rightY);
    }
}
