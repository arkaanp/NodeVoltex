package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.entities.Note;
import com.nodevoltex.game.data.ReplayData;

public class InputController {

    public boolean isAutoPlay = false;

    // --- REPLAY VARIABLES ---
    public boolean isRecording = false;
    public boolean isReplayPlayback = false;
    public ReplayData currentReplay = new ReplayData();
    private int replayPlaybackIndex = 0;

    // THE GHOST KEYBOARD
    public java.util.HashMap<String, Boolean> virtualKeyboard = new java.util.HashMap<>();

    private boolean[] laneJustPressed = new boolean[7];
    private boolean[] laneIsPressed = new boolean[7];

    public boolean isLanePressed(int lane) {
        if (lane >= 1 && lane <= 6) {
            return laneIsPressed[lane];
        }
        return false;
    }

    private void recordEventIfChanged(int lane, String label, boolean currentState, float timeMs) {
        if (laneJustPressed[lane]) {
            currentReplay.events.add(new ReplayData.InputEvent(timeMs, label, true));
        } else if (!currentState && laneIsPressed[lane]) {
            currentReplay.events.add(new ReplayData.InputEvent(timeMs, label, false));
        }
    }

    // --- NEW: THE PLAYBACK ENGINE ---
    public void updatePlayback(float currentAudioTimeMs) {
        if (!isReplayPlayback || currentReplay == null) return;

        while (replayPlaybackIndex < currentReplay.events.size) {
            ReplayData.InputEvent event = currentReplay.events.get(replayPlaybackIndex);

            // If the timeline has reached this exact event, press/release the virtual key!
            if (currentAudioTimeMs >= event.audioTimeMs) {
                virtualKeyboard.put(event.inputType, event.isPressed);
                replayPlaybackIndex++;
            } else {
                break;
            }
        }
    }

