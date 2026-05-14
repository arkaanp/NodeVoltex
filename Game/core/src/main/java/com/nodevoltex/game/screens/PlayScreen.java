package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
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
import com.nodevoltex.game.managers.SettingsManager;
import com.badlogic.gdx.audio.Music;
import com.nodevoltex.game.patterns.StrictJudgment;

public class PlayScreen implements Screen {

    private final NodeVoltex game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private String mapFilePath;
    private Music music;
    private com.badlogic.gdx.audio.Sound slamSound;

    // --- BACKGROUND ---
    private Texture bgTexture;
    private Texture jacketTexture;

    // --- INTRO & UI ANIMATION VARIABLES ---
    private Stage uiStage;
    private Actor playfieldAnchor;
    private Table introCard; // <--- ADD THIS
    private Label bigScoreLabel;
    private Label smallScoreLabel;
    private Table scoreHud;
    private boolean isIntroDone = false;
    private boolean isTransitioningToScore = false;
    private float introTimer = 0f;

    // Time & Math Variables
    private float currentAudioTimeMs = -2000f;
    private boolean hasAudioStarted = false;
    private final float BASE_SCROLL_SPEED = SettingsManager.getScrollSpeed();
    private float hiSpeedMult = 1.0f;
    private boolean isRetry = false;
    private float introCenterWaitTime = 1.0f; // Default shortened from 1.5s
    private float prerollMs = -2000f;
    private float introTotalDuration = 3.6f;

    // --- NEW: True Map End & Retry Variables ---
    private float finalObjectTimeMs = 0f;
    private float retryHoldTimer = 0f;
    private boolean isRetryingViaHold = false;

    // Playfield Dimensions (Dynamic for ScreenViewport)
    private float WORLD_WIDTH;
    private float WORLD_HEIGHT;
    private float HIT_LINE_Y;
    private float LANE_WIDTH;
    private float TRACK_WIDTH;
    private float TRACK_START_X;

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
    private float pauseDimAlpha = 0f;

    // Object Pool for Notes
    private Array<Note> activeNotes = new Array<>();
    private final Pool<Note> notePool = new Pool<Note>() {
        @Override protected Note newObject() { return new Note(); }
    };

    private Note createNote(int lane, float start, float end, String type) {
        Note note = notePool.obtain();
        note.init(lane, start, end, "HOLD".equals(type));
        return note;
    }

    // Wrapper for normal gameplay
    public PlayScreen(NodeVoltex game, String mapFilePath) {
        this(game, mapFilePath, 0L, false);
    }

    // Wrapper for Replay viewing
    public PlayScreen(NodeVoltex game, String mapFilePath, long replayTimestampToLoad) {
        this(game, mapFilePath, replayTimestampToLoad, false);
    }

