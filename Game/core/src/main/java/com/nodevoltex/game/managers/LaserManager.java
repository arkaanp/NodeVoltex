package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.entities.LaserCursor;
import com.nodevoltex.game.patterns.LockedState;

public class LaserManager {

    // --- HELPER 1: Detect if a tick is the exact end of a Slam (< 100ms) ---
    // Now ONLY applies to the absolute final 2 nodes of the entire sequence
    private boolean isTickASlamEnd(Beatmap.LaserSequence seq, float tickTime) {
        // A sequence must have at least 2 nodes to have a distance
        if (seq.nodes.size < 2) return false;

        // Grab ONLY the absolute last two nodes in the entire array
        Beatmap.LaserNode secondToLast = seq.nodes.get(seq.nodes.size - 2);
        Beatmap.LaserNode absoluteLast = seq.nodes.get(seq.nodes.size - 1);

        // If the current tick happens exactly at the final node...
        if (Math.abs(absoluteLast.offset - tickTime) < 1.0f) {
            // ...AND the time between the last two nodes is < 100ms
            if (absoluteLast.offset - secondToLast.offset <= 100f) {
                return true;
            }
        }

        return false;
    }

    // --- HELPER 2: Mathematically calculate where the laser visually is at any given ms ---
    private float calculateLaserPositionAtTime(Beatmap.LaserSequence sequence, float time) {
        if (sequence.nodes.size == 0) return 0f;
        if (time <= sequence.nodes.get(0).offset) return sequence.nodes.get(0).x;
        if (time >= sequence.nodes.get(sequence.nodes.size - 1).offset) return sequence.nodes.get(sequence.nodes.size - 1).x;

        for (int i = 0; i < sequence.nodes.size - 1; i++) {
            Beatmap.LaserNode a = sequence.nodes.get(i);
            Beatmap.LaserNode b = sequence.nodes.get(i + 1);
            if (time >= a.offset && time <= b.offset) {
                float duration = b.offset - a.offset;
                if (duration <= 0) return b.x; // Instant Slam
                float ratio = (time - a.offset) / duration;
                return a.x + ratio * (b.x - a.x);
            }
        }
        return sequence.nodes.get(sequence.nodes.size - 1).x;
    }

    public void updateCursor(LaserCursor cursor, Array<Beatmap.LaserSequence> laserData, float currentTime, float delta, ScoreManager scoreManager) {
        if (laserData == null) return;
        boolean isCurrentlyOnLaser = false;

        for (Beatmap.LaserSequence sequence : laserData) {
            if (sequence.nodes.size == 0) continue;

            float firstOffset = sequence.nodes.get(0).offset;
            float lastOffset = sequence.nodes.get(sequence.nodes.size - 1).offset;

            // --- PART 1: VISUALS & STATE UPDATES ---
            if (currentTime >= firstOffset && currentTime <= lastOffset + 50f) {
                isCurrentlyOnLaser = true;

                if (currentTime - firstOffset < 100f && !cursor.wasAutoSnapped) {
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                    cursor.wasAutoSnapped = true;
                }

                // Setup the State Pattern targets
                float currentLaserX = calculateLaserPositionAtTime(sequence, currentTime);

                // Get direction for input checking
                float direction = 0;
                for (int i = 0; i < sequence.nodes.size - 1; i++) {
                    if (sequence.nodes.get(i).offset <= currentTime && sequence.nodes.get(i+1).offset > currentTime) {
                        direction = Math.signum(sequence.nodes.get(i+1).x - sequence.nodes.get(i).x);
                        break;
                    }
                }

                cursor.targetLaserX = currentLaserX;
                cursor.requiresInput = (direction != 0);
                cursor.pollInputs(direction);
            }

            // --- PART 2: THE PRE-BAKED TICK ENGINE (SCORING) ---
            if (sequence.tickTimes != null) {
                while (sequence.nextTickIndex < sequence.tickTimes.size) {
                    float expectedTickTime = sequence.tickTimes.get(sequence.nextTickIndex);

                    if (currentTime >= expectedTickTime) {
                        boolean isSlamEnd = isTickASlamEnd(sequence, expectedTickTime);
                        float targetLaserPos = calculateLaserPositionAtTime(sequence, expectedTickTime);

                        boolean isHit = false;

                        if (isSlamEnd) {
                            // --- SLAM LOGIC: Check Keyboard Input Intent ---
                            // 1. Find where this slam started to determine direction
                            float slamStartX = targetLaserPos;
                            for (int i = 1; i < sequence.nodes.size; i++) {
                                if (Math.abs(sequence.nodes.get(i).offset - expectedTickTime) < 1.0f) {
                                    slamStartX = sequence.nodes.get(i - 1).x;
                                    break;
                                }
                            }

                            float slamDirection = Math.signum(targetLaserPos - slamStartX);
                            boolean correctFlick = false;

                            // 2. Check if the player is holding the correct direction key
                            if (slamDirection > 0 && cursor.isMovingRight) correctFlick = true;
                            if (slamDirection < 0 && cursor.isMovingLeft) correctFlick = true;

                            // 3. Hit condition: Correct input OR they are physically already there
                            if (correctFlick || Math.abs(cursor.x - targetLaserPos) <= 0.15f) {
                                isHit = true;
                                cursor.x = targetLaserPos; // Force-snap the visual cursor so it doesn't lag!
                            }
                        } else {
                            // --- CONTINUOUS LOGIC: Check Physical Proximity ---
                            // Standard 20% leniency window for normal lasers
                            if (!cursor.isMissed && Math.abs(cursor.x - targetLaserPos) <= 0.20f) {
                                isHit = true;
                            }
                        }

                        // Process the Judgment
                        if (isHit) {
                            scoreManager.onLaserTick();
                            cursor.isMissed = false;
                            if (cursor.wasAutoSnapped) cursor.setState(new LockedState());
                        } else {
                            scoreManager.onMiss();
                            cursor.isMissed = true;
                        }

                        sequence.nextTickIndex++;
                    } else {
                        break;
                    }
                }
            }
        }

        // --- PART 3: CLEANUP & UPCOMING LASERS ---
        if (!isCurrentlyOnLaser) {
            cursor.requiresInput = false;
            cursor.pollInputs(0);
            cursor.wasAutoSnapped = false;
            cursor.missedTimer = 0f;
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

        // Run the State Pattern (Updates X position physically based on inputs)
        cursor.update(delta);
    }

    // ... (Keep your drawLasers and getWarningAlpha methods exactly the same) ...
    public void drawLasers(ShapeRenderer renderer, Array<Beatmap.LaserSequence> laserData, boolean isLeft, LaserCursor cursor, float currentTime, float speed, float mult, float trackX, float trackW, float hitY) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (laserData == null) return;

        float alpha = cursor.isMissed ? 0.15f : 0.5f;

        Color laserColor = isLeft ? new Color(0f, 1f, 1f, alpha) : new Color(1f, 0f, 1f, alpha);
        renderer.setColor(laserColor);

        for (Beatmap.LaserSequence sequence : laserData) {
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
                if (diffMs > 0 && diffMs <= 2000f) {
                    return 0.3f + 0.7f * Math.abs((float)Math.sin(diffMs * 0.01f));
                }
            }
        }
        return 0f;
    }
}
