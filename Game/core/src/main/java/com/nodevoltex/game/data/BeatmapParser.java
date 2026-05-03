package com.nodevoltex.game.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class BeatmapParser {

    public Beatmap parse(String filePath) {
        Json json = new Json();

        // This is a great safety net. If you accidentally leave an extra variable
        // in your JSON that doesn't exist in Java, the game won't crash.
        json.setIgnoreUnknownFields(true);

        // Grab the file
        FileHandle file = Gdx.files.internal(filePath);

        // Automatically map the JSON text perfectly into the Beatmap class!
        return json.fromJson(Beatmap.class, file);
    }
}
