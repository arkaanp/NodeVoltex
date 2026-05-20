package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.managers.ScoreManager;
import com.nodevoltex.game.data.SaveData;
import com.nodevoltex.game.managers.SettingsManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScoreScreen implements Screen {
    private final NodeVoltex game;
    private final Stage stage;
    private final Skin skin;
    private ShapeRenderer shapeRenderer;

    private Texture bgTexture;
    private Image bgImage;
    private Texture jacketTexture;

    private Group rootGroup;

    private String currentDifficulty;
    private String currentMapPath;
    private long currentReplayTimestamp;
    private String currentReplayJson;

    public ScoreScreen(NodeVoltex game, Beatmap.General metadata, ScoreManager scoreManager,
                       SaveData historyData, String difficultyName, String mapFilePath, boolean fromHistory, long playTimestamp, String replayJson) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin;
        this.shapeRenderer = new ShapeRenderer();
        Gdx.input.setInputProcessor(stage);

        // --- Store difficulty and map path for exit transition ---
        this.currentDifficulty = difficultyName;
        this.currentMapPath = mapFilePath;
        this.currentReplayJson = replayJson;

        bgTexture = new Texture(Gdx.files.internal("Back.png"));
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        // --- EXTRACT DATA ---
        int finalScore, totalSCrit, totalCrit, totalNear, totalMid, totalFar, totalMiss, totalLaserTicks, totalLaserMisses, totalEarly, totalLate, maxCombo;
        String grade;

        if (fromHistory && historyData != null) {
            this.currentReplayTimestamp = historyData.timestamp; // <- Capture from history

            finalScore = historyData.score; grade = historyData.grade; maxCombo = historyData.maxCombo;
            totalSCrit = historyData.sCriticals; totalCrit = historyData.criticals; totalNear = historyData.nears;
            totalMid = historyData.mids; totalFar = historyData.fars; totalMiss = historyData.misses;
            totalLaserTicks = historyData.laserTicks; totalLaserMisses = historyData.laserMisses;
            totalEarly = historyData.early; totalLate = historyData.late;
        } else {
            this.currentReplayTimestamp = playTimestamp; // <- Capture from the live play

            finalScore = scoreManager.getFinalScore(); grade = scoreManager.getGrade(); maxCombo = scoreManager.maxCombo;
            totalSCrit = scoreManager.noteStats.sCriticals + scoreManager.releaseStats.sCriticals;
            totalCrit = scoreManager.noteStats.criticals + scoreManager.releaseStats.criticals;
            totalNear = scoreManager.noteStats.nears + scoreManager.releaseStats.nears;
            totalMid = scoreManager.noteStats.mids + scoreManager.releaseStats.mids;
            totalFar = scoreManager.noteStats.fars + scoreManager.releaseStats.fars;
            totalMiss = scoreManager.noteStats.misses + scoreManager.releaseStats.misses;
            totalLaserTicks = scoreManager.laserTicks;
            totalLaserMisses = scoreManager.getLaserMisses();
            totalEarly = scoreManager.noteStats.early + scoreManager.releaseStats.early;
            totalLate = scoreManager.noteStats.late + scoreManager.releaseStats.late;

            // --- Block score history saving if a mod was used ---
            boolean isModded = SettingsManager.getModAutoPlay() || SettingsManager.getModNoLaser();
            if (!isModded) {
                // --- Pass 'playTimestamp' and 'metadata' to the save method ---
                saveScoreData(metadata, mapFilePath, finalScore, grade, scoreManager, totalSCrit, totalCrit, totalNear, totalMid, totalFar, totalMiss, totalEarly, totalLate, playTimestamp);
            }
        }

        // Resolution Independent Math
        float tan3 = (float)Math.tan(Math.toRadians(3));

        // Keep a responsive ratio for the slanted section
        float topRatio = 0.38f;

        rootGroup = new Group();

        // 1. The Slanted Background (computes top width dynamically)
        ParallelogramActor slantBg = new ParallelogramActor(topRatio, tan3);
        slantBg.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        rootGroup.addActor(slantBg);

        // precompute a topWidth value for layout math based on the ratio
        float topWidth = stage.getViewport().getWorldWidth() * topRatio;

        // 2. The Master UI Layout
        Table fullScreenUI = new Table();
        fullScreenUI.setFillParent(true);
        fullScreenUI.pad(40); // 40px global margin

        Table topRow = new Table();

        // --- LEFT COLUMN ---
        Table leftCol = new Table();
        leftCol.top().left();

        Table metaBox = new Table();
        metaBox.setBackground(skin.newDrawable("white", Color.valueOf("#0f007080")));
        metaBox.pad(15);

        Table innerSongBox = new Table();
        Image albumArt = new Image(skin.newDrawable("white", Color.valueOf("#333333")));

        if (mapFilePath != null) {
            try {
                com.badlogic.gdx.utils.JsonReader jsonReader = new com.badlogic.gdx.utils.JsonReader();
                com.badlogic.gdx.utils.JsonValue root = jsonReader.parse(Gdx.files.internal(mapFilePath));
                String jacketFilename = root.get("general").getString("jacketFilename", "jak.png");
                FileHandle jacketFile = Gdx.files.internal(mapFilePath).parent().child(jacketFilename);

                if (jacketFile.exists()) {
                    jacketTexture = new Texture(jacketFile);
                    albumArt.setDrawable(new TextureRegionDrawable(new TextureRegion(jacketTexture)));
                }
            } catch (Exception e) {}
        }

        innerSongBox.add(albumArt).width(180).height(180).padRight(15);

        Table metaText = new Table();
        Label titleLabel = new Label(metadata.title, skin); titleLabel.setFontScale(1.0f);
        metaText.add(titleLabel).align(Align.left).row();
        metaText.add(new Label(metadata.artist, skin)).align(Align.left).row();
        metaText.add(new Label("mapped by " + metadata.mapper, skin)).align(Align.left).padTop(5).row();

        // --- puts the text back next to the jacket ---
        innerSongBox.add(metaText).expandX().left();

        metaBox.add(innerSongBox).expandX().fillX().padBottom(7.5f).row();
        Label diffLabel = new Label(difficultyName + " " + metadata.level, skin);
        diffLabel.setColor(Color.CYAN);
        metaBox.add(diffLabel).align(Align.left).row();

        // Calculate the exact width to the slant line
        float metaBoxDistFromTop = 40f;
        float slantXAtMeta = topWidth + (metaBoxDistFromTop * tan3);
        float metaBoxWidth = slantXAtMeta - 80f;

        // --- The Wrapper ---
        // By wrapping these together, we protect them from the giant statsTable below
        // which was secretly stretching the entire column and throwing your mods out of bounds
        Table metaWrapper = new Table();

        // 1. Add the dark box to the wrapper, locked to the exact slant width
        metaWrapper.add(metaBox).width(metaBoxWidth).left().row();

        // 2. Build the Mod Indicators
        Table modTable = new Table();

        // --- Tell the table itself to align contents right ---
        modTable.right();

        // Accurate logic: Only show mods if it's a live play.
        boolean usedAutoplay = !fromHistory && SettingsManager.getModAutoPlay();
        boolean usedNoLaser = !fromHistory && SettingsManager.getModNoLaser();

        if (usedAutoplay) {
            Label autoLbl = new Label("AUTOPLAY", skin);
            autoLbl.setColor(Color.valueOf("#00E5FF")); // Arcade Cyan
            autoLbl.setFontScale(0.85f);
            modTable.add(autoLbl).padRight(15);
        }

        if (usedNoLaser) {
            Label noLaserLbl = new Label("NO LASERS", skin);
            noLaserLbl.setColor(Color.valueOf("#FF9100")); // Arcade Orange
            noLaserLbl.setFontScale(0.85f);
            modTable.add(noLaserLbl);
        }

        // --- Push the shrink-wrapped table to the right edge ---
        // By using expandX().right() instead of width(), it slides perfectly into the corner.
        metaWrapper.add(modTable).expandX().right().padTop(5).padBottom(15).row();

        // 4. Add the protected wrapper safely to the main column
        leftCol.add(metaWrapper).left().row();

        // B. The Big Score & Integrated Grade
        String scoreString = String.format("%08d", finalScore);
        Table scoreTable = new Table();
        Label bigScore = new Label(scoreString.substring(0, 4), skin, "large");
        Label smallScore = new Label(scoreString.substring(4, 8), skin, "medium");

        Label gradeLabel = new Label(grade, skin, "huge");
        if (grade.equals("S")) gradeLabel.setColor(Color.valueOf("#FFB300"));
        else if (grade.startsWith("A")) gradeLabel.setColor(Color.valueOf("#00E5FF"));
        else if (grade.equals("B")) gradeLabel.setColor(Color.valueOf("#00E676"));
        else if (grade.equals("C")) gradeLabel.setColor(Color.valueOf("#FF9100"));
        else gradeLabel.setColor(Color.valueOf("#FF1744"));

        scoreTable.add(bigScore).align(Align.bottom);
        // Added padLeft(12) so the gap perfectly matches the PlayScreen
        scoreTable.add(smallScore).align(Align.bottom).padBottom(5).padLeft(12).padRight(30);
        scoreTable.add(gradeLabel).align(Align.bottom).padBottom(5);

        leftCol.add(scoreTable).align(Align.left).padLeft(15).padBottom(20).row();

        // C. The Slanting Stats Engine
        Table statsTable = new Table();
        float currentLeftShift = 0f;

        float statsRowWidth = topWidth - 80f;

        // --- SAFE METRIC CALCULATIONS ---
        int totalNotes = totalSCrit + totalCrit + totalNear + totalMid + totalFar + totalMiss;
        int totalLasers = totalLaserTicks + totalLaserMisses;

        float noteAccuracy = 0f;
        if (totalNotes > 0) {
            noteAccuracy = ((((totalSCrit + totalCrit) * 3.0f) + (totalNear * 2.0f) + (totalMid * 1.0f) + (totalFar * 0.5f))
                / (totalNotes * 3.0f)) * 100f;
        }

        float laserAccuracy = 0f;
        if (totalLasers > 0) {
            laserAccuracy = ((float) totalLaserTicks / totalLasers) * 100f;
        }

        // --- Normalized Float Ratio ---
        String ratioStr;
        if (totalLasers > 0) {
            // Calculates how many notes exist per 1 laser tick
            float normalizedNotes = (float) totalNotes / totalLasers;
            ratioStr = String.format(java.util.Locale.US, "%.2f : 1.00", normalizedNotes);
        } else if (totalNotes > 0) {
            // Fallback if the map has literally 0 lasers
            ratioStr = "1.00 : 0.00";
        } else {
            ratioStr = "0.00 : 0.00";
        }

        currentLeftShift = addSlantedStat(statsTable, "S-Critical", totalSCrit, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Critical", totalCrit, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Near", totalNear, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Mid", totalMid, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Far", totalFar, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Miss", totalMiss, Color.WHITE, statsRowWidth, currentLeftShift, 45f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Laser Tick", totalLaserTicks, Color.YELLOW, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Laser Miss", totalLaserMisses, Color.ORANGE, statsRowWidth, currentLeftShift, 45f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Early", totalEarly, Color.CYAN, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Late", totalLate, Color.PINK, statsRowWidth, currentLeftShift, 45f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Max Combo", maxCombo, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);

        // --- METRICS ---
        currentLeftShift = addSlantedStat(statsTable, "Note Accuracy", String.format(java.util.Locale.US, "%.1f%%", noteAccuracy), Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        addSlantedStat(statsTable, "Laser Accuracy", String.format(java.util.Locale.US, "%.1f%%", laserAccuracy), Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);
        //addSlantedStat(statsTable, "Note/Laser Ratio", ratioStr, Color.WHITE, statsRowWidth, currentLeftShift, 30f, tan3);

        leftCol.add(statsTable).expandX().left().padLeft(15).row();

        // --- RIGHT COLUMN ---
        Table rightCol = new Table();
        rightCol.top().right();

        Image pfp = new Image(skin.newDrawable("white", Color.DARK_GRAY));
        // Use history data if viewing replay, otherwise current session
        String currentPfpUrl = fromHistory && historyData != null ? historyData.profilePictureUrl : SettingsManager.getProfilePictureUrl();
        com.nodevoltex.game.utils.TextureLoader.loadIntoImage(currentPfpUrl, pfp, (Texture)null);
        
        rightCol.add(pfp).width(140).height(140).center().row();
        
        String displayName = fromHistory && historyData != null ? historyData.username : SettingsManager.getUserName();
        Label userLbl = new Label(displayName, skin); 
        userLbl.setColor(Color.BLACK);
        rightCol.add(userLbl).center().padTop(5);

        // Inject Columns into a topRow which will be placed inside a scrollable content area
        topRow.add(leftCol).expand().fill().left();
        topRow.add(rightCol).expand().top().right();

        Table contentTable = new Table();
        contentTable.add(topRow).expand().fill().row();

        final ScrollPane contentScroll = new ScrollPane(contentTable, skin);
        contentScroll.setFlickScroll(false);
        contentScroll.setFadeScrollBars(false);
        contentScroll.setScrollbarsOnTop(true);
        contentScroll.setScrollingDisabled(false, false);

        // Make mouse wheel work without first clicking: set stage scroll focus when pointer is over the pane
        contentScroll.addListener(new InputListener() {
            @Override public boolean mouseMoved(InputEvent event, float x, float y) { stage.setScrollFocus(contentScroll); return false; }
            @Override public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) { stage.setScrollFocus(null); }
        });

        fullScreenUI.add(contentScroll).expand().fill().row();

        // ==========================================
        // BOTTOM SECTION (Exit, Retry, Replay)
        // ==========================================
        Table bottomRow = new Table();

        Table leftBtns = new Table();
        leftBtns.bottom().left();

        TextButton exitBtn = new TextButton("Exit", skin);
        TextButton retryBtn = new TextButton("Retry", skin);

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Pass the SongSelectScreen switch into the animation
                animateOut(() -> game.setScreen(new SongSelectScreen(game, null, mapFilePath, true, currentDifficulty)));
            }
        });

        retryBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                animateOut(() -> game.setScreen(new PlayScreen(game, mapFilePath, 0L, true)));
            }
        });

        Table exitCol = new Table();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(fromHistory ? new Date(historyData.timestamp) : new Date());
        Label dateLbl = new Label("played on " + dateStr, skin);
        dateLbl.setFontScale(0.85f); dateLbl.setColor(Color.LIGHT_GRAY);
        exitCol.add(dateLbl).align(Align.left).padBottom(5).row();
        exitCol.add(exitBtn).width(120).height(40).left();

        leftBtns.add(exitCol).padRight(15).bottom().left();
        if (!fromHistory) leftBtns.add(retryBtn).width(120).height(40).bottom().left();

        Table rightBtns = new Table();
        rightBtns.bottom().right();
        TextButton replayBtn = new TextButton("Watch Replay", skin);
        replayBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                SongListPanel.stopAudio();

                // 1. Check if the .json file exists on the hard drive
                com.badlogic.gdx.files.FileHandle checkFile = Gdx.files.local("replays/replay_" + currentReplayTimestamp + ".json");

                // 2. If it doesn't exist, check if we have the raw data from the server (Global Replay)
                if (!checkFile.exists() && historyData != null && historyData.rawReplayData != null) {
                    try {
                        checkFile.writeString(historyData.rawReplayData, false);
                        Gdx.app.log("Network", "Restored global replay data to disk: " + currentReplayTimestamp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (checkFile.exists()) {
                    animateOut(() -> game.setScreen(new PlayScreen(game, mapFilePath, currentReplayTimestamp)));
                } else {
                    replayBtn.setText("No Replay Found");
                    replayBtn.setColor(Color.DARK_GRAY);
                }
            }
        });
        rightBtns.add(replayBtn).width(160).height(40).bottom().right();

        bottomRow.add(leftBtns).expandX().fillX().bottom().left();
        bottomRow.add(rightBtns).expandX().fillX().bottom().right();

        fullScreenUI.add(bottomRow).expandX().fillX().bottom();

        rootGroup.addActor(fullScreenUI);
        stage.addActor(rootGroup);

        // --- TRIGGER ANIMATION ---
        animateIn();
    }

    // --- THE UNIFIED ANIMATION ENGINE ---
    private void animateIn() {
        float w = stage.getWidth();
        // Entire screen starts off-left, slides right to center (0)
        rootGroup.setX(-w);
        rootGroup.addAction(Actions.moveTo(0, 0, 0.6f, Interpolation.pow3Out));
    }

    // Accept a Runnable so we can dynamically choose where to go AFTER the animation
    private void animateOut(final Runnable onFinish) {
        stage.getRoot().setTouchable(Touchable.disabled);
        float w = stage.getWidth();

        // Entire screen slides out to the left (-w)
        rootGroup.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(-w, 0, 0.5f, Interpolation.pow3In),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.run(onFinish) // <- Runs the screen change here
        ));
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        // Keep root size and parallelogram in sync so slant and layout remain responsive
        rootGroup.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        for (Actor a : rootGroup.getChildren()) {
            if (a instanceof ParallelogramActor) {
                a.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
            }
        }
    }

    // --- INT TO STRING BRIDGE ---
    // Add padLeft dynamically to push the row rightwards, tracing the `\` slant
    private float addSlantedStat(Table table, String label, int value, Color labelColor, float rowWidth, float currentShift, float deltaY, float tan3) {
        // Don't draw the UI here, Convert the int to a String and hand it off
        return addSlantedStat(table, label, String.valueOf(value), labelColor, rowWidth, currentShift, deltaY, tan3);
    }

    // --- THE MASTER STRING METHOD ---
    private float addSlantedStat(Table table, String label, String valueText, Color labelColor, float rowWidth, float currentShift, float deltaY, float tan3) {
        Table row = new Table();
        Label lbl = new Label(label, skin);
        lbl.setColor(labelColor);

        // safely accepts strings like "98.5%" or "1200 : 400"
        Label val = new Label(valueText, skin);
        val.setColor(Color.WHITE);

        row.add(lbl).expandX().left();
        // Slightly widened the value column to 80 to ensure the ratio string doesn't get clipped
        row.add(val).width(80).align(Align.right);

        // padLeft pushes the entire row rightwards, creating the visual slant
        table.add(row).width(rowWidth).left().padLeft(currentShift).padBottom(deltaY - 28f).row();

        return currentShift + (deltaY * tan3);
    }

    private void saveScoreData(com.nodevoltex.game.data.Beatmap.General metadata, String mapFilePath, int finalScore, String grade, ScoreManager scoreManager,
                              int sc, int c, int n, int m, int f, int miss, int early, int late, long playTimestamp) {
        String safeFileName = mapFilePath.replace("/", "_").replace("\\", "_") + "_save.json";
        com.badlogic.gdx.files.FileHandle saveFile = Gdx.files.local("scores/" + safeFileName);
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();

        com.nodevoltex.game.data.ScoreHistory history = new com.nodevoltex.game.data.ScoreHistory();
        if (saveFile.exists()) {
            try { history = json.fromJson(com.nodevoltex.game.data.ScoreHistory.class, saveFile); if (history == null) history = new com.nodevoltex.game.data.ScoreHistory(); }
            catch (Exception e) {}
        }

        com.nodevoltex.game.data.SaveData newData = new com.nodevoltex.game.data.SaveData();
        newData.score = finalScore; newData.grade = grade;
        newData.username = com.nodevoltex.game.managers.SettingsManager.getUserName();
        newData.profilePictureUrl = com.nodevoltex.game.managers.SettingsManager.getProfilePictureUrl();

        // --- Use the synchronized timestamp! ---
        newData.timestamp = playTimestamp;
        newData.maxCombo = scoreManager.maxCombo; newData.sCriticals = sc; newData.criticals = c; newData.nears = n;
        newData.mids = m; newData.fars = f; newData.misses = miss; newData.early = early; newData.late = late;
        newData.laserTicks = scoreManager.laserTicks; newData.laserMisses = scoreManager.getLaserMisses();

        history.plays.add(newData);
        saveFile.writeString(json.prettyPrint(history), false);

        // --- Network Upload if Logged In ---
        if (!com.nodevoltex.game.managers.SettingsManager.getAuthToken().isEmpty()) {
            com.nodevoltex.game.networking.NetworkManager.ScoreRequest scoreReq = new com.nodevoltex.game.networking.NetworkManager.ScoreRequest();
            // Use ID format matching StatsPanel: Title_Difficulty
            scoreReq.mapId = (metadata != null ? metadata.title : "Unknown") + "_" + currentDifficulty;
            scoreReq.title = metadata != null ? metadata.title : "Unknown";
            scoreReq.artist = metadata != null ? metadata.artist : "Unknown";
            scoreReq.difficulty = currentDifficulty;
            scoreReq.level = metadata != null ? metadata.level : 0;
            scoreReq.score = finalScore;
            scoreReq.grade = grade;
            scoreReq.maxCombo = scoreManager.maxCombo;
            scoreReq.sCriticals = sc;
            scoreReq.criticals = c;
            scoreReq.nears = n;
            scoreReq.mids = m;
            scoreReq.fars = f;
            scoreReq.misses = miss;
            scoreReq.laserTicks = scoreManager.laserTicks;
            scoreReq.laserMisses = scoreManager.getLaserMisses();
            scoreReq.early = early;
            scoreReq.late = late;
            scoreReq.replayDataJson = currentReplayJson != null ? currentReplayJson : json.toJson(newData);

            com.nodevoltex.game.networking.NetworkManager.submitScore(scoreReq, new com.nodevoltex.game.networking.NetworkManager.NetworkCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Gdx.app.log("Network", "Score uploaded!");
                }

                @Override
                public void onError(String message) {
                    Gdx.app.error("Network", "Upload failed: " + message);
                }
            });
        }
    }

    private class ParallelogramActor extends Actor {
        private final float topRatio;
        private final float tan3;

        public ParallelogramActor(float topRatio, float tan3) {
            this.topRatio = topRatio;
            this.tan3 = tan3;
        }

        @Override
        public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.valueOf("#1800ad80"));

            float h = getHeight();
            float slantWidth = h * tan3;
            float stageW = (getStage() != null) ? getStage().getViewport().getWorldWidth() : getWidth();
            float topW = stageW * topRatio;
            float bottomW = topW + slantWidth;

            float x = getX();
            float y = getY();

            shapeRenderer.rect(x, y, topW, h);
            shapeRenderer.triangle(x + topW, y + h, x + topW, y, x + bottomW, y);

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            batch.begin();
        }
    }

    @Override public void render(float delta) { Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); stage.act(delta); stage.draw(); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); shapeRenderer.dispose(); if (bgTexture != null) bgTexture.dispose(); if (jacketTexture != null) jacketTexture.dispose(); }
}
