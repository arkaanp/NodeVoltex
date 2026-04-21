package com.nodevoltex.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class PlayScreen implements Screen {

    private final NodeVoltex game;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private float currentAudioTimeMs = 0f;
    private final float BASE_SCROLL_SPEED = 1.5f;
    private float hiSpeedMult = 1.0f;

    private final float WORLD_WIDTH = 800f;
    private final float WORLD_HEIGHT = 600f;
    private final float HIT_LINE_Y = 100f;
    private final float LANE_WIDTH = 75f;
    private final float TRACK_WIDTH = LANE_WIDTH * 4;
    private final float TRACK_START_X = (WORLD_WIDTH - TRACK_WIDTH) / 2f;

    // Parser and Data
    private Beatmap beatmap;
    private int nextNoteIndex = 0;

    // Design Pattern: Object Pool
    private final Array<Note> activeNotes = new Array<>();
    private final Pool<Note> notePool = new Pool<Note>() {
        @Override
        protected Note newObject() {
            return new Note();
        }
    };

    public PlayScreen(NodeVoltex game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        loadBeatmap();
    }

    private void loadBeatmap() {
        Json json = new Json();
        // Automatically maps the JSON text directly into our Java objects!
        beatmap = json.fromJson(Beatmap.class, Gdx.files.internal("test_map.json"));
    }

    @Override
    public void render(float delta) {
        currentAudioTimeMs += delta * 1000f;

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.shapeRenderer.setProjectionMatrix(camera.combined);

        // 1. Spawner Logic: Spawn notes exactly 2 seconds before they hit
        float spawnLeadTime = 2000f;
        while (nextNoteIndex < beatmap.hitObjects.size) {
            Beatmap.HitObject hitData = beatmap.hitObjects.get(nextNoteIndex);

            if (hitData.startTime - currentAudioTimeMs <= spawnLeadTime) {
                // Grab a clean note from the pool
                Note newNote = notePool.obtain();
                boolean isHold = "HOLD".equals(hitData.type);
                newNote.init(hitData.lane, hitData.startTime, hitData.endTime, isHold);

                activeNotes.add(newNote);
                nextNoteIndex++;
            } else {
                break; // Not time to spawn the next note yet
            }
        }

        // 2. Rendering Pipeline
        game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

        drawTrack();

        // 3. Update, Draw, and Recycle Notes
        for (int i = activeNotes.size - 1; i >= 0; i--) {
            Note note = activeNotes.get(i);
            note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, HIT_LINE_Y);

            // If the note falls way past the hit line, remove it and put it back in the pool
            if (note.y < -50) {
                activeNotes.removeIndex(i);
                notePool.free(note);
            }
        }

        game.shapeRenderer.end();
    }

    private void drawTrack() {
        game.shapeRenderer.setColor(Color.RED);
        game.shapeRenderer.rect(TRACK_START_X - 20, HIT_LINE_Y, TRACK_WIDTH + 40, 5);

        game.shapeRenderer.setColor(Color.DARK_GRAY);
        for(int i = 0; i <= 4; i++) {
            game.shapeRenderer.rect(TRACK_START_X + (i * LANE_WIDTH), 0, 2, WORLD_HEIGHT);
        }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
