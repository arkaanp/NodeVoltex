package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.nodevoltex.game.NodeVoltex;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.entities.LaserCursor;
import com.nodevoltex.game.entities.Note;
import com.nodevoltex.game.managers.InputController;
import com.nodevoltex.game.managers.LaserManager;
import com.nodevoltex.game.managers.ScoreManager;
import com.nodevoltex.game.patterns.GameArchitecture;

public class PlayScreen implements Screen {

    private final NodeVoltex game;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    // Time & Math Variables
    private float currentAudioTimeMs = 0f;
    private final float BASE_SCROLL_SPEED = 1.0f;
    private float hiSpeedMult = 1.0f;

    // Playfield Dimensions
    private final float WORLD_WIDTH = 800f;
    private final float WORLD_HEIGHT = 600f;
    private final float HIT_LINE_Y = 100f;
    private final float LANE_WIDTH = 75f;
    private final float TRACK_WIDTH = LANE_WIDTH * 4;
    private final float TRACK_START_X = (WORLD_WIDTH - TRACK_WIDTH) / 2f;

    // Data & Parsing
    private Beatmap beatmap;
    private int nextNoteIndex = 0;

    // Architecture Managers
    private final InputController inputController;
    private final LaserManager laserManager;
    private final ScoreManager scoreManager;
    private final BitmapFont font;

    // Cursors
    private final LaserCursor leftCursor;
    private final LaserCursor rightCursor;

    // Object Pool for Notes
    private final Array<Note> activeNotes = new Array<>();
    private final Pool<Note> notePool = new Pool<Note>() {
        @Override protected Note newObject() { return new Note(); }
    };

    // Factory Method for the Pool
    private Note createNote(int lane, float start, float end, String type) {
        Note note = notePool.obtain();
        note.init(lane, start, end, "HOLD".equals(type));
        return note;
    }

    public PlayScreen(NodeVoltex game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        // Initialize Managers
        inputController = new InputController();
        laserManager = new LaserManager();
        scoreManager = new ScoreManager(new GameArchitecture.StrictJudgment());

        // Initialize UI Font
        font = new BitmapFont();
        font.getData().setScale(2f);

        // Initialize Laser Cursors with specific keybinds
        leftCursor = new LaserCursor(true, Input.Keys.NUM_2, Input.Keys.NUM_3);
        rightCursor = new LaserCursor(false, Input.Keys.NUM_9, Input.Keys.NUM_0);

        // Load the Beatmap
        Json json = new Json();
        beatmap = json.fromJson(Beatmap.class, Gdx.files.internal("test_map.json"));
    }

    @Override
    public void render(float delta) {
        // Advance Time
        currentAudioTimeMs += delta * 1000f;

        // --- LOGIC UPDATES ---

        // Process Note Inputs
        inputController.processNoteInputs(activeNotes, currentAudioTimeMs, scoreManager);

        // Process Laser Interpolation & States
        if (beatmap.lasers != null) {
            laserManager.updateCursor(leftCursor, beatmap.lasers.left, currentAudioTimeMs, delta);
            laserManager.updateCursor(rightCursor, beatmap.lasers.right, currentAudioTimeMs, delta);
        }

        // --- CLEAR SCREEN & SETUP CAMERA ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.shapeRenderer.setProjectionMatrix(camera.combined);
        game.batch.setProjectionMatrix(camera.combined);

        // --- SPAWNER WINDOW ---
        while (nextNoteIndex < beatmap.hitObjects.size) {
            Beatmap.HitObject data = beatmap.hitObjects.get(nextNoteIndex);
            if (data.startTime - currentAudioTimeMs <= 2000f) {
                activeNotes.add(createNote(data.lane, data.startTime, data.endTime, data.type));
                nextNoteIndex++;
            } else {
                break;
            }
        }

        // --- RENDER SHAPES ---
        game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

        // Draw Track
        drawTrack();

        // Draw Lasers
        if (beatmap.lasers != null) {
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.left, true, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.right, false, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
        }

        // Update, Draw, and Manage Notes
        for (int i = activeNotes.size - 1; i >= 0; i--) {
            Note note = activeNotes.get(i);
            note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, HIT_LINE_Y);

            // MISS DETECTION (For notes that scroll past unhit)
            if (!note.wasHeadHit && !note.isMissed && currentAudioTimeMs - note.startTime > 150.0f) {
                scoreManager.onMiss();
                note.isMissed = true;
            }

            // CLEANUP, Remove if the tail is way past the screen, or if it was successfully completed
            if (note.getTailY(currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, HIT_LINE_Y) < -200 || note.isCompleted) {
                activeNotes.removeIndex(i);
                notePool.free(note);
            }
        }

        // Draw Cursors over everything else on the track
        leftCursor.draw(game.shapeRenderer, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
        rightCursor.draw(game.shapeRenderer, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);

        game.shapeRenderer.end();

        // --- RENDER UI ---
        game.batch.begin();
        font.setColor(Color.WHITE);
        font.draw(game.batch, "Combo: " + scoreManager.combo, 50, WORLD_HEIGHT - 50);

        // Dynamic Judgment Color
        if (scoreManager.latestJudgment.contains("CRITICAL")) font.setColor(Color.GOLD);
        else if (scoreManager.latestJudgment.equals("NEAR")) font.setColor(Color.GREEN);
        else font.setColor(Color.RED);

        font.draw(game.batch, scoreManager.latestJudgment, WORLD_WIDTH / 2f - 80, WORLD_HEIGHT / 2f + 100);
        game.batch.end();
    }

    private void drawTrack() {
        // Red hit line
        game.shapeRenderer.setColor(Color.RED);
        game.shapeRenderer.rect(TRACK_START_X - 20, HIT_LINE_Y, TRACK_WIDTH + 40, 5);

        // Gray lane dividers
        game.shapeRenderer.setColor(Color.DARK_GRAY);
        for(int i = 0; i <= 4; i++) {
            game.shapeRenderer.rect(TRACK_START_X + (i * LANE_WIDTH), 0, 2, WORLD_HEIGHT);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        font.dispose();
    }
}