    public void processNoteInputs(Array<Note> activeNotes, float currentAudioTimeMs, ScoreManager scoreManager) {
        if (isAutoPlay) {
            for (Note note : activeNotes) {
                if (note.isMissed || note.isCompleted) continue;
                if (!note.wasHeadHit && currentAudioTimeMs >= note.startTime) {
                    scoreManager.onHit(0f, "NOTE");
                    note.wasHeadHit = true;
                    if (!note.isHold) note.isCompleted = true;
                }
                if (note.isHold && note.wasHeadHit && currentAudioTimeMs >= note.endTime) {
                    scoreManager.onHit(0f, "RELEASE");
                    note.isCompleted = true;
                }
            }
            return;
        }

        // 1. Advance the Ghost Keyboard
        updatePlayback(currentAudioTimeMs);

        // 2. Poll Physical OR Virtual Keys using the SettingsManager!
        boolean currentBT1 = isReplayPlayback ? virtualKeyboard.getOrDefault("BT1", false) :
            (Gdx.input.isKeyPressed(SettingsManager.getKey("BT1", true)) || Gdx.input.isKeyPressed(SettingsManager.getKey("BT1", false)));

        boolean currentBT2 = isReplayPlayback ? virtualKeyboard.getOrDefault("BT2", false) :
            (Gdx.input.isKeyPressed(SettingsManager.getKey("BT2", true)) || Gdx.input.isKeyPressed(SettingsManager.getKey("BT2", false)));

        boolean currentBT3 = isReplayPlayback ? virtualKeyboard.getOrDefault("BT3", false) :
            (Gdx.input.isKeyPressed(SettingsManager.getKey("BT3", true)) || Gdx.input.isKeyPressed(SettingsManager.getKey("BT3", false)));

        boolean currentBT4 = isReplayPlayback ? virtualKeyboard.getOrDefault("BT4", false) :
            (Gdx.input.isKeyPressed(SettingsManager.getKey("BT4", true)) || Gdx.input.isKeyPressed(SettingsManager.getKey("BT4", false)));

        boolean currentFXL = isReplayPlayback ? virtualKeyboard.getOrDefault("FXL", false) :
            (Gdx.input.isKeyPressed(SettingsManager.getKey("FXL", true)) || Gdx.input.isKeyPressed(SettingsManager.getKey("FXL", false)));

        boolean currentFXR = isReplayPlayback ? virtualKeyboard.getOrDefault("FXR", false) :
            (Gdx.input.isKeyPressed(SettingsManager.getKey("FXR", true)) || Gdx.input.isKeyPressed(SettingsManager.getKey("FXR", false)));

        laneJustPressed[1] = currentBT1 && !laneIsPressed[1];
        laneJustPressed[2] = currentBT2 && !laneIsPressed[2];
        laneJustPressed[3] = currentBT3 && !laneIsPressed[3];
        laneJustPressed[4] = currentBT4 && !laneIsPressed[4];
        laneJustPressed[5] = currentFXL && !laneIsPressed[5];
        laneJustPressed[6] = currentFXR && !laneIsPressed[6];

        if (isRecording) {
            recordEventIfChanged(1, "BT1", currentBT1, currentAudioTimeMs);
            recordEventIfChanged(2, "BT2", currentBT2, currentAudioTimeMs);
            recordEventIfChanged(3, "BT3", currentBT3, currentAudioTimeMs);
            recordEventIfChanged(4, "BT4", currentBT4, currentAudioTimeMs);
            recordEventIfChanged(5, "FXL", currentFXL, currentAudioTimeMs);
            recordEventIfChanged(6, "FXR", currentFXR, currentAudioTimeMs);
        }

        laneIsPressed[1] = currentBT1;
        laneIsPressed[2] = currentBT2;
        laneIsPressed[3] = currentBT3;
        laneIsPressed[4] = currentBT4;
        laneIsPressed[5] = currentFXL;
        laneIsPressed[6] = currentFXR;

        // --- 1. HEAD HITS (Taps & Initial Hold Presses) ---
        for (int lane = 1; lane <= 6; lane++) {
            if (laneJustPressed[lane]) {
                Note targetNote = null;
                for (Note note : activeNotes) {
                    // This strictly finds the absolute oldest unresolved note (TRUE NOTELOCK)
                    if (note.lane == lane && !note.isMissed && !note.isCompleted && !note.wasHeadHit) {
                        if (targetNote == null || note.startTime < targetNote.startTime) {
                            targetNote = note;
                        }
                    }
                }

                if (targetNote != null) {
                    float diffMs = currentAudioTimeMs - targetNote.startTime;

                    // The widest FAR window based on x=8 (151 - 3*8 = 127ms)
                    float maxNoteWindow = 127.0f;

                    if (Math.abs(diffMs) <= maxNoteWindow) {
                        scoreManager.onHit(diffMs, "NOTE");
                        targetNote.wasHeadHit = true;
                        if (!targetNote.isHold) targetNote.isCompleted = true;
                    }
                    // If diffMs < -127.0f, it is a Ghost Tap!
                    // Because of Notelock, it completely ignores the input instead of breaking combo or piercing to the next note.
                }
            }
        }

        // --- 2. CONTINUOUS HOLD TRACKING ---
        for (Note note : activeNotes) {
            if (note.isMissed || note.isCompleted || !note.isHold || !note.wasHeadHit) continue;

            if (laneIsPressed[note.lane]) {
                if (currentAudioTimeMs >= note.endTime) {
                    note.isCompleted = true;
                    scoreManager.onHit(0f, "RELEASE");
                }
            } else {
                float diffMs = currentAudioTimeMs - note.endTime;

                // Releases get double the leniency (254ms)
                float maxReleaseWindow = 254.0f;

                if (Math.abs(diffMs) <= maxReleaseWindow) {
                    note.isCompleted = true;
                    scoreManager.onHit(diffMs, "RELEASE");
                } else if (diffMs < -maxReleaseWindow) {
                    // Unlike ghost tapping, letting go of an active hold too early IS a dropped combo!
                    note.isMissed = true;
                    scoreManager.onMiss("RELEASE");
                }
            }
        }
    }
}
