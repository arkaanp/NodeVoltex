package com.nodevoltex.game.data;

import com.badlogic.gdx.utils.Array;

public class Beatmap {
    public General general;
    public Array<HitObject> hitObjects;
    public Lasers lasers;

    public static class General {
        public String title = "Unknown Title";
        public String artist = "Unknown Artist";
        public String mapper = "Unknown Mapper";
        public int level = 0;
        public String audioFilename = "audio.ogg";
        public float audioOffset = 0f;
        public int previewOffset = 0;
        public String jacketFilename = "jak.png";

        // --- Pre-calculated Stats ---
        public int noteCount = 0;
        public int holdCount = 0;
        public int laserCount = 0;
    }

    public static class HitObject {
        public int lane;
        public float startTime;
        public String type;
        public float endTime;
    }

    public static class Lasers {
        public Array<LaserSequence> left;
        public Array<LaserSequence> right;
    }

    // Wrapper class to prevent Type Erasure crashes
    public static class LaserSequence {
        public Array<LaserNode> nodes;

        // --- Pre-baked tick engine variables ---
        // 'transient' tells the JSON parser to ignore these variables when loading the file
        public transient com.badlogic.gdx.utils.Array<Float> tickTimes;
        public transient int nextTickIndex = 0;
    }

    public static class LaserNode {
        public float offset;
        public float x;
    }
}
