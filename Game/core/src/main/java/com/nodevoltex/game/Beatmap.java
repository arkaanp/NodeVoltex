package com.nodevoltex.game;

import com.badlogic.gdx.utils.Array;

public class Beatmap {
    public GeneralData general;
    public Array<HitObject> hitObjects;

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
}
