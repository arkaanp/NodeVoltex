package com.nodevoltex.game.data;

import com.badlogic.gdx.utils.Array;

public class ReplayData {
    public String songTitle = "";
    public String difficulty = "";
    public int finalScore = 0;
    public long timestamp = 0;

    // A chronological list of every single button press/release
    public Array<InputEvent> events = new Array<>();

    public static class InputEvent {
        public float audioTimeMs;
        public String inputType; // "BT1", "BT2", "FX_L", "LASER_L", etc.
        public boolean isPressed; // true = Pressed down, false = Released

        // Empty constructor needed for JSON parsing
        public InputEvent() {}

        public InputEvent(float audioTimeMs, String inputType, boolean isPressed) {
            this.audioTimeMs = audioTimeMs;
            this.inputType = inputType;
            this.isPressed = isPressed;
        }
    }
}
