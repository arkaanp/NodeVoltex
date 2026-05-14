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

            // --- THE FIX: Wake up Part 1 early by expanding the window backwards to catch initial slams! ---
            if (currentTime >= firstOffset - graceWindowMs && currentTime <= lastOffset + 50f) {
                isCurrentlyOnLaser = true;

                if (currentTime - firstOffset < 100f && !cursor.wasAutoSnapped) {
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                    cursor.wasAutoSnapped = true;
                    cursor.x = sequence.nodes.get(0).x;
                }

                float currentLaserX = calculateLaserPositionAtTime(sequence, currentTime);

                float direction = 0;
                for (int i = 0; i < sequence.nodes.size - 1; i++) {
                    if (sequence.nodes.get(i).offset <= currentTime && sequence.nodes.get(i+1).offset > currentTime) {
                        direction = Math.signum(sequence.nodes.get(i+1).x - sequence.nodes.get(i).x);
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
                        if (currentTime >= nodeA.offset - graceWindowMs && currentTime <= nodeA.offset + graceWindowMs) {

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
                                    // Make sure you have your SettingsManager properly imported for this!
                                    float vol = SettingsManager.getMasterVolume() * SettingsManager.getEffectVolume();
                                    slamSound.play(vol);
                                }
                            }
                        }
                    }
                }

                // --- THE FREEZE FIX: Absolutely bolt the cursor in place before the laser requires movement ---
                if (currentTime < firstOffset) {
                    // Since it hasn't technically started, enforce the position to stop physical wiggling.
                    // (Note: currentLaserX perfectly accounts for early slam flicks because we updated it above!)
                    cursor.x = currentLaserX;
                }

                cursor.targetLaserX = currentLaserX;

                if (isAutoPlay || isNoLaser) {
                    cursor.x = currentLaserX;
                    cursor.isHoldingCorrectKey = true;
                    if (direction < 0) { cursor.isMovingLeft = true; cursor.isMovingRight = false; }
                    else if (direction > 0) { cursor.isMovingRight = true; cursor.isMovingLeft = false; }
                    else { cursor.isMovingLeft = false; cursor.isMovingRight = false; }
                }

                boolean driftedTooFar = Math.abs(cursor.x - currentLaserX) > 0.15f;
                boolean wrongInput = cursor.requiresInput && !cursor.isHoldingCorrectKey;

                if (wrongInput || driftedTooFar) {
                    if (isCoastingOnResolvedSlam) {
                        wrongInput = false;
                        driftedTooFar = false;
                    } else {
                        // 1. EARLY TURN LENIENCY (Lookahead)
                        float futureTime = currentTime + graceWindowMs;
                        float futureDirection = 0;
                        for (int i = 0; i < sequence.nodes.size - 1; i++) {
                            if (sequence.nodes.get(i).offset <= futureTime && sequence.nodes.get(i+1).offset > futureTime) {
                                futureDirection = Math.signum(sequence.nodes.get(i+1).x - sequence.nodes.get(i).x);
                                break;
                            }
                        }

                        if (futureDirection != 0 && futureDirection != direction) {
                            boolean holdingFuture = (futureDirection < 0 && cursor.isMovingLeft) || (futureDirection > 0 && cursor.isMovingRight);
                            if (holdingFuture) {
                                wrongInput = false;
                                driftedTooFar = false;
                                currentLaserX = calculateLaserPositionAtTime(sequence, futureTime);
                                cursor.x = currentLaserX;
                            }
                        }

                        // 2. LATE TURN LENIENCY (Lookbehind)
                        if (wrongInput) {
                            float pastTime = currentTime - graceWindowMs;
                            float pastDirection = 0;
                            for (int i = 0; i < sequence.nodes.size - 1; i++) {
                                if (sequence.nodes.get(i).offset <= pastTime && sequence.nodes.get(i+1).offset > pastTime) {
                                    pastDirection = Math.signum(sequence.nodes.get(i+1).x - sequence.nodes.get(i).x);
                                    break;
                                }
                            }

                            if (pastDirection != 0 && pastDirection != direction) {
                                boolean holdingPast = (pastDirection < 0 && cursor.isMovingLeft) || (pastDirection > 0 && cursor.isMovingRight);
                                if (holdingPast) {
                                    wrongInput = false;
                                    driftedTooFar = false;
                                }
                            }
                        }
                    }
                }

                if (driftedTooFar || wrongInput) {
                    cursor.isMissed = true;
                    cursor.missedTimer += delta * 1000f;
                    cursor.setState(new com.nodevoltex.game.patterns.FreeState());

                    if (cursor.missedTimer >= graceWindowMs && !cursor.hasComboBroken) {
                        scoreManager.onMiss("LASER");
                        cursor.hasComboBroken = true;
                    }
                } else {
                    cursor.isMissed = false;
                    cursor.missedTimer = 0f;
                    cursor.hasComboBroken = false;
                    cursor.setState(new LockedState());
                }
            }

            // --- PART 2: THE PRE-BAKED SCORE DISPENSER ---
            if (sequence.tickTimes != null) {
                while (sequence.nextTickIndex < sequence.tickTimes.size) {
                    float expectedTickTime = sequence.tickTimes.get(sequence.nextTickIndex);

                    if (currentTime >= expectedTickTime) {
                        if (isAutoPlay || isNoLaser) {
                            scoreManager.onLaserTick();
                        } else {
                            int slamNodeIndex = getSlamEndNodeIndex(sequence, expectedTickTime);
                            boolean isSlamEnd = (slamNodeIndex != -1);

                            if (isSlamEnd) {
                                float slamStartOffset = sequence.nodes.get(slamNodeIndex - 1).offset;
                                float lastResolved = resolvedSlamTimes.getOrDefault(cursor, -1f);

                                if (lastResolved >= slamStartOffset) {
                                    scoreManager.onLaserTick();
                                } else {
                                    if (!cursor.hasComboBroken) {
                                        scoreManager.onMiss("LASER");
                                        cursor.hasComboBroken = true;
                                    }
                                }
                            } else {
                                if (!cursor.isMissed) {
                                    scoreManager.onLaserTick();
                                } else if (cursor.missedTimer < graceWindowMs) {
                                    scoreManager.onLaserTick();
                                } else {
                                    if (!cursor.hasComboBroken) {
                                        scoreManager.onMiss("LASER");
                                        cursor.hasComboBroken = true;
                                    }
                                }
                            }
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
                    // --- THE FREEZE FIX (Part 2): Bolt the cursor exactly in place while waiting ---
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

        for (Beatmap.LaserSequence sequence : laserData) {
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
