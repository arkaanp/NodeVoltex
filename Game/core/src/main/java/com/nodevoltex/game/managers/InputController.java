package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.entities.Note;

public class InputController {

    public void processNoteInputs(Array<Note> activeNotes, float currentAudioTimeMs, ScoreManager scoreManager) {
        boolean[] laneJustPressed = new boolean[7];
        boolean[] laneIsPressed = new boolean[7];

        // BT Buttons
        laneJustPressed[1] = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.Y);
        laneJustPressed[2] = Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.U);
        laneJustPressed[3] = Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.I);
        laneJustPressed[4] = Gdx.input.isKeyJustPressed(Input.Keys.T) || Gdx.input.isKeyJustPressed(Input.Keys.O);

        laneIsPressed[1] = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.Y);
        laneIsPressed[2] = Gdx.input.isKeyPressed(Input.Keys.E) || Gdx.input.isKeyPressed(Input.Keys.U);
        laneIsPressed[3] = Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.I);
        laneIsPressed[4] = Gdx.input.isKeyPressed(Input.Keys.T) || Gdx.input.isKeyPressed(Input.Keys.O);

        // FX Buttons
        laneJustPressed[5] = Gdx.input.isKeyJustPressed(Input.Keys.X) || Gdx.input.isKeyJustPressed(Input.Keys.N);
        laneJustPressed[6] = Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.C);

        laneIsPressed[5] = Gdx.input.isKeyPressed(Input.Keys.X) || Gdx.input.isKeyPressed(Input.Keys.N);
        laneIsPressed[6] = Gdx.input.isKeyPressed(Input.Keys.M) || Gdx.input.isKeyPressed(Input.Keys.C);

        // --- 1. HEAD HITS (Taps & Initial Hold Presses) ---
        for (int lane = 1; lane <= 6; lane++) {
            if (laneJustPressed[lane]) {
                Note targetNote = null;

                for (Note note : activeNotes) {
                    if (note.lane == lane && !note.isMissed && !note.isCompleted && !note.wasHeadHit) {
                        if (targetNote == null || note.startTime < targetNote.startTime) {
                            targetNote = note;
                        }
                    }
                }

                if (targetNote != null) {
                    // FIX: Remove Math.abs() here.
                    // Negative diff = Early, Positive diff = Late
                    float diffMs = currentAudioTimeMs - targetNote.startTime;

                    // Boundary check pushed to 300ms so it properly catches the FAR MISS penalty
                    if (Math.abs(diffMs) <= 300.0f) {
                        scoreManager.onHit(diffMs, "NOTE");
                        targetNote.wasHeadHit = true;

                        if (!targetNote.isHold) targetNote.isCompleted = true;
                    }
                }
            }
        }

        // --- 2. CONTINUOUS HOLD TRACKING ---
        for (Note note : activeNotes) {
            if (note.isMissed || note.isCompleted || !note.isHold || !note.wasHeadHit) continue;

            if (laneIsPressed[note.lane]) {
                // Kept holding until the absolute end
                if (currentAudioTimeMs >= note.endTime) {
                    note.isCompleted = true;
                    scoreManager.onHit(0f, "RELEASE");
                }
            } else {
                // HOLD RELEASE LOGIC
                float diffMs = currentAudioTimeMs - note.endTime;

                // Release window is doubled, so we use 300ms here as well
                if (Math.abs(diffMs) <= 300.0f) {
                    note.isCompleted = true;
                    scoreManager.onHit(diffMs, "RELEASE");
                } else {
                    note.isMissed = true;
                    scoreManager.onMiss("RELEASE");
                }
            }
        }
    }
}
