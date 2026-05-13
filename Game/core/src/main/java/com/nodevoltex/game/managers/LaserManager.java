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

    public boolean isAutoPlay = false;

    private int getSlamEndNodeIndex(Beatmap.LaserSequence seq, float tickTime) {
        if (seq.nodes.size < 2) return -1;
        for (int i = 1; i < seq.nodes.size; i++) {
            Beatmap.LaserNode prev = seq.nodes.get(i - 1);
            Beatmap.LaserNode curr = seq.nodes.get(i);
            if (Math.abs(curr.offset - tickTime) < 1.0f) {
                if (curr.offset - prev.offset < 50f && Math.abs(curr.x - prev.x) > 0.01f) {
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

            // --- PART 1: VISUALS & STATE UPDATES ---
            if (currentTime >= firstOffset && currentTime <= lastOffset + 50f) {
                isCurrentlyOnLaser = true;

                if (currentTime - firstOffset < 100f && !cursor.wasAutoSnapped) {
                    cursor.setState(new LockedState());
                    cursor.isMissed = false;
                    cursor.wasAutoSnapped = true;
                    cursor.x = sequence.nodes.get(0).x;
                }

                float currentLaserX = calculateLaserPositionAtTime(sequence, currentTime);

                // --- INSTANT RECOVERY & GRACE TIMER LOGIC ---
                if (cursor.isMissed) {
                    if (Math.abs(cursor.x - currentLaserX) <= 0.35f) {
                        cursor.isMissed = false;
                        cursor.missedTimer = 0f;
                        cursor.setState(new LockedState());
                    } else {
                        cursor.missedTimer += delta * 1000f;
                        if (cursor.missedTimer >= 200f && !cursor.hasComboBroken) {
                            scoreManager.onMiss("LASER");
                            cursor.hasComboBroken = true;
                        }
                    }
                }

                float direction = 0;
                for (int i = 0; i < sequence.nodes.size - 1; i++) {
                    if (sequence.nodes.get(i).offset <= currentTime && sequence.nodes.get(i+1).offset > currentTime) {
                        direction = Math.signum(sequence.nodes.get(i+1).x - sequence.nodes.get(i).x);
                        break;
                    }
                }

                cursor.targetLaserX = currentLaserX;
                cursor.requiresInput = (direction != 0);

                // Read the human physical keyboard first...
                cursor.pollInputs(direction, inputController, currentTime);

                // --- THE LASER ROBOT OVERRIDE ---
                if (isAutoPlay) {
                    cursor.x = currentLaserX;
                    // Fool the engine into thinking the player is holding the exact right keys!
                    cursor.isHoldingCorrectKey = true;

                    if (direction < 0) {
                        cursor.isMovingLeft = true;
                        cursor.isMovingRight = false;
                    } else if (direction > 0) {
                        cursor.isMovingRight = true;
                        cursor.isMovingLeft = false;
                    } else {
                        cursor.isMovingLeft = false;
                        cursor.isMovingRight = false;
                    }
                }
            }

            // --- PART 2: THE PRE-BAKED TICK ENGINE (SCORING) ---
            if (sequence.tickTimes != null) {
                while (sequence.nextTickIndex < sequence.tickTimes.size) {
                    float expectedTickTime = sequence.tickTimes.get(sequence.nextTickIndex);

                    if (currentTime >= expectedTickTime) {
                        boolean isStartTick = (sequence.nextTickIndex == 0);
                        int slamNodeIndex = getSlamEndNodeIndex(sequence, expectedTickTime);
                        boolean isSlamEnd = (slamNodeIndex != -1);
                        float targetLaserPos = calculateLaserPositionAtTime(sequence, expectedTickTime);
                        boolean isHit = false;

                        if (isStartTick) {
                            if (cursor.wasAutoSnapped || Math.abs(cursor.x - targetLaserPos) <= 0.40f) {
                                isHit = true;
                                cursor.x = targetLaserPos;
                            }
                        } else if (isSlamEnd) {
                            float slamStartX = sequence.nodes.get(slamNodeIndex - 1).x;
                            float slamDirection = Math.signum(targetLaserPos - slamStartX);
                            boolean correctFlick = false;

                            // --- ROBOT FLICK OVERRIDE ---
                            if (isAutoPlay) {
                                correctFlick = true; // The robot never misses a flick!
                            } else {
                                if (slamDirection > 0 && cursor.isMovingRight) correctFlick = true;
                                if (slamDirection < 0 && cursor.isMovingLeft) correctFlick = true;
                            }

                            if (correctFlick || Math.abs(cursor.x - targetLaserPos) <= 0.30f) {
                                isHit = true;
                                cursor.x = targetLaserPos;

                                if (correctFlick && slamSound != null) {
                                    if (shouldPlayHitsound(sequence, slamNodeIndex)) {
                                        // --- THE FIX: Apply Master & Effect Volume to the slam! ---
                                        float vol = SettingsManager.getMasterVolume() * SettingsManager.getEffectVolume();
                                        slamSound.play(vol);
                                    }
                                }
                            }
                        } else {
                            if (Math.abs(cursor.x - targetLaserPos) <= 0.35f) {
                                isHit = true;
                            }
                        }

                        if (isHit) {
                            scoreManager.onLaserTick();
                            cursor.isMissed = false;
                            cursor.missedTimer = 0f;
                            cursor.hasComboBroken = false;
                            cursor.setState(new LockedState());
                        } else {
                            cursor.isMissed = true;
                            cursor.setState(new com.nodevoltex.game.patterns.FreeState());

                            if (!cursor.hasComboBroken && cursor.missedTimer < 200f) {
                                scoreManager.onLaserTick();
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
