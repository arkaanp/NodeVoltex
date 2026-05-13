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

    // --- NEW: Store difficulty and map path for exit transition ---
    private String currentDifficulty;
    private String currentMapPath;

    public ScoreScreen(NodeVoltex game, Beatmap.General metadata, ScoreManager scoreManager, SaveData historyData, String difficultyName, String mapFilePath, boolean fromHistory) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin;
        this.shapeRenderer = new ShapeRenderer();
        Gdx.input.setInputProcessor(stage);

        // --- NEW: Store difficulty and map path for exit transition ---
        this.currentDifficulty = difficultyName;
        this.currentMapPath = mapFilePath;

        bgTexture = new Texture(Gdx.files.internal("assets/Back.png"));
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        // --- EXTRACT DATA ---
        int finalScore, totalSCrit, totalCrit, totalNear, totalMid, totalFar, totalMiss, totalEarly, totalLate, maxCombo;
        String grade;

        if (fromHistory && historyData != null) {
            finalScore = historyData.score; grade = historyData.grade; maxCombo = historyData.maxCombo;
            totalSCrit = historyData.sCriticals; totalCrit = historyData.criticals; totalNear = historyData.nears;
            totalMid = historyData.mids; totalFar = historyData.fars; totalMiss = historyData.misses;
            totalEarly = historyData.early; totalLate = historyData.late;
        } else {
            finalScore = scoreManager.getFinalScore(); grade = scoreManager.getGrade(); maxCombo = scoreManager.maxCombo;
            totalSCrit = scoreManager.noteStats.sCriticals + scoreManager.releaseStats.sCriticals;
            totalCrit = scoreManager.noteStats.criticals + scoreManager.releaseStats.criticals;
            totalNear = scoreManager.noteStats.nears + scoreManager.releaseStats.nears;
            totalMid = scoreManager.noteStats.mids + scoreManager.releaseStats.mids;
            totalFar = scoreManager.noteStats.fars + scoreManager.releaseStats.fars;
            totalMiss = scoreManager.noteStats.misses + scoreManager.releaseStats.misses;
            totalEarly = scoreManager.noteStats.early + scoreManager.releaseStats.early;
            totalLate = scoreManager.noteStats.late + scoreManager.releaseStats.late;

            saveScoreData(mapFilePath, finalScore, grade, scoreManager, totalSCrit, totalCrit, totalNear, totalMid, totalFar, totalMiss, totalEarly, totalLate);
        }

        // Resolution Independent Math
        float w = stage.getWidth();
        float h = stage.getHeight();
        float tan3 = (float)Math.tan(Math.toRadians(3));

        // Base width of the slanted section: 35% of the screen.
        float topWidth = w * 0.38f;

        rootGroup = new Group();
        rootGroup.setSize(w, h);

        // 1. The Slanted Background
        ParallelogramActor slantBg = new ParallelogramActor(topWidth, tan3);
        slantBg.setSize(w, h);
        rootGroup.addActor(slantBg);

        // 2. The Master UI Layout
        Table fullScreenUI = new Table();
        fullScreenUI.setSize(w, h);
        fullScreenUI.pad(40); // 40px global margin

        Table topRow = new Table();

        // --- LEFT COLUMN ---
        Table leftCol = new Table();
        leftCol.top().left();

        // A. Metadata Box & Exact Jacket Loader
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
        innerSongBox.add(metaText).expandX().left();

        metaBox.add(innerSongBox).expandX().fillX().padBottom(7.5f).row();
        Label diffLabel = new Label(difficultyName + " " + metadata.level, skin);
        diffLabel.setColor(Color.CYAN);
        metaBox.add(diffLabel).align(Align.left).row();

        // THE FIX: Perfect Symmetry!
        // 40px left screen pad + Box Width + 40px right gap = Exact position of the slant line
        float metaBoxDistFromTop = 40f;
        float slantXAtMeta = topWidth + (metaBoxDistFromTop * tan3);
        float metaBoxWidth = slantXAtMeta - 80f;

        leftCol.add(metaBox).width(metaBoxWidth).left().padBottom(20).row();

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
        scoreTable.add(smallScore).align(Align.bottom).padBottom(5).padRight(30);
        scoreTable.add(gradeLabel).align(Align.bottom).padBottom(5);

        leftCol.add(scoreTable).align(Align.left).padLeft(15).padBottom(20).row();

        // C. The Slanting Stats Engine
        Table statsTable = new Table();
        float currentLeftShift = 0f; // Pushes both text and numbers rightwards

        // THE FIX: Calculate exact row width to reach the parallelogram!
        // 40 (global pad) + 15 (statsTable pad) + 25 (safe gap from line) = 80f deduction.
        float statsRowWidth = topWidth - 80f;

        currentLeftShift = addSlantedStat(statsTable, "S-Critical", totalSCrit, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Critical", totalCrit, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Near", totalNear, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Mid", totalMid, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Far", totalFar, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Miss", totalMiss, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 45f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Early", totalEarly, Color.valueOf("#80DFFF"), statsRowWidth, currentLeftShift, 30f, tan3);
        currentLeftShift = addSlantedStat(statsTable, "Late", totalLate, Color.valueOf("#FF80BF"), statsRowWidth, currentLeftShift, 45f, tan3);
        addSlantedStat(statsTable, "Max Combo", maxCombo, Color.LIGHT_GRAY, statsRowWidth, currentLeftShift, 30f, tan3);

        leftCol.add(statsTable).expandX().left().padLeft(15).row();

        // --- RIGHT COLUMN ---
        Table rightCol = new Table();
        rightCol.top().right();

        Image pfp = new Image(skin.newDrawable("white", Color.DARK_GRAY));
        rightCol.add(pfp).width(140).height(140).center().row();
        Label userLbl = new Label("GUEST", skin); userLbl.setColor(Color.BLACK);
        rightCol.add(userLbl).center().padTop(5);

        // Inject Columns
        topRow.add(leftCol).expand().fill().left();
        topRow.add(rightCol).expand().top().right();
        fullScreenUI.add(topRow).expand().fill().row();

        // ==========================================
        // BOTTOM SECTION (Exit, Retry, Replay)
        // ==========================================
        Table bottomRow = new Table();

        Table leftBtns = new Table();
        leftBtns.bottom().left();

        TextButton exitBtn = new TextButton("Exit", skin);
        TextButton retryBtn = new TextButton("Retry", skin);

        exitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { animateOut(mapFilePath); }
        });
        retryBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { game.setScreen(new PlayScreen(game, mapFilePath)); }
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
        TextButton replayBtn = new TextButton("Replay Score", skin);
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

    private void animateOut(final String mapFilePath) {
        stage.getRoot().setTouchable(Touchable.disabled);
        float w = stage.getWidth();

        // Entire screen slides out to the left (-w)
        rootGroup.addAction(Actions.sequence(
            Actions.moveTo(-w, 0, 0.5f, Interpolation.pow3In),
            Actions.run(() -> game.setScreen(new SongSelectScreen(game, null, mapFilePath, true, currentDifficulty)))
        ));
    }

    // THE FIX: Adds padLeft dynamically to push the row rightwards, tracing the `\` slant!
    private float addSlantedStat(Table table, String label, int value, Color labelColor, float rowWidth, float currentShift, float deltaY, float tan3) {
        Table row = new Table();
        Label lbl = new Label(label, skin);
        lbl.setColor(labelColor);
        Label val = new Label(String.valueOf(value), skin);
        val.setColor(Color.WHITE);

        row.add(lbl).expandX().left();
        row.add(val).width(60).align(Align.right);

        // padLeft pushes the entire row rightwards, creating the visual slant!
        table.add(row).width(rowWidth).left().padLeft(currentShift).padBottom(deltaY - 28f).row();

        return currentShift + (deltaY * tan3);
    }

    private void saveScoreData(String mapFilePath, int finalScore, String grade, ScoreManager scoreManager, int sc, int c, int n, int m, int f, int miss, int early, int late) {
        String safeFileName = mapFilePath.replace("/", "_").replace("\\", "_") + "_save.json";
        com.badlogic.gdx.files.FileHandle saveFile = Gdx.files.local("assets/scores/" + safeFileName);
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();

        com.nodevoltex.game.data.ScoreHistory history = new com.nodevoltex.game.data.ScoreHistory();
        if (saveFile.exists()) {
            try { history = json.fromJson(com.nodevoltex.game.data.ScoreHistory.class, saveFile); if (history == null) history = new com.nodevoltex.game.data.ScoreHistory(); }
            catch (Exception e) {}
        }

        com.nodevoltex.game.data.SaveData newData = new com.nodevoltex.game.data.SaveData();
        newData.score = finalScore; newData.grade = grade; newData.timestamp = System.currentTimeMillis();
        newData.maxCombo = scoreManager.maxCombo; newData.sCriticals = sc; newData.criticals = c; newData.nears = n;
        newData.mids = m; newData.fars = f; newData.misses = miss; newData.early = early; newData.late = late;
        newData.laserTicks = scoreManager.laserTicks; newData.laserMisses = scoreManager.laserMisses;

        history.plays.add(newData);
        saveFile.writeString(json.prettyPrint(history), false);
    }

    private class ParallelogramActor extends Actor {
        private final float topW;
        private final float tan3;

        public ParallelogramActor(float topW, float tan3) {
            this.topW = topW;
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
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); shapeRenderer.dispose(); if (bgTexture != null) bgTexture.dispose(); if (jacketTexture != null) jacketTexture.dispose(); }
}
