package com.nodevoltex.game.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.entities.LaserCursor;
import com.nodevoltex.game.patterns.LockedState;

public class LaserManager {

    public void updateCursor(LaserCursor cursor, Array<Beatmap.LaserSequence> laserData, float currentTime, float delta) {
        if (laserData == null) return;
        boolean isCurrentlyOnLaser = false;

        for (Beatmap.LaserSequence sequence : laserData) {
            if (sequence.nodes.size == 0) continue;

            float firstOffset = sequence.nodes.get(0).offset;
            float lastOffset = sequence.nodes.get(sequence.nodes.size - 1).offset;

            // Check if we are currently inside this laser sequence (with a tiny 50ms buffer for the final slam)
            if (currentTime >= firstOffset && currentTime <= lastOffset + 50f) {
                isCurrentlyOnLaser = true;

                // Auto-Snap at the very beginning of a new laser
                if (currentTime - firstOffset < 100f && !cursor.wasAutoSnapped) {
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                    cursor.wasAutoSnapped = true;
                }

                // Find the active segment (Handles 0ms horizontal slams)
                int startIndex = 0;
                for (int i = 0; i < sequence.nodes.size; i++) {
                    if (sequence.nodes.get(i).offset <= currentTime) {
                        startIndex = i;
                    } else {
                        break;
                    }
                }

                Beatmap.LaserNode nodeA;
                Beatmap.LaserNode nodeB;

                if (startIndex < sequence.nodes.size - 1) {
                    nodeA = sequence.nodes.get(startIndex);
                    nodeB = sequence.nodes.get(startIndex + 1);
                } else {
                    // At the very end of the laser
                    nodeA = sequence.nodes.get(sequence.nodes.size - 1);
                    nodeB = nodeA;
                }

                // Interpolation Math
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

        // If the laser is completely finished or hasn't started
        if (!isCurrentlyOnLaser) {
            cursor.requiresInput = false;
            cursor.pollInputs(0);
            cursor.wasAutoSnapped = false; // Reset so the next laser auto-snaps again
        }

        cursor.update(delta);
    }

    public void drawLasers(ShapeRenderer renderer, Array<Beatmap.LaserSequence> laserData, boolean isLeft, float currentTime, float speed, float mult, float trackX, float trackW, float hitY) {
        if (laserData == null) return;
        renderer.setColor(isLeft ? Color.CYAN : Color.MAGENTA);

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
}
