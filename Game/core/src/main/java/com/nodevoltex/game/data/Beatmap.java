package com.nodevoltex.game.data;

import com.badlogic.gdx.utils.Array;

public class Beatmap {
    public GeneralData general;
    public Array<HitObject> hitObjects;
    public Lasers lasers; // NEW

    public static class GeneralData {
        public String audioFilename;
        public float audioOffset;
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
    }

    public static class LaserNode {
        public float offset;
        public float x;
    }
}
