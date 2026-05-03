package com.nodevoltex.game.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.entities.LaserCursor;
import com.nodevoltex.game.patterns.LockedState;

public class LaserManager {

    // NEW: Passed in the ScoreManager
    public void updateCursor(LaserCursor cursor, Array<Beatmap.LaserSequence> laserData, float currentTime, float delta, ScoreManager scoreManager) {
        if (laserData == null) return;

        // Track the state from the previous frame to detect the exact moment a miss happens
        boolean wasMissedBefore = cursor.isMissed;
        boolean isCurrentlyOnLaser = false;

        for (Beatmap.LaserSequence sequence : laserData) {
            if (sequence.nodes.size == 0) continue;

            float firstOffset = sequence.nodes.get(0).offset;
            float lastOffset = sequence.nodes.get(sequence.nodes.size - 1).offset;

            if (currentTime >= firstOffset && currentTime <= lastOffset + 50f) {
                isCurrentlyOnLaser = true;

                if (currentTime - firstOffset < 100f && !cursor.wasAutoSnapped) {
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                    cursor.wasAutoSnapped = true;
                }

                int startIndex = 0;
                for (int i = 0; i < sequence.nodes.size; i++) {
                    if (sequence.nodes.get(i).offset <= currentTime) startIndex = i;
                    else break;
                }

                Beatmap.LaserNode nodeA;
                Beatmap.LaserNode nodeB;

                if (startIndex < sequence.nodes.size - 1) {
                    nodeA = sequence.nodes.get(startIndex);
                    nodeB = sequence.nodes.get(startIndex + 1);
                } else {
                    nodeA = sequence.nodes.get(sequence.nodes.size - 1);
                    nodeB = nodeA;
                }

                float duration = nodeB.offset - nodeA.offset;
                float ratio = (duration <= 0) ? 1.0f : (currentTime - nodeA.offset) / duration;
                float currentLaserX = nodeA.x + ratio * (nodeB.x - nodeA.x);
                float direction = Math.signum(nodeB.x - nodeA.x);

                cursor.targetLaserX = currentLaserX;
                cursor.requiresInput = (direction != 0);

                cursor.pollInputs(direction);
                break;
            }
        }

        // ... (The active laser loop finishes right before this) ...

        if (!isCurrentlyOnLaser) {
            cursor.requiresInput = false;
            cursor.pollInputs(0);
            cursor.wasAutoSnapped = false;
            cursor.comboTimer = 0f;
            cursor.missedTimer = 0f; // Reset leniency timer when no laser is active
            cursor.hasComboBroken = false;

            Beatmap.LaserSequence upcomingLaser = null;
            for (Beatmap.LaserSequence sequence : laserData) {
                if (sequence.nodes.size > 0 && sequence.nodes.get(0).offset > currentTime) {
                    upcomingLaser = sequence;
                    break;
                }
            }

            if (upcomingLaser != null) {
                float timeToNextMs = upcomingLaser.nodes.get(0).offset - currentTime;
                if (timeToNextMs <= 1000f) {
                    cursor.targetLaserX = upcomingLaser.nodes.get(0).x;
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                }
            }
        }

        // Run the State Pattern (Updates X position and handles snapping)
        cursor.update(delta);

        // --- NEW: Leniency Math & Delayed Combo Breaks ---
        if (isCurrentlyOnLaser) {
            if (cursor.isMissed) {
                cursor.comboTimer = 0f;
                // Cursor is detached! Start the leniency timer.
                cursor.missedTimer += delta * 1000f;

                // If 200ms passes and they haven't snapped back, break the combo.
                if (cursor.missedTimer >= 200.0f && !cursor.hasComboBroken) {
                    scoreManager.onMiss();
                    cursor.hasComboBroken = true; // Prevents spamming the miss function
                }
            } else {
                // The cursor is locked! Reset the leniency timers and flags.
                cursor.missedTimer = 0f;
                cursor.hasComboBroken = false;

                // Process continuous Combo Ticks
                cursor.comboTimer += delta * 1000f;
                while (cursor.comboTimer >= 100.0f) {
                    scoreManager.onLaserTick();
                    cursor.comboTimer -= 100.0f;
                }
            }
        }
    }


    public void drawLasers(ShapeRenderer renderer, Array<Beatmap.LaserSequence> laserData, boolean isLeft, float currentTime, float speed, float mult, float trackX, float trackW, float hitY) {
        if (laserData == null) return;
        Color laserColor = isLeft ? new Color(0f, 1f, 1f, 0.1f) : new Color(1f, 0f, 1f, 0.1f);
        renderer.setColor(laserColor);

        // Iterate through the wrappers
        for (Beatmap.LaserSequence sequence : laserData) {
            // Access the internal nodes array
            for (int i = 0; i < sequence.nodes.size - 1; i++) {
                Beatmap.LaserNode nodeA = sequence.nodes.get(i);
                Beatmap.LaserNode nodeB = sequence.nodes.get(i + 1);


                float yA = (nodeA.offset - currentTime) * speed * mult + hitY;
                float yB = (nodeB.offset - currentTime) * speed * mult + hitY;

                if ((yA > 800 && yB > 800) || (yA < -200 && yB < -200)) continue;

                float xA = trackX + (nodeA.x * trackW);
                float xB = trackX + (nodeB.x * trackW);

                renderer.rectLine(xA, yA, xB, yB, 15f);
            }
        }
    }

    public float getWarningAlpha(Array<Beatmap.LaserSequence> laserData, float currentTime) {
        if (laserData == null) return 0f;

        for (Beatmap.LaserSequence sequence : laserData) {
            if (sequence.nodes.size > 0) {
                float startOffset = sequence.nodes.get(0).offset;
                float diffMs = startOffset - currentTime;

                // If the laser hasn't started yet, and is 2 seconds or less away
                if (diffMs > 0 && diffMs <= 2000f) {
                    // Creates a fast arcade-style blinking effect (approx 3 flashes per second)
                    return 0.3f + 0.7f * Math.abs((float)Math.sin(diffMs * 0.01f));
                }
            }
        }
        return 0f; // No upcoming laser in the 2-second window
    }
}
