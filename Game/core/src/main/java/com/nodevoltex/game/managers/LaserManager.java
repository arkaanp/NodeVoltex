package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.entities.LaserCursor;
import com.nodevoltex.game.patterns.LockedState;

import java.util.HashMap;
import java.util.Map;

public class LaserManager {

    public boolean isAutoPlay = false;
    public boolean isNoLaser = false;

    public float graceWindowMs = 40f;

    private Map<LaserCursor, Float> resolvedSlamTimes = new HashMap<>();

    private int getSlamEndNodeIndex(Beatmap.LaserSequence seq, float tickTime) {
        if (seq.nodes.size < 2) return -1;
        for (int i = 1; i < seq.nodes.size; i++) {
            Beatmap.LaserNode prev = seq.nodes.get(i - 1);
            Beatmap.LaserNode curr = seq.nodes.get(i);
            if (Math.abs(curr.offset - tickTime) < 1.0f) {
                if (curr.offset - prev.offset <= 50f && Math.abs(curr.x - prev.x) > 0.05f) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean shouldPlayHitsound(Beatmap.LaserSequence seq, int slamNodeIndex) {
        if (slamNodeIndex < 1) return false;
        Beatmap.LaserNode curr = seq.nodes.get(slamNodeIndex);
        Beatmap.LaserNode prev = seq.nodes.get(slamNodeIndex - 1);
        float currentDirection = Math.signum(curr.x - prev.x);

        if (slamNodeIndex >= 2) {
            Beatmap.LaserNode prevPrev = seq.nodes.get(slamNodeIndex - 2);
            float prevDirection = Math.signum(prev.x - prevPrev.x);
            if (prevDirection == currentDirection) return false;
        }

        if (slamNodeIndex < seq.nodes.size - 1) {
            Beatmap.LaserNode next = seq.nodes.get(slamNodeIndex + 1);
            float nextDirection = Math.signum(next.x - curr.x);
            if (nextDirection == currentDirection) return false;
        }
        return true;
    }

    private float calculateLaserPositionAtTime(Beatmap.LaserSequence sequence, float time) {
        if (sequence.nodes.size == 0) return 0f;
        if (time <= sequence.nodes.get(0).offset) return sequence.nodes.get(0).x;
        if (time >= sequence.nodes.get(sequence.nodes.size - 1).offset) return sequence.nodes.get(sequence.nodes.size - 1).x;

        for (int i = 0; i < sequence.nodes.size - 1; i++) {
            Beatmap.LaserNode a = sequence.nodes.get(i);
            Beatmap.LaserNode b = sequence.nodes.get(i + 1);
            if (time >= a.offset && time <= b.offset) {
                float duration = b.offset - a.offset;
                if (duration <= 0) return b.x;
                float ratio = (time - a.offset) / duration;
                return a.x + ratio * (b.x - a.x);
            }
        }
        return sequence.nodes.get(sequence.nodes.size - 1).x;
    }

    public void updateCursor(LaserCursor cursor, Array<Beatmap.LaserSequence> laserData, float currentTime, float delta,
                             ScoreManager scoreManager, com.badlogic.gdx.audio.Sound slamSound, InputController inputController) {
        if (laserData == null) return;
        boolean isCurrentlyOnLaser = false;

        for (Beatmap.LaserSequence sequence : laserData) {
            if (sequence.nodes.size == 0) continue;

            float firstOffset = sequence.nodes.get(0).offset;
            float lastOffset = sequence.nodes.get(sequence.nodes.size - 1).offset;

            if (currentTime >= firstOffset - graceWindowMs && currentTime <= lastOffset + 50f) {
                isCurrentlyOnLaser = true;

                if (currentTime - firstOffset < 100f && !cursor.wasAutoSnapped) {
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                    cursor.wasAutoSnapped = true;
                    cursor.x = sequence.nodes.get(0).x;
                }

                float currentLaserX = calculateLaserPositionAtTime(sequence, currentTime);

                int currentSegIdx = -1;
                float direction = 0;
                for (int i = 0; i < sequence.nodes.size - 1; i++) {
                    if (sequence.nodes.get(i).offset <= currentTime && sequence.nodes.get(i+1).offset > currentTime) {
                        direction = Math.signum(sequence.nodes.get(i+1).x - sequence.nodes.get(i).x);
                        currentSegIdx = i;
                        break;
                    }
                }

                cursor.requiresInput = (direction != 0);
                cursor.pollInputs(direction, inputController, currentTime);

                boolean slamResolvedThisFrame = false;
                boolean isCoastingOnResolvedSlam = false;

                for (int i = 0; i < sequence.nodes.size - 1; i++) {
                    Beatmap.LaserNode nodeA = sequence.nodes.get(i);
                    Beatmap.LaserNode nodeB = sequence.nodes.get(i+1);

                    if (nodeB.offset - nodeA.offset <= 50f && Math.abs(nodeB.x - nodeA.x) > 0.05f) {
                        if (currentTime >= nodeA.offset - graceWindowMs && currentTime <= nodeB.offset + graceWindowMs) {

                            float lastResolved = resolvedSlamTimes.getOrDefault(cursor, -1f);
                            if (lastResolved >= nodeA.offset) {
                                currentLaserX = nodeB.x;
                                isCoastingOnResolvedSlam = true;
                                continue;
                            }

                            if (slamResolvedThisFrame) continue;

                            float expectedDir = Math.signum(nodeB.x - nodeA.x);
                            boolean flicked = false;

                            if (isAutoPlay || isNoLaser) {
                                if (currentTime >= nodeA.offset) flicked = true;
                            } else {
                                if (expectedDir > 0 && cursor.isMovingRight) flicked = true;
                                if (expectedDir < 0 && cursor.isMovingLeft) flicked = true;
                            }

                            if (flicked) {
                                cursor.x = nodeB.x;
                                currentLaserX = nodeB.x;
                                resolvedSlamTimes.put(cursor, nodeA.offset);
                                slamResolvedThisFrame = true;
                                isCoastingOnResolvedSlam = true;

                                if (slamSound != null && shouldPlayHitsound(sequence, i + 1)) {
                                    float vol = SettingsManager.getMasterVolume() * SettingsManager.getEffectVolume();
                                    slamSound.play(vol);
                                }
                            }
                        }
                    }
                }

                if (currentTime < firstOffset) {
                    currentLaserX = sequence.nodes.get(0).x;
                }

                cursor.targetLaserX = currentLaserX;

                if (isAutoPlay || isNoLaser) {
                    cursor.x = currentLaserX;
                    cursor.isHoldingCorrectKey = true;
                    if (direction < 0) { cursor.isMovingLeft = true; cursor.isMovingRight = false; }
                    else if (direction > 0) { cursor.isMovingRight = true; cursor.isMovingLeft = false; }
                    else { cursor.isMovingLeft = false; cursor.isMovingRight = false; }
                }

                // --- PURE INTENT ENGINE ---
                boolean intentMatched = false;

                if (isAutoPlay || isNoLaser || isCoastingOnResolvedSlam) {
                    intentMatched = true;
                } else {
                    if (direction != 0) {
                        // 1. SLANTS: Simply check if they are holding the correct direction.
                        intentMatched = cursor.isHoldingCorrectKey;
                    } else {
                        // 2. VERTICAL LINES
                        if (!cursor.isMissed) {
                            // If they are locked on a straight line, they can let go entirely and coast.
                            intentMatched = true;
                        } else {
                            // If they are detached (Gray Cursor), they MUST spin towards the laser to recover.
                            boolean turningRightTowards = (cursor.x < currentLaserX - 0.02f) && cursor.isMovingRight;
                            boolean turningLeftTowards = (cursor.x > currentLaserX + 0.02f) && cursor.isMovingLeft;

                            // If they are physically sitting right on top of it, lock them back in.
                            boolean alreadyOnIt = Math.abs(cursor.x - currentLaserX) <= 0.05f;

                            if (turningRightTowards || turningLeftTowards || alreadyOnIt) {
                                intentMatched = true;
                            }
                        }
                    }
                }

                boolean wrongInput = !intentMatched;

                // --- EARLY/LATE LENIENCY SCANNER (Only runs if current intent failed) ---
                if (wrongInput && currentSegIdx != -1) {
                    // 1. EARLY TURN LENIENCY (Lookahead)
                    boolean foundEarlyForgiveness = false;
                    Beatmap.LaserNode nodeB = sequence.nodes.get(currentSegIdx + 1);

                    if (nodeB.offset - currentTime <= graceWindowMs) {
                        float futureDir = 0;
                        if (currentSegIdx < sequence.nodes.size - 2) {
                            Beatmap.LaserNode nodeC = sequence.nodes.get(currentSegIdx + 2);
                            futureDir = Math.signum(nodeC.x - nodeB.x);
                        }

                        if (futureDir != direction) {
                            boolean holdingFuture;
                            if (futureDir == 0) {
                                holdingFuture = !cursor.isMovingLeft && !cursor.isMovingRight;
                            } else {
                                holdingFuture = (futureDir < 0 && cursor.isMovingLeft) || (futureDir > 0 && cursor.isMovingRight);
                            }

                            if (holdingFuture) {
                                wrongInput = false;
                                currentLaserX = nodeB.x;
                                foundEarlyForgiveness = true;
                            }
                        }
                    }

                    // 2. LATE TURN LENIENCY (Lookbehind)
                    if (wrongInput && !foundEarlyForgiveness) {
                        Beatmap.LaserNode nodeA = sequence.nodes.get(currentSegIdx);

                        if (currentTime - nodeA.offset <= graceWindowMs) {
                            float pastDir = 0;
                            if (currentSegIdx > 0) {
                                Beatmap.LaserNode nodePrev = sequence.nodes.get(currentSegIdx - 1);
                                pastDir = Math.signum(nodeA.x - nodePrev.x);
                            }

                            if (pastDir != direction) {
                                boolean holdingPast;
                                if (pastDir == 0) {
                                    holdingPast = !cursor.isMovingLeft && !cursor.isMovingRight;
                                } else {
                                    holdingPast = (pastDir < 0 && cursor.isMovingLeft) || (pastDir > 0 && cursor.isMovingRight);
                                }

                                if (holdingPast) {
                                    wrongInput = false;
                                }
                            }
                        }
                    }
                }

                // --- APPLY FINAL STATE ---
                if (wrongInput) {
                    cursor.isMissed = true;
                    cursor.missedTimer += delta * 1000f;
                    cursor.setState(new com.nodevoltex.game.patterns.FreeState());

                    // They stay physically stranded wherever they fell off so they can see their mistake

                    if (cursor.missedTimer >= graceWindowMs && !cursor.hasComboBroken) {
                        scoreManager.onMiss("LASER");
                        cursor.hasComboBroken = true;
                    }
                } else {
                    cursor.isMissed = false;
                    cursor.missedTimer = 0f;
                    cursor.hasComboBroken = false;
                    cursor.setState(new LockedState());

                    // Arcade Rail Glue: Because intent was correct, magnetically teleport them to the exact laser coordinate
                    cursor.x = currentLaserX;
                }
            }

            // --- PART 2: THE PRE-BAKED SCORE DISPENSER ---
            if (sequence.tickTimes != null) {
                while (sequence.nextTickIndex < sequence.tickTimes.size) {
                    float expectedTickTime = sequence.tickTimes.get(sequence.nextTickIndex);

                    if (currentTime >= expectedTickTime) {
                        if (isAutoPlay || isNoLaser) {
                            scoreManager.onLaserTick();
                            sequence.nextTickIndex++;
                        } else {
                            int slamNodeIndex = getSlamEndNodeIndex(sequence, expectedTickTime);
                            boolean isSlamEnd = (slamNodeIndex != -1);

                            if (isSlamEnd) {
                                float slamStartOffset = sequence.nodes.get(slamNodeIndex - 1).offset;
                                float lastResolved = resolvedSlamTimes.getOrDefault(cursor, -1f);

                                if (lastResolved >= slamStartOffset) {
                                    scoreManager.onLaserTick();
                                    sequence.nextTickIndex++;
                                } else {
                                    if (currentTime > slamStartOffset + graceWindowMs) {
                                        if (!cursor.hasComboBroken) {
                                            scoreManager.onMiss("LASER");
                                            cursor.hasComboBroken = true;
                                        }
                                        sequence.nextTickIndex++;
                                    } else {
                                        break;
                                    }
                                }
                            } else {
                                if (!cursor.isMissed) {
                                    scoreManager.onLaserTick();
                                    sequence.nextTickIndex++;
                                } else if (cursor.missedTimer < graceWindowMs) {
                                    scoreManager.onLaserTick();
                                    sequence.nextTickIndex++;
                                } else {
                                    if (!cursor.hasComboBroken) {
                                        scoreManager.onMiss("LASER");
                                        cursor.hasComboBroken = true;
                                    }
                                    sequence.nextTickIndex++;
                                }
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        // --- PART 3: CLEANUP & UPCOMING LASERS ---
        if (!isCurrentlyOnLaser) {
            cursor.requiresInput = false;
            cursor.pollInputs(0, inputController, currentTime);
            cursor.wasAutoSnapped = false;
            cursor.missedTimer = 0f;
            cursor.hasComboBroken = false;
            resolvedSlamTimes.put(cursor, -1f);

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
                    cursor.x = cursor.targetLaserX;
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                }
            }
        }

        cursor.update(delta);
    }

    public void drawLasers(ShapeRenderer renderer, Array<Beatmap.LaserSequence> laserData, boolean isLeft, LaserCursor cursor, float currentTime, float speed, float mult, float trackX, float trackW, float hitY) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (laserData == null) return;

        float alpha = cursor.isMissed ? 0.15f : 0.5f;
        Color laserColor = isLeft ? new Color(0f, 1f, 1f, alpha) : new Color(1f, 0f, 1f, alpha);
        renderer.setColor(laserColor);

        // Pre-allocate arrays to avoid garbage collection stutter
        com.badlogic.gdx.utils.FloatArray sx = new com.badlogic.gdx.utils.FloatArray(64);
        com.badlogic.gdx.utils.FloatArray sy = new com.badlogic.gdx.utils.FloatArray(64);

        for (Beatmap.LaserSequence sequence : laserData) {
            sx.clear();
            sy.clear();

            // 1. Gather all visible points and clip them exactly at the hitline
            for (int i = 0; i < sequence.nodes.size - 1; i++) {
                Beatmap.LaserNode nodeA = sequence.nodes.get(i);
                Beatmap.LaserNode nodeB = sequence.nodes.get(i + 1);

                float yA = (nodeA.offset - currentTime) * speed * mult + hitY;
                float yB = (nodeB.offset - currentTime) * speed * mult + hitY;

                if ((yA > Gdx.graphics.getHeight() + 200f && yB > Gdx.graphics.getHeight() + 200f) || (yA < hitY && yB < hitY)) continue;

                float xA = trackX + (nodeA.x * trackW);
                float xB = trackX + (nodeB.x * trackW);

                if (yA < hitY) {
                    float t = (hitY - yA) / (yB - yA);
                    xA = xA + (t * (xB - xA));
                    yA = hitY;
                }
                if (yB < hitY) {
                    float t = (hitY - yB) / (yA - yB);
                    xB = xB + (t * (xA - xB));
                    yB = hitY;
                }

                // Add points, preventing duplicates to keep the geometry clean
                if (sx.size == 0 || Math.abs(sx.get(sx.size - 1) - xA) > 0.1f || Math.abs(sy.get(sy.size - 1) - yA) > 0.1f) {
                    sx.add(xA);
                    sy.add(yA);
                }
                sx.add(xB);
                sy.add(yB);
            }

            int n = sx.size;
            if (n < 2) continue;

            // 2. The Miter Engine: Calculate interlocking, zero-overlap polygons
            float hw = 15f / 2f; // Half-width of the laser
            float prevLx = 0, prevLy = 0, prevRx = 0, prevRy = 0;

            for (int i = 0; i < n; i++) {
                float dxIn = 0, dyIn = 0, dxOut = 0, dyOut = 0;
                boolean hasIn = false, hasOut = false;

                // Incoming trajectory
                if (i > 0) {
                    dxIn = sx.get(i) - sx.get(i - 1);
                    dyIn = sy.get(i) - sy.get(i - 1);
                    float len = (float) Math.sqrt(dxIn * dxIn + dyIn * dyIn);
                    if (len > 0.001f) { dxIn /= len; dyIn /= len; hasIn = true; }
                }

                // Outgoing trajectory
                if (i < n - 1) {
                    dxOut = sx.get(i + 1) - sx.get(i);
                    dyOut = sy.get(i + 1) - sy.get(i);
                    float len = (float) Math.sqrt(dxOut * dxOut + dyOut * dyOut);
                    if (len > 0.001f) { dxOut /= len; dyOut /= len; hasOut = true; }
                }

                // Calculate Tangent vector
                float tx, ty;
                if (hasIn && hasOut) {
                    tx = dxIn + dxOut;
                    ty = dyIn + dyOut;
                    float len = (float) Math.sqrt(tx * tx + ty * ty);
                    if (len > 0.001f) { tx /= len; ty /= len; }
                    else { tx = -dyIn; ty = dxIn; } // Fallback for 180 degree snaps
                } else if (hasIn) {
                    tx = dxIn; ty = dyIn;
                } else {
                    tx = dxOut; ty = dyOut;
                }

                // Miter vector is perpendicular to tangent
                float mx = -ty;
                float my = tx;

                // Adjust miter length to preserve thickness during sharp angled turns
                float miterLen = hw;
                if (hasIn && hasOut) {
                    float dot = mx * (-dyIn) + my * (dxIn);
                    if (Math.abs(dot) > 0.01f) {
                        miterLen = hw / dot;
                    }
                }

                // Clamp to prevent visual spikes on physically impossible angles
                miterLen = Math.min(miterLen, hw * 5f);

                // Project left and right boundary vertices
                float currLx = sx.get(i) + mx * miterLen;
                float currLy = sy.get(i) + my * miterLen;
                float currRx = sx.get(i) - mx * miterLen;
                float currRy = sy.get(i) - my * miterLen;

                // 3. Draw the Quad
                if (i > 0) {
                    // Because we draw this exactly from the previous points,
                    // it is mathematically impossible for it to overlap or leave a gap.
                    renderer.triangle(prevLx, prevLy, prevRx, prevRy, currRx, currRy);
                    renderer.triangle(prevLx, prevLy, currRx, currRy, currLx, currLy);
                }

                // Cycle memory for the next segment
                prevLx = currLx; prevLy = currLy;
                prevRx = currRx; prevRy = currRy;
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

    private void updateExtremes(Array<Beatmap.LaserSequence> seqs, float currentTime, float lookahead, float[] extremes) {
        if (seqs == null) return;
        for (Beatmap.LaserSequence seq : seqs) {
            if (seq.nodes.size == 0) continue;

            float first = seq.nodes.get(0).offset;
            float last = seq.nodes.get(seq.nodes.size - 1).offset;

            if (last < currentTime || first > currentTime + lookahead) continue;

            for (int i = 0; i < seq.nodes.size - 1; i++) {
                Beatmap.LaserNode a = seq.nodes.get(i);
                Beatmap.LaserNode b = seq.nodes.get(i+1);

                if (b.offset >= currentTime && a.offset <= currentTime + lookahead) {
                    if (a.x < extremes[0]) extremes[0] = a.x;
                    if (a.x > extremes[1]) extremes[1] = a.x;
                    if (b.x < extremes[0]) extremes[0] = b.x;
                    if (b.x > extremes[1]) extremes[1] = b.x;
                }
            }
        }
    }

    public float getTargetZoom(Array<Beatmap.LaserSequence> left, Array<Beatmap.LaserSequence> right, float currentTime) {
        float[] extremes = {0.0f, 1.0f};
        float lookaheadMs = 4000f;

        updateExtremes(left, currentTime, lookaheadMs, extremes);
        updateExtremes(right, currentTime, lookaheadMs, extremes);

        float maxDist = Math.max(Math.abs(extremes[0] - 0.5f), Math.abs(extremes[1] - 0.5f));

        if (maxDist > 0.5f) {
            return 0.5f / maxDist;
        }
        return 1.0f;
    }
}
