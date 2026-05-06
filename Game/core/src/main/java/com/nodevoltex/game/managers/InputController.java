package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.entities.Note;

public class InputController {

    public void processNoteInputs(Array<Note> activeNotes, float currentAudioTimeMs, ScoreManager scoreManager) {
        boolean[] laneJustPressed = new boolean[7];
        boolean[] laneIsPressed = new boolean[7];

        // BT Buttons (Lanes 1 to 4)
        laneJustPressed[1] = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.Y);
        laneJustPressed[2] = Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.U);
        laneJustPressed[3] = Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.I);
        laneJustPressed[4] = Gdx.input.isKeyJustPressed(Input.Keys.T) || Gdx.input.isKeyJustPressed(Input.Keys.O);

        laneIsPressed[1] = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.Y);
        laneIsPressed[2] = Gdx.input.isKeyPressed(Input.Keys.E) || Gdx.input.isKeyPressed(Input.Keys.U);
        laneIsPressed[3] = Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.I);
        laneIsPressed[4] = Gdx.input.isKeyPressed(Input.Keys.T) || Gdx.input.isKeyPressed(Input.Keys.O);

        // FX Buttons (Lane 5 = Left FX, Lane 6 = Right FX)
        laneJustPressed[5] = Gdx.input.isKeyJustPressed(Input.Keys.X) || Gdx.input.isKeyJustPressed(Input.Keys.N);
        laneJustPressed[6] = Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.C);

        laneIsPressed[5] = Gdx.input.isKeyPressed(Input.Keys.X) || Gdx.input.isKeyPressed(Input.Keys.N);
        laneIsPressed[6] = Gdx.input.isKeyPressed(Input.Keys.M) || Gdx.input.isKeyPressed(Input.Keys.C);

        // --- 1. HEAD HITS (Taps & Initial Hold Presses) ---
        for (int lane = 1; lane <= 6; lane++) {
            if (laneJustPressed[lane]) {
                Note targetNote = null;

                // FIX: Strictly find the EARLIEST unhit note to prevent Ghost Hitting
                for (Note note : activeNotes) {
                    if (note.lane == lane && !note.isMissed && !note.isCompleted && !note.wasHeadHit) {
                        if (targetNote == null || note.startTime < targetNote.startTime) {
                            targetNote = note;
                        }
                    }
                }

                if (targetNote != null) {
                    float diffMs = Math.abs(targetNote.startTime - currentAudioTimeMs);
                    if (diffMs <= 150.0f) {
                        scoreManager.onHit(diffMs);
                        targetNote.wasHeadHit = true;

                        // Tap notes die immediately on successful hit
                        if (!targetNote.isHold) targetNote.isCompleted = true;
                    }
                }
            }
        }

        // --- 2. CONTINUOUS HOLD TRACKING ---
        for (Note note : activeNotes) {
            // Only process active Hold notes that have already been hit successfully
            if (note.isMissed || note.isCompleted || !note.isHold || !note.wasHeadHit) continue;

            if (laneIsPressed[note.lane]) {
                // Kept holding until the absolute end
                if (currentAudioTimeMs >= note.endTime) {
                    note.isCompleted = true;
                    scoreManager.onHit(0f);
                }
            } else {
                // FIX: Hold Release Leniency Window
                // If the user lets go early, but they are within 100ms of the end of the note,
                // grant them the completion anyway.
                if (note.endTime - currentAudioTimeMs <= 100.0f) {
                    note.isCompleted = true;
                    scoreManager.onHit(0f);
                } else {
                    // They truly let go too early
                    note.isMissed = true;
                    scoreManager.onMiss();
                }
            }
        }
    }
}
