package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
import com.nodevoltex.game.data.BeatmapParser;
import com.nodevoltex.game.entities.LaserCursor;
import com.nodevoltex.game.entities.Note;
import com.nodevoltex.game.managers.InputController;
import com.nodevoltex.game.managers.LaserManager;
import com.nodevoltex.game.managers.ScoreManager;
import com.nodevoltex.game.patterns.GameArchitecture;
import com.badlogic.gdx.audio.Music;
import com.nodevoltex.game.patterns.StrictJudgment;

public class PlayScreen implements Screen {

    private final NodeVoltex game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private String mapFilePath;
    private Music music;

    // Time & Math Variables
    private float currentAudioTimeMs = -2000f;
    private boolean hasAudioStarted = false;
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

    // Pause Menu Variables
    private boolean isPaused = false;
    private Stage pauseStage;

    // Object Pool for Notes
    private Array<Note> activeNotes = new Array<>();
    private final Pool<Note> notePool = new Pool<Note>() {
        @Override protected Note newObject() { return new Note(); }
    };

    // Factory Method for the Pool
    private Note createNote(int lane, float start, float end, String type) {
        Note note = notePool.obtain();
        note.init(lane, start, end, "HOLD".equals(type));
        return note;
    }

    public PlayScreen(NodeVoltex game, String mapFilePath) {
        this.game = game;
        this.mapFilePath = mapFilePath;

        // --- 1. SETUP GRAPHICS & CAMERA ---
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        // --- 2. PARSE THE DYNAMIC JSON ---
        BeatmapParser parser = new BeatmapParser();
        this.beatmap = parser.parse(mapFilePath);

        // CRITICAL SAFETY NET: If the JSON was completely empty or failed to parse
        if (this.beatmap == null) {
            System.out.println("WARNING: JSON failed to parse! Creating blank map.");
            this.beatmap = new Beatmap();
            this.beatmap.general = new Beatmap.General();
        }

        // --- 3. LOAD THE DYNAMIC AUDIO ---
        com.badlogic.gdx.files.FileHandle jsonFile = Gdx.files.internal(mapFilePath);
        com.badlogic.gdx.files.FileHandle audioFile = jsonFile.parent().child(this.beatmap.general.audioFilename);

        try {
            if (audioFile.exists()) {
                music = Gdx.audio.newMusic(audioFile);
            } else {
                System.out.println("WARNING: Audio file not found at " + audioFile.path());
            }
        } catch (Exception e) {
            System.out.println("CRITICAL: Could not load gameplay audio!");
        }

        // Initialize UI Font
        font = new BitmapFont();
        font.getData().setScale(2f);

        // --- 4. INITIALIZE MANAGERS & ENTITIES ---
        inputController = new InputController();
        laserManager = new LaserManager();
        scoreManager = new ScoreManager(new StrictJudgment());

        // FOOLPROOF SCORE MATH: Calculate actual total notes and exact laser ticks!
        int totalNotes = (this.beatmap != null && this.beatmap.hitObjects != null) ? this.beatmap.hitObjects.size : 0;

        // --- THE FIX: Replace the 500 placeholder with the actual calculation ---
        int totalLaserTicks = calculateTotalLaserTicks(this.beatmap);

        scoreManager.setMaxPossibleScore(totalNotes, totalLaserTicks);

        activeNotes = new Array<>();
        leftCursor = new LaserCursor(true, Input.Keys.NUM_2, Input.Keys.NUM_3);
        rightCursor = new LaserCursor(false, Input.Keys.NUM_9, Input.Keys.NUM_0);





        // --- 6. PAUSE MENU SETUP ---
        // Pass your existing viewport so it scales perfectly with the game window
        pauseStage = new Stage(viewport, game.batch);
        Table pauseTable = new Table();
        pauseTable.setFillParent(true);
        pauseStage.addActor(pauseTable);

        Skin skin = NodeVoltex.skin;

        TextButton continueBtn = new TextButton("Continue", skin);
        TextButton retryBtn = new TextButton("Retry", skin);
        TextButton exitBtn = new TextButton("Exit", skin);

        // Stack the buttons in the center of the screen
        pauseTable.add(continueBtn).fillX().pad(10).height(60).row();
        pauseTable.add(retryBtn).fillX().pad(10).height(60).row();
        pauseTable.add(exitBtn).fillX().pad(10).height(60).row();

        // --- BUTTON EVENTS ---
        continueBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                resumeGame();
            }
        });

        retryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                music.stop();
                game.setScreen(new PlayScreen(game, mapFilePath)); // Reload the exact same map
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                music.stop();
                game.setScreen(new SongSelectScreen(game)); // Go back to song selection
            }
        });
    }

    @Override
    public void render(float delta) {
        // --- ESC Key Listener ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isPaused) resumeGame();
            else pauseGame();
        }

        // --- MATH & LOGIC UPDATES (Only runs if NOT paused) ---
        if (!isPaused) {
            // --- TIMELINE & AUDIO SYNC ENGINE ---
            if (!hasAudioStarted) {
                // 1. Move the visual gameplay timeline forward using the framerate
                currentAudioTimeMs += delta * 1000f;

                // 2. The audio offset strictly controls when the music starts playing.
                // If offset is 500, the gameplay ignores it, but the music waits until 500ms to play.
                // If offset is -500, the music plays early while the visual timer is at -500ms.
                if (currentAudioTimeMs >= beatmap.general.audioOffset) {
                    music.play();
                    hasAudioStarted = true;
                }
            } else {
                // --- Check if song finished naturally ---
                if (!music.isPlaying() && !isPaused) {
                    // Determine difficulty name from the file path
                    String diffName = "UNKNOWN";
                    if (mapFilePath.contains("nov.json")) diffName = "NOV";
                    else if (mapFilePath.contains("adv.json")) diffName = "ADV";
                    else if (mapFilePath.contains("exh.json")) diffName = "EXH";
                    else if (mapFilePath.contains("mxm.json")) diffName = "MXM";

                    game.setScreen(new ScoreScreen(game, beatmap.general, scoreManager, diffName, mapFilePath));
                    return; // Stop rendering this frame immediately
                }
                // 3. Once playing, anchor the visual timeline to the music hardware so they never drift.
                // We add the offset back here so the visual timer stays accurately synced to the audio.
                currentAudioTimeMs = (music.getPosition() * 1000f) + beatmap.general.audioOffset;
            }

            // --- LOGIC UPDATES ---

            // Process Note Inputs
            inputController.processNoteInputs(activeNotes, currentAudioTimeMs, scoreManager);

            // Process Laser Interpolation & States
            if (beatmap.lasers != null) {
                laserManager.updateCursor(leftCursor, beatmap.lasers.left, currentAudioTimeMs, delta, scoreManager);
                laserManager.updateCursor(rightCursor, beatmap.lasers.right, currentAudioTimeMs, delta, scoreManager);
            }

            // --- CLEAR SCREEN & SETUP CAMERA ---
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            camera.update();
        }


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

        // --- NEW: Enable Alpha Blending for Lasers ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 3rd: Draw Lasers (Top Layer - Overwrites Notes)
        if (beatmap.lasers != null) {
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.left, true, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.right, false, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
        }

        // --- NEW: Draw Laser Alert Backgrounds ---
        float leftWarningAlpha = laserManager.getWarningAlpha(beatmap.lasers.left, currentAudioTimeMs);
        float rightWarningAlpha = laserManager.getWarningAlpha(beatmap.lasers.right, currentAudioTimeMs);

        if (leftWarningAlpha > 0f) {
            // Faint Cyan Box
            game.shapeRenderer.setColor(0f, 1f, 1f, leftWarningAlpha * 0.2f);
            game.shapeRenderer.rect(50, WORLD_HEIGHT / 2f - 100, 150, 200);
        }

        if (rightWarningAlpha > 0f) {
            // Faint Magenta Box
            game.shapeRenderer.setColor(1f, 0f, 1f, rightWarningAlpha * 0.2f);
            game.shapeRenderer.rect(WORLD_WIDTH - 200, WORLD_HEIGHT / 2f - 100, 150, 200);
        }

        // --- Disable Blending to keep Cursors and UI opaque ---
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Update, Draw, and Manage Notes
        // 1. First Pass: Update math and handle Miss/Cleanup logic for ALL notes
        for (int i = activeNotes.size - 1; i >= 0; i--) {
            Note note = activeNotes.get(i);

            // Miss Detection
            if (!note.wasHeadHit && !note.isMissed && currentAudioTimeMs - note.startTime > 150.0f) {
                scoreManager.onMiss();
                note.isMissed = true;
            }

            // Cleanup
            if (note.getTailY(currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, HIT_LINE_Y) < -200 || note.isCompleted) {
                activeNotes.removeIndex(i);
                notePool.free(note);
            }
        }

        // 2nd: Draw All Notes (Middle Layers)
        for (Note note : activeNotes) {
            if (note.isHold && note.lane >= 5)
                note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, HIT_LINE_Y);
        }
        for (Note note : activeNotes) {
            if (note.isHold && note.lane <= 4)
                note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, HIT_LINE_Y);
        }
        for (Note note : activeNotes) {
            if (!note.isHold && note.lane >= 5)
                note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, HIT_LINE_Y);
        }
        for (Note note : activeNotes) {
            if (!note.isHold && note.lane <= 4)
                note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, HIT_LINE_Y);
        }

        // 3rd: Draw Lasers (Top Layer - Overwrites Notes)
        if (beatmap.lasers != null) {
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.left, true, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.right, false, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
        }

        // 4th: Draw Cursors (Absolute Top)
        leftCursor.draw(game.shapeRenderer, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);
        rightCursor.draw(game.shapeRenderer, TRACK_START_X, TRACK_WIDTH, HIT_LINE_Y);

        game.shapeRenderer.end();

        // --- RENDER UI ---
        game.batch.begin();

        // Temporarily double the font size for the massive warning letters
        font.getData().setScale(4f);

        if (leftWarningAlpha > 0f) {
            //font.setColor(0f, 1f, 1f, leftWarningAlpha); // Blinking Cyan
            font.draw(game.batch, "L", 95, WORLD_HEIGHT / 2f + 30);
        }

        if (rightWarningAlpha > 0f) {
            //font.setColor(1f, 0f, 1f, rightWarningAlpha); // Blinking Magenta
            font.draw(game.batch, "R", WORLD_WIDTH - 145, WORLD_HEIGHT / 2f + 30);
        }

        // Reset the font size back to normal for the Combo text
        font.getData().setScale(2f);


        font.setColor(Color.WHITE);
        font.draw(game.batch, "Combo: " + scoreManager.combo, WORLD_WIDTH / 2f - 80, WORLD_HEIGHT - 125);

        // Dynamic Judgment Color
        if (scoreManager.latestJudgment.contains("CRITICAL")) font.setColor(Color.GOLD);
        else if (scoreManager.latestJudgment.equals("NEAR")) font.setColor(Color.GREEN);
        else font.setColor(Color.RED);

        font.draw(game.batch, scoreManager.latestJudgment, WORLD_WIDTH / 2f - 80, WORLD_HEIGHT / 2f + 100);
        game.batch.end();

        if (isPaused) {
            // 1. Draw 80% Black Dimming Layer
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            game.shapeRenderer.setColor(0f, 0f, 0f, 0.8f); // R, G, B, Alpha
            game.shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            game.shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            // 2. Draw the Buttons on top
            pauseStage.act(delta);
            pauseStage.draw();
        }
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

    private void pauseGame() {
        isPaused = true;
        // Only pause the music if the 2-second lead-in timer has finished!
        if (hasAudioStarted && music.isPlaying()) {
            music.pause();
        }
        // Give control to the UI Stage so buttons can be clicked
        Gdx.input.setInputProcessor(pauseStage);
    }

    private void resumeGame() {
        isPaused = false;
        // Only resume music if it was actually playing before
        if (hasAudioStarted) {
            music.play();
        }
        // Take control away from UI so they can't accidentally click buttons while playing
        Gdx.input.setInputProcessor(null);
    }

    // --- LASER TICK CALCULATION ---
    private int calculateTotalLaserTicks(Beatmap beatmap) {
        if (beatmap == null || beatmap.lasers == null) return 0;

        int totalTicks = 0;
        totalTicks += countTicksForLaserArray(beatmap.lasers.left);
        totalTicks += countTicksForLaserArray(beatmap.lasers.right);

        return totalTicks;
    }

    private int countTicksForLaserArray(Array<Beatmap.LaserSequence> sequences) {
        if (sequences == null) return 0;

        int ticks = 0;
        // 50ms is roughly equivalent to a 1/8th beat tick at 150 BPM
        final float TICK_INTERVAL_MS = 100.0f;

        for (Beatmap.LaserSequence seq : sequences) {
            if (seq.nodes == null || seq.nodes.size == 0) continue;

            // The start of a new laser sequence always grants 1 tick
            ticks++;

            for (int i = 1; i < seq.nodes.size; i++) {
                Beatmap.LaserNode prev = seq.nodes.get(i - 1);
                Beatmap.LaserNode curr = seq.nodes.get(i);

                float duration = curr.offset - prev.offset;

                if (duration <= 1.0f) {
                    // It's a Slam! (Instant horizontal movement with 0ms duration)
                    ticks++;
                } else {
                    // It's a continuous line! Calculate how many 50ms ticks fit inside it
                    ticks += (int) (duration / TICK_INTERVAL_MS);
                }
            }
        }
        return ticks;
    }

    @Override
    public void dispose() {
        font.dispose();
        pauseStage.dispose();
    }
}
