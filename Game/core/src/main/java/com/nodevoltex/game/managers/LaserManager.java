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

    // --- HELPER 1: Finds the Slam and returns its exact Index (< 50ms threshold) ---
    private int getSlamEndNodeIndex(Beatmap.LaserSequence seq, float tickTime) {
        if (seq.nodes.size < 2) return -1;

        for (int i = 1; i < seq.nodes.size; i++) {
            Beatmap.LaserNode prev = seq.nodes.get(i - 1);
            Beatmap.LaserNode curr = seq.nodes.get(i);

            // If this tick aligns with this node, AND it's < 50ms, AND it moved horizontally
            if (Math.abs(curr.offset - tickTime) < 1.0f) {
                if (curr.offset - prev.offset < 50f && Math.abs(curr.x - prev.x) > 0.01f) {
                    return i;
                }
            }
        }
        return -1;
    }

    // --- HELPER 2: The Curve-Prevention Math (Look-Ahead / Look-Behind) ---
    private boolean shouldPlayHitsound(Beatmap.LaserSequence seq, int slamNodeIndex) {
        if (slamNodeIndex < 1) return false;

        Beatmap.LaserNode curr = seq.nodes.get(slamNodeIndex);
        Beatmap.LaserNode prev = seq.nodes.get(slamNodeIndex - 1);
        float currentDirection = Math.signum(curr.x - prev.x);

        // 1. LOOK-BEHIND: The segment BEFORE this must be vertical, nothing, OR in a DIFFERENT direction.
        if (slamNodeIndex >= 2) {
            Beatmap.LaserNode prevPrev = seq.nodes.get(slamNodeIndex - 2);
            float prevDirection = Math.signum(prev.x - prevPrev.x);

            // If the previous segment was moving in the EXACT SAME direction, it's a smooth curve!
            // (e.g., Left -> Left, or Right -> Right). Do NOT play the sound.
            if (prevDirection == currentDirection) {
                return false;
            }
        }

        // 2. LOOK-AHEAD: The segment AFTER this must not continue in the same direction.
        if (slamNodeIndex < seq.nodes.size - 1) {
            Beatmap.LaserNode next = seq.nodes.get(slamNodeIndex + 1);
            float nextDirection = Math.signum(next.x - curr.x);

            // If it keeps going the same way (e.g. 0.5 -> 0.6 -> 0.7), it's a curve!
            if (nextDirection == currentDirection) {
                return false;
            }
        }

        // If it passed all checks, it is a true, isolated flick or a sharp zig-zag!
        return true;
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

    public void updateCursor(LaserCursor cursor, Array<Beatmap.LaserSequence> laserData, float currentTime, float delta, ScoreManager scoreManager,
                             com.badlogic.gdx.audio.Sound slamSound) {
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
                    // --- THE FIX: Anchor the cursor perfectly to the first node! ---
                    cursor.x = sequence.nodes.get(0).x;
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

                    // If the song timeline has passed this baked tick's timestamp
                    // If the song timeline has passed this baked tick's timestamp
                    if (currentTime >= expectedTickTime) {
                        boolean isStartTick = (sequence.nextTickIndex == 0);

                        // NEW: Grab the exact index of the slam to speed up math
                        int slamNodeIndex = getSlamEndNodeIndex(sequence, expectedTickTime);
                        boolean isSlamEnd = (slamNodeIndex != -1);

                        float targetLaserPos = calculateLaserPositionAtTime(sequence, expectedTickTime);
                        boolean isHit = false;

                        if (isStartTick) {
                            // --- START TICK LOGIC ---
                            if (cursor.wasAutoSnapped || Math.abs(cursor.x - targetLaserPos) <= 0.40f) {
                                isHit = true;
                                cursor.x = targetLaserPos;
                            }
                        } else if (isSlamEnd) {
                            // --- SLAM LOGIC ---
                            // We don't need a for-loop anymore! We know the exact start index!
                            float slamStartX = sequence.nodes.get(slamNodeIndex - 1).x;
                            float slamDirection = Math.signum(targetLaserPos - slamStartX);
                            boolean correctFlick = false;

                            // Check Intent
                            if (slamDirection > 0 && cursor.isMovingRight) correctFlick = true;
                            if (slamDirection < 0 && cursor.isMovingLeft) correctFlick = true;

                            // Hit if correct flick, OR if they were just safely parked there
                            if (correctFlick || Math.abs(cursor.x - targetLaserPos) <= 0.30f) {
                                isHit = true;
                                cursor.x = targetLaserPos;

                                // --- HITSOUND LOGIC ---
                                // ONLY play if they actively pressed the correct key AND it's not a curve
                                if (correctFlick && slamSound != null) {
                                    if (shouldPlayHitsound(sequence, slamNodeIndex)) {
                                        slamSound.play(0.35f);
                                    }
                                }
                            }
                        } else {
                            // --- CONTINUOUS LOGIC ---
                            if (Math.abs(cursor.x - targetLaserPos) <= 0.35f) {
                                isHit = true;
                            }
                        }

                        // Process the Judgment
                        if (isHit) {
                            scoreManager.onLaserTick();
                            cursor.isMissed = false;
                            cursor.setState(new LockedState());
                        } else {
                            scoreManager.onMiss();
                            cursor.isMissed = true;
                            cursor.setState(new com.nodevoltex.game.patterns.FreeState());
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
