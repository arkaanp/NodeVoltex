package com.nodevoltex.game.patterns;

import com.nodevoltex.game.entities.LaserCursor;

public class FreeState implements CursorState {

    // --- TUNABLE ACCELERATION SETTINGS ---
    // 1.0f means it takes exactly 1 second to cross the entire track.
    private final float BASE_SPEED = 1.25f;       // Starting speed (fine control)
    private final float MAX_SPEED = 8.0f;        // Top speed (rapid whipping)

    // Time in seconds to wait before the acceleration kicks in
    private final float ACCEL_DELAY = 0.05f;      // 50ms delay

    // How quickly it reaches MAX_SPEED once the delay finishes
    private final float ACCEL_RATE = 16.0f;

    // --- INTERNAL STATE TRACKERS ---
    private float currentSpeed = BASE_SPEED;
    private float holdTimer = 0f;

    @Override
    public void update(LaserCursor cursor, float delta) {
        boolean isMoving = cursor.isMovingLeft || cursor.isMovingRight;

        // 1. Handle Acceleration Math
        if (isMoving) {
            holdTimer += delta;

            // If they have held the key longer than the delay, start speeding up!
            if (holdTimer >= ACCEL_DELAY) {
                currentSpeed += ACCEL_RATE * delta;

                // Clamp the speed so it doesn't go infinitely fast
                if (currentSpeed > MAX_SPEED) {
                    currentSpeed = MAX_SPEED;
                }
            }
        } else {
            // 2. Reset the speed the millisecond they let go of the keys
            holdTimer = 0f;
            currentSpeed = BASE_SPEED;
        }

        // 3. Apply the Movement
        if (cursor.isMovingLeft) {
            cursor.x -= currentSpeed * delta;
        }
        if (cursor.isMovingRight) {
            cursor.x += currentSpeed * delta;
        }

        // 4. Hard clamp to the track edges (0.0 = Far Left, 1.0 = Far Right)
        if (cursor.x < 0.0f) cursor.x = 0.0f;
        if (cursor.x > 1.0f) cursor.x = 1.0f;
    }
}
