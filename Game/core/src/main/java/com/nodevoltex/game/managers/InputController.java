package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.nodevoltex.game.entities.Note;

public class InputController {

    public void processNoteInputs(Array<Note> activeNotes, float currentAudioTimeMs, ScoreManager scoreManager) {
        boolean[] laneJustPressed = new boolean[5];
        boolean[] laneIsPressed = new boolean[5];

        laneJustPressed[1] = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.Y);
        laneJustPressed[2] = Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.U);
        laneJustPressed[3] = Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.I);
        laneJustPressed[4] = Gdx.input.isKeyJustPressed(Input.Keys.T) || Gdx.input.isKeyJustPressed(Input.Keys.O);

        laneIsPressed[1] = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.Y);
        laneIsPressed[2] = Gdx.input.isKeyPressed(Input.Keys.E) || Gdx.input.isKeyPressed(Input.Keys.U);
        laneIsPressed[3] = Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.I);
        laneIsPressed[4] = Gdx.input.isKeyPressed(Input.Keys.T) || Gdx.input.isKeyPressed(Input.Keys.O);

        for (Note note : activeNotes) {
            if (note.isMissed || note.isCompleted) continue;

            if (!note.wasHeadHit && laneJustPressed[note.lane]) {
                float diffMs = Math.abs(note.startTime - currentAudioTimeMs);
                if (diffMs <= 150.0f) {
                    scoreManager.onHit(diffMs);
                    note.wasHeadHit = true;
                    if (!note.isHold) note.isCompleted = true;
                }
            }

            if (note.isHold && note.wasHeadHit) {
                if (laneIsPressed[note.lane]) {
                    if (currentAudioTimeMs >= note.endTime) {
                        note.isCompleted = true;
                        scoreManager.onHit(0f);
                    }
                } else {
                    note.isMissed = true;
                    scoreManager.onMiss();
                }
            }
        }
    }
}