    // THE FIX: Master Constructor that applies the Fast Retry math
    public PlayScreen(NodeVoltex game, String mapFilePath, long replayTimestampToLoad, boolean isRetry) {
        this.game = game;
        this.mapFilePath = mapFilePath;
        this.isRetry = isRetry;

        // --- FAST RETRY MATH ---
        this.introCenterWaitTime = isRetry ? 0.15f : 1.0f;
        this.prerollMs = isRetry ? -1000f : -2000f;
        this.currentAudioTimeMs = this.prerollMs;

        // 0.7s slide in + wait time + 0.6s slide out + 0.8s playfield slide up
        this.introTotalDuration = 0.7f + introCenterWaitTime + 0.6f + 0.8f;

        // ... (Keep the rest of your normal graphics setup code below this!)

        // --- 1. SETUP GRAPHICS & CAMERA ---
        camera = new OrthographicCamera();
        viewport = new com.badlogic.gdx.utils.viewport.ScreenViewport(camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        WORLD_WIDTH = viewport.getWorldWidth();
        WORLD_HEIGHT = viewport.getWorldHeight();
        HIT_LINE_Y = SettingsManager.getPlayfieldHitPosY();
        TRACK_WIDTH = SettingsManager.getPlayfieldWidth();
        LANE_WIDTH = TRACK_WIDTH / 4f;
        TRACK_START_X = (WORLD_WIDTH - TRACK_WIDTH) / 2f;

        // Ensures the transition from SongSelect is seamless!
        bgTexture = new Texture(Gdx.files.internal("assets/Back.png"));

        // --- 2. PARSE THE DYNAMIC JSON ---
        BeatmapParser parser = new BeatmapParser();
        this.beatmap = parser.parse(mapFilePath);

        // --- THE FIX: Scan for the absolute final object in the map ---
        if (this.beatmap != null) {
            if (this.beatmap.hitObjects != null && this.beatmap.hitObjects.size > 0) {
                finalObjectTimeMs = Math.max(finalObjectTimeMs, this.beatmap.hitObjects.get(this.beatmap.hitObjects.size - 1).endTime);
            }
            if (this.beatmap.lasers != null) {
                if (this.beatmap.lasers.left != null && this.beatmap.lasers.left.size > 0) {
                    finalObjectTimeMs = Math.max(finalObjectTimeMs, this.beatmap.lasers.left.get(this.beatmap.lasers.left.size - 1).nodes.peek().offset);
                }
                if (this.beatmap.lasers.right != null && this.beatmap.lasers.right.size > 0) {
                    finalObjectTimeMs = Math.max(finalObjectTimeMs, this.beatmap.lasers.right.get(this.beatmap.lasers.right.size - 1).nodes.peek().offset);
                }
            }
        }

        // --- 3. LOAD THE DYNAMIC AUDIO & JACKET ---
        com.badlogic.gdx.files.FileHandle jsonFile = Gdx.files.internal(mapFilePath);
        com.badlogic.gdx.files.FileHandle audioFile = jsonFile.parent().child(this.beatmap.general.audioFilename);
        slamSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/laser_slam1.wav"));

        try {
            com.badlogic.gdx.files.FileHandle jacketFile = jsonFile.parent().child(this.beatmap.general.jacketFilename);
            if (jacketFile.exists()) {
                jacketTexture = new Texture(jacketFile);
            }
        } catch (Exception e) {}

        try {
            if (audioFile.exists()) {
                music = Gdx.audio.newMusic(audioFile);
                music.setVolume(SettingsManager.getMasterVolume() * SettingsManager.getMusicVolume());
            }
        } catch (Exception e) {
            System.out.println("CRITICAL: Could not load gameplay audio!");
        }

        font = com.nodevoltex.game.NodeVoltex.skin.getFont("default");
        font.getData().setScale(2f);

        // --- 4. INITIALIZE MANAGERS ---
        inputController = new InputController();
        laserManager = new LaserManager();

        // --- THE FIX: Pass the mod flags to the gameplay managers! ---
        inputController.isAutoPlay = SettingsManager.getModAutoPlay();
        laserManager.isAutoPlay = SettingsManager.getModAutoPlay();
        laserManager.isNoLaser = SettingsManager.getModNoLaser(); // You will add this field in Step 4

        inputController.isRecording = true;
        scoreManager = new ScoreManager(new StrictJudgment());

        int totalNotes = 0;
        int totalReleases = 0;
        if (this.beatmap != null && this.beatmap.hitObjects != null) {
            for (Beatmap.HitObject note : this.beatmap.hitObjects) {
                totalNotes++;
                if ("HOLD".equals(note.type)) totalReleases++;
            }
        }
        int totalLaserTicks = bakeAndCountLaserTicks(this.beatmap);
        scoreManager.setMaxPossibleScore(totalNotes, totalReleases, totalLaserTicks);

        activeNotes = new Array<>();
        leftCursor = new LaserCursor(true, SettingsManager.getKey("LL", true), SettingsManager.getKey("LR", true), SettingsManager.getKey("LL", false), SettingsManager.getKey("LR", false));
        rightCursor = new LaserCursor(false, SettingsManager.getKey("RL", true), SettingsManager.getKey("RR", true), SettingsManager.getKey("RL", false), SettingsManager.getKey("RR", false));

        // --- THE REPLAY LOADER ---
        if (replayTimestampToLoad > 0) {
            try {
                // --- THE FIX: Load from the safe 'local' directory ---
                com.badlogic.gdx.files.FileHandle replayFile = Gdx.files.local("replays/replay_" + replayTimestampToLoad + ".json");

                if (replayFile.exists()) {
                    com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                    com.nodevoltex.game.data.ReplayData savedReplay = json.fromJson(com.nodevoltex.game.data.ReplayData.class, replayFile);

                    if (savedReplay != null) {
                        inputController.currentReplay = savedReplay;

                        inputController.isReplayPlayback = true;
                        inputController.isRecording = false;

                        inputController.isAutoPlay = false;
                        laserManager.isAutoPlay = false;
                        laserManager.isNoLaser = false;
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to load replay file: " + e.getMessage());
            }
        }

        // --- 5. BUILD INTRO & SCORE UI ---
        buildIntroAndUI();

        // --- 6. PAUSE MENU SETUP ---
        pauseStage = new Stage(viewport, game.batch);
        Table pauseTable = new Table();
        pauseTable.setFillParent(true);
        pauseStage.addActor(pauseTable);

        Skin skin = NodeVoltex.skin;
        TextButton continueBtn = new TextButton("Continue", skin);
        TextButton retryBtn = new TextButton("Retry", skin);
        TextButton exitBtn = new TextButton("Exit", skin);

        pauseTable.add(continueBtn).fillX().pad(10).height(60).row();
        pauseTable.add(retryBtn).fillX().pad(10).height(60).row();
        pauseTable.add(exitBtn).fillX().pad(10).height(60).row();

        continueBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { resumeGame(); } });
        retryBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (music != null) music.stop();
                game.setScreen(new PlayScreen(game, mapFilePath, 0L, true)); // Pass TRUE!
            }
        });
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (music != null) music.stop();

                // --- THE FIX: Destroy the old, off-screen SongSelect menu ---
                if (game.songSelectScreen != null) {
                    game.songSelectScreen.dispose();
                }

                // --- Create a fresh one that automatically slides back in from the right! ---
                game.songSelectScreen = new SongSelectScreen(game, null, mapFilePath, true);
                game.setScreen(game.songSelectScreen);
            }
        });
    }

    private Image dimOverlay;

    private void buildIntroAndUI() {
        uiStage = new Stage(viewport, game.batch);
        Skin skin = NodeVoltex.skin;

        // 1. Background Dim Overlay
        dimOverlay = new Image(skin.newDrawable("white", new Color(0, 0, 0, 1)));
        dimOverlay.setFillParent(true);
        dimOverlay.getColor().a = 0f;
        dimOverlay.setVisible(false); // <--- THE FIX: Hides it from drawing over the UI
        uiStage.addActor(dimOverlay);

        // 2. The Intro Card (Matches ScoreScreen styling perfectly)
        introCard = new Table();

        // --- THE FIX: Generate a guaranteed solid background texture so the box never goes invisible! ---
        com.badlogic.gdx.graphics.Pixmap bgPix = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        bgPix.setColor(new Color(0.1f, 0.1f, 0.15f, 0.9f));
        bgPix.fill();
        introCard.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(bgPix)));
        bgPix.dispose();

        introCard.pad(15);

        Image albumArt = new Image(skin.newDrawable("white", Color.DARK_GRAY));
        if (jacketTexture != null) albumArt.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(jacketTexture)));
        introCard.add(albumArt).width(130).height(130).padRight(15);

        Table metaText = new Table();
        Label titleLbl = new Label(beatmap.general.title, skin); titleLbl.setFontScale(1.0f);
        Label artistLbl = new Label(beatmap.general.artist, skin); artistLbl.setFontScale(1.0f);
        Label mapperLbl = new Label("mapped by " + beatmap.general.mapper, skin); mapperLbl.setFontScale(1.0f);

        // Dynamically get difficulty from the file path
        String diffName = "UNKNOWN";
        if (mapFilePath.contains("nov.json")) diffName = "NOV";
        else if (mapFilePath.contains("adv.json")) diffName = "ADV";
        else if (mapFilePath.contains("exh.json")) diffName = "EXH";
        else if (mapFilePath.contains("mxm.json")) diffName = "MXM";

        Label diffLbl = new Label(diffName + " " + beatmap.general.level, skin);
        diffLbl.setFontScale(1.0f);
        diffLbl.setColor(Color.CYAN);

        metaText.add(titleLbl).left().minWidth(250f).row();
        metaText.add(artistLbl).left().padTop(5).row();
        metaText.add(mapperLbl).left().padTop(5).row();
        metaText.add(diffLbl).left().padTop(5).row();

        introCard.add(metaText).left();

        // Pack calculates the exact width/height of the generated box
        introCard.pack();

        // Start completely off-screen to the RIGHT
        introCard.setPosition(WORLD_WIDTH + 100f, WORLD_HEIGHT / 2f - introCard.getHeight() / 2f);

        // Transition Sequence
        introCard.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(WORLD_WIDTH / 2f - introCard.getWidth() / 2f, introCard.getY(), 0.7f, Interpolation.pow3Out),
            // THE FIX: Use the dynamic wait time
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(introCenterWaitTime),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(25f, WORLD_HEIGHT - introCard.getHeight() - 25f, 0.6f, Interpolation.pow3),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(() -> {
                    float targetDim = 1.0f - SettingsManager.getBackgroundBrightness();
                    dimOverlay.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(targetDim, 0.6f, Interpolation.pow3));
                })
            )
        ));

        // THE FIX: Calculate exactly when the playfield should slide up based on the wait time
        float playfieldDelay = 0.7f + introCenterWaitTime;

        // 3. The Score HUD (Top Right)
        scoreHud = new Table();
        scoreHud.top().right();
        scoreHud.setFillParent(true);
        scoreHud.padTop(20).padRight(30);

        Table scoreTable = new Table();
        // THE FIX: Use "large" and "medium" skins from ScoreScreen to fix font scale
        bigScoreLabel = new Label("0000", skin, "large");
        smallScoreLabel = new Label("0000", skin, "medium");

        scoreTable.add(bigScoreLabel).align(Align.bottom);
        // THE FIX: Added padLeft(12) to widen the gap between the numbers by ~1%
        scoreTable.add(smallScoreLabel).align(Align.bottom).padBottom(5).padLeft(12);
        scoreHud.add(scoreTable).right().row();

        scoreHud.getColor().a = 0f;
        scoreHud.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(playfieldDelay),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.5f)
        ));

        // 4. Playfield Anchor
        playfieldAnchor = new Actor();
        playfieldAnchor.setY(-WORLD_HEIGHT);
        playfieldAnchor.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(playfieldDelay),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(0, 0, 0.8f, Interpolation.pow3Out)
        ));

        uiStage.addActor(introCard);
        uiStage.addActor(scoreHud);
        uiStage.addActor(playfieldAnchor);
    }

    // --- THE OUTRO TRANSITION ENGINE ---
    private void animateOutToScoreScreen() {
        isTransitioningToScore = true;

        // 1. Fade out the background dim darkness
        dimOverlay.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(0f, 0.8f, Interpolation.pow3In));

        // 2. Move Song Box UP and off-screen
        introCard.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(0f, 250f, 0.8f, Interpolation.pow3In));

        // 3. Move Score HUD UP and off-screen
        scoreHud.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(0f, 250f, 0.8f, Interpolation.pow3In));

        // 4. Move Playfield DOWN (This natively fades the 4 track lines & combo text to 0 opacity!)
        playfieldAnchor.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(0f, -WORLD_HEIGHT, 0.8f, Interpolation.pow3In),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.run(() -> {
                String diffName = "UNKNOWN";
                if (mapFilePath.contains("nov.json")) diffName = "NOV";
                else if (mapFilePath.contains("adv.json")) diffName = "ADV";
                else if (mapFilePath.contains("exh.json")) diffName = "EXH";
                else if (mapFilePath.contains("mxm.json")) diffName = "MXM";

                boolean isModded = SettingsManager.getModAutoPlay() || SettingsManager.getModNoLaser();

                // --- THE FIX: Generate exactly ONE timestamp for both the Replay and the Score! ---
                long runTimestamp = System.currentTimeMillis();

                // Don't record if we are modded OR if we are currently watching a replay!
                if (inputController.isRecording && !isModded && !inputController.isReplayPlayback) {
                    inputController.currentReplay.songTitle = beatmap.general.title;
                    inputController.currentReplay.difficulty = diffName;
                    inputController.currentReplay.finalScore = scoreManager.getFinalScore();
                    inputController.currentReplay.timestamp = runTimestamp;

                    // --- Save as a real .json file in the safe 'local' directory (NOT assets!) ---
                    com.badlogic.gdx.files.FileHandle replayFile = Gdx.files.local("replays/replay_" + runTimestamp + ".json");
                    com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                    replayFile.writeString(json.prettyPrint(inputController.currentReplay), false);
                }

                // --- THE FIX: Stop the music exactly as the new screen loads! ---
                if (music != null) {
                    music.stop();
                }

                // --- Pass the exact runTimestamp to the ScoreScreen ---
                game.setScreen(new ScoreScreen(game, beatmap.general, scoreManager, null, diffName, mapFilePath, false, runTimestamp));
            })
        ));
    }

    @Override
    public void render(float delta) {
        // --- THE FIX: Immediate Abort to prevent the 800p/Resize glitch! ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (!isIntroDone) {
                if (music != null) music.stop();

                if (game.songSelectScreen != null) {
                    game.songSelectScreen.dispose();
                }

                game.songSelectScreen = new SongSelectScreen(game, null, mapFilePath, true);
                game.setScreen(game.songSelectScreen);

                // THE FIX: Manually force the viewport to capture the true screen size!
                game.songSelectScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                return;
            } else {
                if (isPaused) resumeGame();
                else pauseGame();
            }
        }

        // --- MATH & LOGIC UPDATES ---
        if (!isPaused) {
            uiStage.act(delta);

            // --- TIMELINE & AUDIO SYNC ENGINE ---
            if (!isIntroDone) {
                introTimer += delta;
                // THE FIX: Use the calculated duration and dynamic preroll values!
                if (introTimer >= introTotalDuration) {
                    isIntroDone = true;
                    currentAudioTimeMs = prerollMs;
                }
            } else if (!hasAudioStarted) {
                currentAudioTimeMs += delta * 1000f;

                if (currentAudioTimeMs >= beatmap.general.audioOffset + SettingsManager.getGlobalOffset()) {
                    if (music != null) {
                        music.play();
                        music.setVolume(SettingsManager.getMasterVolume() * SettingsManager.getMusicVolume());
                    }
                    hasAudioStarted = true;
                }
            } else {
                // --- THE FIX: Hold to Retry Logic ---
                if (Gdx.input.isKeyPressed(SettingsManager.getRetryKey())) {
                    retryHoldTimer += delta;
                    if (retryHoldTimer >= SettingsManager.getRetryHoldTime() && !isRetryingViaHold) {
                        isRetryingViaHold = true;
                        if (music != null) music.stop();
                        game.setScreen(new PlayScreen(game, mapFilePath, 0L, true)); // Trigger Fast Retry
                        return;
                    }
                } else {
                    retryHoldTimer = 0f; // Reset if they let go early
                }

                // --- THE FIX: End the map 2 seconds after the final note, ignoring music length! ---
                if (currentAudioTimeMs >= finalObjectTimeMs + 2000f) {
                    if (!isTransitioningToScore && !isPaused) {
                        animateOutToScoreScreen();
                    }
                }

                if (music != null) currentAudioTimeMs = (music.getPosition() * 1000f) + beatmap.general.audioOffset + SettingsManager.getGlobalOffset();
                else currentAudioTimeMs += delta * 1000f; // Failsafe if audio is missing
            }

            // Logic Updates
            inputController.processNoteInputs(activeNotes, currentAudioTimeMs, scoreManager);

            if (beatmap.lasers != null) {
                laserManager.updateCursor(leftCursor, beatmap.lasers.left, currentAudioTimeMs, delta, scoreManager, slamSound, inputController);
                laserManager.updateCursor(rightCursor, beatmap.lasers.right, currentAudioTimeMs, delta, scoreManager, slamSound, inputController);
            }

            camera.update();
        }

        // --- CLEAR SCREEN & DRAW BACKGROUND ---
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.setProjectionMatrix(camera.combined);
        game.shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw seamless background
        game.batch.begin();
        if (bgTexture != null) game.batch.draw(bgTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        game.batch.end();

        // --- THE FIX: Draw the Background Dim HERE, behind the notes! ---
        if (dimOverlay.getColor().a > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            game.shapeRenderer.setColor(0f, 0f, 0f, dimOverlay.getColor().a);
            game.shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            game.shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

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

        // --- RENDER PLAYFIELD ---
        float drawHitY = HIT_LINE_Y + playfieldAnchor.getY();

        // Calculate fade-in alpha (0f when off-screen, 1f when fully up)
        float trackAlpha = Math.max(0f, 1f - (Math.abs(playfieldAnchor.getY()) / WORLD_HEIGHT));

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

        // Pass trackAlpha into the draw method
        drawTrack(drawHitY, trackAlpha);

        // --- KEY PRESS INDICATORS ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // --- THE FIX: Bulletproof Autoplay scanner ---
        boolean[] autoLanePressed = new boolean[7];
        if (inputController.isAutoPlay && beatmap != null && beatmap.hitObjects != null) {
            // Simply check all notes that have spawned so far! (Extremely fast in Java)
            for (int i = 0; i < nextNoteIndex; i++) {
                Beatmap.HitObject obj = beatmap.hitObjects.get(i);

                if ("HOLD".equals(obj.type)) {
                    if (currentAudioTimeMs >= obj.startTime && currentAudioTimeMs <= obj.endTime) {
                        autoLanePressed[obj.lane] = true;
                    }
                } else { // It's a TAP note
                    // Flashes exactly 80ms for a visual tap
                    if (currentAudioTimeMs >= obj.startTime && currentAudioTimeMs <= obj.startTime + 40f) {
                        autoLanePressed[obj.lane] = true;
                    }
                }
            }
        }

        float btY = drawHitY - 12f;
        float fxY = drawHitY - 24f;

        for(int lane = 1; lane <= 6; lane++) {
            if(inputController.isLanePressed(lane) || autoLanePressed[lane]) {
                if(lane <= 4) { // BT Keys (White)
                    game.shapeRenderer.setColor(1f, 1f, 1f, 0.4f);
                    game.shapeRenderer.rect(TRACK_START_X + (lane-1)*LANE_WIDTH + 5f, btY, LANE_WIDTH - 10f, 10f);
                } else { // FX Keys (Orange/Red)
                    game.shapeRenderer.setColor(1f, 0.3f, 0f, 0.5f);
                    float fxX = (lane == 5) ? TRACK_START_X : TRACK_START_X + LANE_WIDTH*2;
                    game.shapeRenderer.rect(fxX + 5f, fxY, LANE_WIDTH*2 - 10f, 10f);
                }
            }
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- THE FIX: 4 Distinct Laser Input Indicators ---
        // (Make sure there is NO game.shapeRenderer.begin() here!)

        float indicatorY = drawHitY - 5f;
        float radius = 8f; // Slightly larger so the halves are visible
        float leftLaserX = TRACK_START_X - 45f;
        float rightLaserX = TRACK_START_X + TRACK_WIDTH + 45f;

        // Left Laser (Cyan)
        if (leftCursor.isMovingLeft) {
            game.shapeRenderer.setColor(0f, 1f, 1f, 1f);
            // Start at 90 (top), sweep 180 degrees (left half)
            game.shapeRenderer.arc(leftLaserX, indicatorY, radius, 90f, 180f, 20);
        }
        if (leftCursor.isMovingRight) {
            game.shapeRenderer.setColor(0f, 1f, 1f, 1f);
            // Start at 270 (bottom), sweep 180 degrees (right half)
            game.shapeRenderer.arc(leftLaserX, indicatorY, radius, 270f, 180f, 20);
        }

        // Right Laser (Magenta)
        if (rightCursor.isMovingLeft) {
            game.shapeRenderer.setColor(1f, 0f, 1f, 1f);
            game.shapeRenderer.arc(rightLaserX, indicatorY, radius, 90f, 180f, 20);
        }
        if (rightCursor.isMovingRight) {
            game.shapeRenderer.setColor(1f, 0f, 1f, 1f);
            game.shapeRenderer.arc(rightLaserX, indicatorY, radius, 270f, 180f, 20);
        }

        // (Make sure there is NO game.shapeRenderer.end() here!)
        // ----------------------------------------------------------

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (beatmap.lasers != null) {
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.left, true, leftCursor, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, drawHitY);
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.right, false, rightCursor, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, drawHitY);
        }

        float leftWarningAlpha = laserManager.getWarningAlpha(beatmap.lasers.left, currentAudioTimeMs);
        float rightWarningAlpha = laserManager.getWarningAlpha(beatmap.lasers.right, currentAudioTimeMs);

        // Fade out warnings if the playfield is still hidden to prevent ugly clipping
        float visibilityRatio = 1f - (Math.abs(playfieldAnchor.getY()) / WORLD_HEIGHT);

        // --- THE FIX: Increased the distance from the playfield so it doesn't overlap lasers ---
        float warningWidth = 5f;
        float warningMargin = 25f; // <--- THE FIX: Pushes the warning 25px away from the track

        if (leftWarningAlpha > 0f) {
            game.shapeRenderer.setColor(0f, 1f, 1f, leftWarningAlpha * 0.5f * visibilityRatio);
            // Draws safely to the left
            game.shapeRenderer.rect(TRACK_START_X - warningWidth - warningMargin, drawHitY, warningWidth, WORLD_HEIGHT);
        }
        if (rightWarningAlpha > 0f) {
            game.shapeRenderer.setColor(1f, 0f, 1f, rightWarningAlpha * 0.5f * visibilityRatio);
            // Draws safely to the right
            game.shapeRenderer.rect(TRACK_START_X + TRACK_WIDTH + warningMargin, drawHitY, warningWidth, WORLD_HEIGHT);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Update and Draw Notes
        for (int i = activeNotes.size - 1; i >= 0; i--) {
            Note note = activeNotes.get(i);
            if (!note.wasHeadHit && !note.isMissed && currentAudioTimeMs - note.startTime > 150.0f) {
                scoreManager.onMiss("NOTE");
                note.isMissed = true;
            }
            if (note.getTailY(currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, drawHitY) < -200 || note.isCompleted) {
                activeNotes.removeIndex(i);
                notePool.free(note);
            }
        }

        for (Note note : activeNotes) if (note.isHold && note.lane >= 5) note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, drawHitY);
        for (Note note : activeNotes) if (note.isHold && note.lane <= 4) note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, drawHitY);
        for (Note note : activeNotes) if (!note.isHold && note.lane >= 5) note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, drawHitY);
        for (Note note : activeNotes) if (!note.isHold && note.lane <= 4) note.updateAndDraw(game.shapeRenderer, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, LANE_WIDTH, drawHitY);

        if (beatmap.lasers != null) {
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.left, true, leftCursor, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, drawHitY);
            laserManager.drawLasers(game.shapeRenderer, beatmap.lasers.right, false, rightCursor, currentAudioTimeMs, BASE_SCROLL_SPEED, hiSpeedMult, TRACK_START_X, TRACK_WIDTH, drawHitY);
        }

        leftCursor.draw(game.shapeRenderer, TRACK_START_X, TRACK_WIDTH, drawHitY);
        rightCursor.draw(game.shapeRenderer, TRACK_START_X, TRACK_WIDTH, drawHitY);

        game.shapeRenderer.end();

        // --- RENDER UI ---
        String liveScore = String.format("%08d", scoreManager.getFinalScore());
        bigScoreLabel.setText(liveScore.substring(0, 4));
        smallScoreLabel.setText(liveScore.substring(4, 8));

        uiStage.draw(); // Draws dim overlay, intro card, and score hud

        // --- THE FIX: Hold to Retry Progress Visual ---
        if (retryHoldTimer > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            float progress = retryHoldTimer / SettingsManager.getRetryHoldTime();
            float arcDegrees = progress * 360f;

            game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            game.shapeRenderer.setColor(1f, 1f, 1f, 0.6f);
            // Draw a pie chart in the top left corner!
            game.shapeRenderer.arc(40f, WORLD_HEIGHT - 40f, 20f, 90f, -arcDegrees, 30);
            game.shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        game.batch.begin();
        //float visibilityRatio = 1f - (Math.abs(playfieldAnchor.getY()) / WORLD_HEIGHT);

        // --- THE FIX: Fades the text fully to 0 instead of snapping at 0.5 ---
        if (visibilityRatio > 0f) {
            float centerX = TRACK_START_X + (TRACK_WIDTH / 2f);
            float comboY = WORLD_HEIGHT - SettingsManager.getJudgmentComboTopOffset();
            float judY = comboY - 40f;
            float timingY = judY - 30f;

            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

            // Mathematically binds the text opacity to the sliding playfield!
            float textAlpha = scoreHud.getColor().a * visibilityRatio;

            // Draw Combo
            font.getData().setScale(1.0f);
            font.setColor(1f, 1f, 1f, textAlpha);
            layout.setText(font, "Combo " + scoreManager.combo);
            font.draw(game.batch, layout, centerX - layout.width/2f, comboY);

            // Parse Timing
            String jud = scoreManager.latestJudgment;
            String timing = "";
            if (jud.endsWith(" EARLY")) { timing = "EARLY"; jud = jud.replace(" EARLY", ""); }
            else if (jud.endsWith(" LATE")) { timing = "LATE"; jud = jud.replace(" LATE", ""); }

            // Draw Judgment
            if (jud.contains("CRITICAL")) font.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, textAlpha);
            else if (jud.equals("NEAR")) font.setColor(Color.GREEN.r, Color.GREEN.g, Color.GREEN.b, textAlpha);
            else font.setColor(Color.RED.r, Color.RED.g, Color.RED.b, textAlpha);
            layout.setText(font, jud);
            font.draw(game.batch, layout, centerX - layout.width/2f, judY);

            // Draw Early/Late
            if (!timing.isEmpty()) {
                if (timing.equals("EARLY")) font.setColor(Color.SKY.r, Color.SKY.g, Color.SKY.b, textAlpha);
                else font.setColor(Color.PINK.r, Color.PINK.g, Color.PINK.b, textAlpha);
                font.getData().setScale(0.85f);
                layout.setText(font, timing);
                font.draw(game.batch, layout, centerX - layout.width/2f, timingY);
            }
        }
        game.batch.end();

        // --- PAUSE MENU ---
        if (isPaused) {
            // --- THE FIX: Gradually fade to 0.8f instead of instantly snapping ---
            pauseDimAlpha = Math.min(0.8f, pauseDimAlpha + (delta * 5f));

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

            game.shapeRenderer.setColor(0f, 0f, 0f, pauseDimAlpha);
            game.shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

            game.shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            pauseStage.act(delta);
            pauseStage.draw();
        }
    }

    private void drawTrack(float drawHitY, float alpha) {
        game.shapeRenderer.setColor(1f, 0f, 0f, alpha); // Red hit line fades in
        game.shapeRenderer.rect(TRACK_START_X - 20, drawHitY, TRACK_WIDTH + 40, 5);

        game.shapeRenderer.setColor(0.3f, 0.3f, 0.3f, alpha); // Dark gray lanes fade in
        for(int i = 0; i <= 4; i++) {
            game.shapeRenderer.rect(TRACK_START_X + (i * LANE_WIDTH), 0, 2, WORLD_HEIGHT);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        WORLD_WIDTH = viewport.getWorldWidth();
        WORLD_HEIGHT = viewport.getWorldHeight();
        TRACK_START_X = (WORLD_WIDTH - TRACK_WIDTH) / 2f;
        if (uiStage != null) uiStage.getViewport().update(width, height, true);
        if (pauseStage != null) pauseStage.getViewport().update(width, height, true);
    }

    @Override public void show() {} @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { font.dispose(); pauseStage.dispose(); uiStage.dispose(); if (slamSound != null) slamSound.dispose(); if (bgTexture != null) bgTexture.dispose(); if (jacketTexture != null) jacketTexture.dispose(); }

    private void pauseGame() {
        pauseDimAlpha = 0f;
        isPaused = true;
        if (hasAudioStarted && music != null && music.isPlaying()) music.pause();
        Gdx.input.setInputProcessor(pauseStage);
    }

    private void resumeGame() {
        isPaused = false;
        if (hasAudioStarted && music != null) { music.play(); music.setVolume(SettingsManager.getMasterVolume() * SettingsManager.getMusicVolume()); }
        Gdx.input.setInputProcessor(null);
    }

    private int bakeAndCountLaserTicks(Beatmap beatmap) {
        if (beatmap == null || beatmap.lasers == null) return 0;
        int totalTicks = 0;
        totalTicks += bakeArray(beatmap.lasers.left);
        totalTicks += bakeArray(beatmap.lasers.right);
        return totalTicks;
    }

    private int bakeArray(Array<Beatmap.LaserSequence> sequences) {
        if (sequences == null) return 0;
        int ticks = 0;
        for (Beatmap.LaserSequence seq : sequences) {
            seq.tickTimes = new Array<>();
            if (seq.nodes == null || seq.nodes.size == 0) continue;
            seq.tickTimes.add(seq.nodes.first().offset);
            for (int i = 1; i < seq.nodes.size; i++) {
                Beatmap.LaserNode prev = seq.nodes.get(i - 1);
                Beatmap.LaserNode curr = seq.nodes.get(i);
                float duration = curr.offset - prev.offset;
                if (duration <= 100.0f) {
                    if (!seq.tickTimes.contains(curr.offset, false)) seq.tickTimes.add(curr.offset);
                } else {
                    float tickTime = prev.offset + 100.0f;
                    while (tickTime < curr.offset) {
                        if (!seq.tickTimes.contains(tickTime, false)) seq.tickTimes.add(tickTime);
                        tickTime += 100.0f;
                    }
                }
            }
            float lastOffset = seq.nodes.get(seq.nodes.size - 1).offset;
            if (!seq.tickTimes.contains(lastOffset, false)) seq.tickTimes.add(lastOffset);
            seq.tickTimes.sort();
            ticks += seq.tickTimes.size;
        }
        return ticks;
    }


}
