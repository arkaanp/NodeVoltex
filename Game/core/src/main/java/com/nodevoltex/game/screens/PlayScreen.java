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
    private Label bigScoreLabel;
    private Label smallScoreLabel;
    private Table scoreHud;
    private boolean isIntroDone = false;
    private float introTimer = 0f;

    // Time & Math Variables
    private float currentAudioTimeMs = -2000f;
    private boolean hasAudioStarted = false;
    private final float BASE_SCROLL_SPEED = SettingsManager.getScrollSpeed();
    private float hiSpeedMult = 1.0f;

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

    public PlayScreen(NodeVoltex game, String mapFilePath) {
        this.game = game;
        this.mapFilePath = mapFilePath;

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

        if (this.beatmap == null) {
            System.out.println("WARNING: JSON failed to parse! Creating blank map.");
            this.beatmap = new Beatmap();
            this.beatmap.general = new Beatmap.General();
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
        try {
            com.badlogic.gdx.files.FileHandle replayFile = Gdx.files.local("assets/replays/replay_1778315319697.json");
            com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
            com.nodevoltex.game.data.ReplayData savedReplay = json.fromJson(com.nodevoltex.game.data.ReplayData.class, replayFile);

            if (savedReplay != null) {
                inputController.currentReplay = savedReplay;
                inputController.isReplayPlayback = false;
                inputController.isRecording = false;
                inputController.isAutoPlay = false;
                laserManager.isAutoPlay = false;
            }
        } catch (Exception e) {}

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
        retryBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { if (music != null) music.stop(); game.setScreen(new PlayScreen(game, mapFilePath)); } });
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

        // 2. The Intro Card (Smaller, Cleaner)
        Table introCard = new Table();
        introCard.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.9f)));
        introCard.pad(15);

        Image albumArt = new Image(skin.newDrawable("white", Color.DARK_GRAY));
        if (jacketTexture != null) albumArt.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(jacketTexture)));
        introCard.add(albumArt).width(130).height(130).padRight(15);

        Table metaText = new Table();
        Label titleLbl = new Label(beatmap.general.title, skin); titleLbl.setFontScale(1.0f);
        Label artistLbl = new Label(beatmap.general.artist, skin); artistLbl.setFontScale(1.0f);
        Label mapperLbl = new Label("mapped by " + beatmap.general.mapper, skin); mapperLbl.setFontScale(1.0f);

        metaText.add(titleLbl).left().row();
        metaText.add(artistLbl).left().padTop(5).row();
        metaText.add(mapperLbl).left().padTop(5).row();
        introCard.add(metaText).left();

        introCard.pack();

        // Start completely off-screen to the RIGHT
        introCard.setPosition(WORLD_WIDTH + 100f, WORLD_HEIGHT / 2f - introCard.getHeight() / 2f);

        // Transition Sequence
        introCard.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(WORLD_WIDTH / 2f - introCard.getWidth() / 2f, introCard.getY(), 0.7f, Interpolation.pow3Out), // Slide to center
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(1.5f), // Hold in center
            com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(40f, WORLD_HEIGHT - introCard.getHeight() - 40f, 0.6f, Interpolation.pow3), // Slide to top left
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(() -> {
                    // Dim the background to the exact setting
                    float targetDim = 1.0f - SettingsManager.getBackgroundBrightness();
                    dimOverlay.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(targetDim, 0.6f, Interpolation.pow3));
                })
            )
        ));

        // 3. The Score HUD (Top Right)
        scoreHud = new Table();
        scoreHud.top().right();
        scoreHud.setFillParent(true);
        scoreHud.padTop(20).padRight(30);

        Table scoreTable = new Table();
        bigScoreLabel = new Label("0000", skin); bigScoreLabel.setFontScale(1.6f);
        smallScoreLabel = new Label("0000", skin); smallScoreLabel.setFontScale(1.1f);
        scoreTable.add(bigScoreLabel).align(Align.bottom);
        scoreTable.add(smallScoreLabel).align(Align.bottom).padBottom(3).padLeft(2);
        scoreHud.add(scoreTable).right().row();

        scoreHud.getColor().a = 0f;
        scoreHud.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(2.8f), // Wait until playfield slides up
            com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.5f)
        ));

        // 4. Playfield Anchor
        playfieldAnchor = new Actor();
        playfieldAnchor.setY(-WORLD_HEIGHT); // Start bottom
        playfieldAnchor.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(2.8f), // Start sliding when jacket hits corner
            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(0, 0, 0.8f, Interpolation.pow3Out)
        ));

        uiStage.addActor(introCard);
        uiStage.addActor(scoreHud);
        uiStage.addActor(playfieldAnchor);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isPaused) resumeGame();
            else pauseGame();
        }

        // --- MATH & LOGIC UPDATES ---
        if (!isPaused) {
            uiStage.act(delta);

            // --- INTRO & AUDIO SYNC ENGINE ---
            // --- TIMELINE & AUDIO SYNC ENGINE ---
            if (!isIntroDone) {
                introTimer += delta;
                // Intro animations (Card + Playfield) finish completely at 3.6s.
                if (introTimer >= 3.6f) {
                    isIntroDone = true;
                    // Start the visual timeline 2 seconds BEFORE the song's zero point.
                    // This creates the 2-second visual delay, letting notes fall before audio plays.
                    currentAudioTimeMs = -2000f;
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
                if (music != null && !music.isPlaying() && !isPaused) {
                    String diffName = "UNKNOWN";
                    if (mapFilePath.contains("nov.json")) diffName = "NOV";
                    else if (mapFilePath.contains("adv.json")) diffName = "ADV";
                    else if (mapFilePath.contains("exh.json")) diffName = "EXH";
                    else if (mapFilePath.contains("mxm.json")) diffName = "MXM";

                    if (inputController.isRecording) {
                        inputController.currentReplay.songTitle = beatmap.general.title;
                        inputController.currentReplay.difficulty = diffName;
                        inputController.currentReplay.finalScore = scoreManager.getFinalScore();
                        inputController.currentReplay.timestamp = System.currentTimeMillis();

                        com.badlogic.gdx.files.FileHandle replayFile = Gdx.files.local("assets/replays/replay_" + inputController.currentReplay.timestamp + ".json");
                        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                        replayFile.writeString(json.prettyPrint(inputController.currentReplay), false);
                    }
                    game.setScreen(new ScoreScreen(game, beatmap.general, scoreManager, null, diffName, mapFilePath, false));
                    return;
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

        // --- RENDER PLAYFIELD (With dynamic slide-up offset!) ---
        float drawHitY = HIT_LINE_Y + playfieldAnchor.getY();

        game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        drawTrack(drawHitY);

        // --- KEY PRESS INDICATORS ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float btY = drawHitY - 12f;
        float fxY = drawHitY - 24f;

        for(int lane = 1; lane <= 6; lane++) {
            if(inputController.isLanePressed(lane)) {
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

        if (leftWarningAlpha > 0f) {
            game.shapeRenderer.setColor(0f, 1f, 1f, leftWarningAlpha * 0.2f * visibilityRatio);
            game.shapeRenderer.rect(50, WORLD_HEIGHT / 2f - 100, 150, 200);
        }
        if (rightWarningAlpha > 0f) {
            game.shapeRenderer.setColor(1f, 0f, 1f, rightWarningAlpha * 0.2f * visibilityRatio);
            game.shapeRenderer.rect(WORLD_WIDTH - 200, WORLD_HEIGHT / 2f - 100, 150, 200);
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

        game.batch.begin();
        //float visibilityRatio = 1f - (Math.abs(playfieldAnchor.getY()) / WORLD_HEIGHT);

        if (visibilityRatio > 0.5f) {
            float centerX = TRACK_START_X + (TRACK_WIDTH / 2f);
            float comboY = WORLD_HEIGHT - SettingsManager.getJudgmentComboTopOffset();
            float judY = comboY - 40f;
            float timingY = judY - 30f;

            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

            // Draw Combo
            font.getData().setScale(1.0f);
            font.setColor(1f, 1f, 1f, scoreHud.getColor().a);
            layout.setText(font, "Combo " + scoreManager.combo);
            font.draw(game.batch, layout, centerX - layout.width/2f, comboY);

            // Parse Timing
            String jud = scoreManager.latestJudgment;
            String timing = "";
            if (jud.endsWith(" EARLY")) { timing = "EARLY"; jud = jud.replace(" EARLY", ""); }
            else if (jud.endsWith(" LATE")) { timing = "LATE"; jud = jud.replace(" LATE", ""); }

            // Draw Judgment
            if (jud.contains("CRITICAL")) font.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, scoreHud.getColor().a);
            else if (jud.equals("NEAR")) font.setColor(Color.GREEN.r, Color.GREEN.g, Color.GREEN.b, scoreHud.getColor().a);
            else font.setColor(Color.RED.r, Color.RED.g, Color.RED.b, scoreHud.getColor().a);
            layout.setText(font, jud);
            font.draw(game.batch, layout, centerX - layout.width/2f, judY);

            // Draw Early/Late
            if (!timing.isEmpty()) {
                if (timing.equals("EARLY")) font.setColor(Color.SKY.r, Color.SKY.g, Color.SKY.b, scoreHud.getColor().a);
                else font.setColor(Color.PINK.r, Color.PINK.g, Color.PINK.b, scoreHud.getColor().a);
                font.getData().setScale(0.85f);
                layout.setText(font, timing);
                font.draw(game.batch, layout, centerX - layout.width/2f, timingY);
            }
        }
        game.batch.end();

        // --- PAUSE MENU ---
        if (isPaused) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            game.shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            game.shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
            game.shapeRenderer.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            game.shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            pauseStage.act(delta);
            pauseStage.draw();
        }
    }

    private void drawTrack(float drawHitY) {
        game.shapeRenderer.setColor(Color.RED);
        game.shapeRenderer.rect(TRACK_START_X - 20, drawHitY, TRACK_WIDTH + 40, 5);
        game.shapeRenderer.setColor(Color.DARK_GRAY);
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
