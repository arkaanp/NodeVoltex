package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.managers.ScoreManager;

public class ScoreScreen implements Screen {
    private final NodeVoltex game;
    private final Stage stage;
    private final Skin skin;

    public ScoreScreen(NodeVoltex game, Beatmap.General metadata, ScoreManager scoreManager, String difficultyName, String mapFilePath) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin;
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.pad(30);
        stage.addActor(rootTable);

        // --- LEFT PANEL: METADATA ---
        Table metaTable = new Table();
        metaTable.align(Align.topLeft);

        Label titleLabel = new Label(metadata.title, skin);
        titleLabel.setFontScale(2f); // Make title bigger
        metaTable.add(titleLabel).align(Align.left).padBottom(10).row();
        metaTable.add(new Label("Artist: " + metadata.artist, skin)).align(Align.left).row();
        metaTable.add(new Label("Mapper: " + metadata.mapper, skin)).align(Align.left).padBottom(20).row();

        Label diffLabel = new Label(difficultyName + " - LVL " + metadata.level, skin);
        diffLabel.setColor(Color.CYAN);
        metaTable.add(diffLabel).align(Align.left).row();

        // --- RIGHT PANEL: SCORES & STATS ---
        Table statsTable = new Table();
        statsTable.align(Align.topRight);

        int finalScore = scoreManager.getFinalScore();
        String grade = scoreManager.getGrade();

        // --- CUSTOM JSON SAVING LOGIC (MULTIPLE SCORES) ---
        String safeFileName = mapFilePath.replace("/", "_").replace("\\", "_") + "_save.json";
        com.badlogic.gdx.files.FileHandle saveFile = Gdx.files.local("assets/scores/" + safeFileName);
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();

        com.nodevoltex.game.data.ScoreHistory history = new com.nodevoltex.game.data.ScoreHistory();

        if (saveFile.exists()) {
            try {
                history = json.fromJson(com.nodevoltex.game.data.ScoreHistory.class, saveFile);
                if (history == null) history = new com.nodevoltex.game.data.ScoreHistory();
            } catch (Exception e) {
                System.out.println("Corrupted save file. Starting fresh.");
            }
        }

        // Create the new record for this exact play
        com.nodevoltex.game.data.SaveData newData = new com.nodevoltex.game.data.SaveData();
        newData.score = finalScore;
        newData.grade = grade;
        newData.timestamp = System.currentTimeMillis();

        // --- NEW: Inject Arcade Stats into Save Data ---
        // (We do this AFTER we calculated totalSCrit, totalEarly, etc. further down,
        // so you might need to move this saving block slightly lower in your code,
        // OR calculate the totals right here!)

        int totalSCrit = scoreManager.noteStats.sCriticals + scoreManager.releaseStats.sCriticals;
        int totalCrit = scoreManager.noteStats.criticals + scoreManager.releaseStats.criticals;
        int totalNear = scoreManager.noteStats.nears + scoreManager.releaseStats.nears;
        int totalMid = scoreManager.noteStats.mids + scoreManager.releaseStats.mids;
        int totalFar = scoreManager.noteStats.fars + scoreManager.releaseStats.fars;
        int totalMiss = scoreManager.noteStats.misses + scoreManager.releaseStats.misses;

        int totalEarly = scoreManager.noteStats.early + scoreManager.releaseStats.early;
        int totalLate = scoreManager.noteStats.late + scoreManager.releaseStats.late;

        newData.maxCombo = scoreManager.maxCombo;
        newData.sCriticals = totalSCrit;
        newData.criticals = totalCrit;
        newData.nears = totalNear;
        newData.mids = totalMid;
        newData.fars = totalFar;
        newData.misses = totalMiss;
        newData.laserTicks = scoreManager.laserTicks;
        newData.laserMisses = scoreManager.laserMisses;
        newData.early = totalEarly;
        newData.late = totalLate;

        // Add it to the history array and save!
        history.plays.add(newData);
        saveFile.writeString(json.prettyPrint(history), false);

        // --- THE GRADE DISPLAY ---
        Label gradeLabel = new Label(grade, skin);
        gradeLabel.setFontScale(6.0f);

        if (grade.equals("S")) gradeLabel.setColor(Color.GOLD);
        else if (grade.startsWith("A")) gradeLabel.setColor(Color.CYAN);
        else if (grade.equals("B")) gradeLabel.setColor(Color.GREEN);
        else if (grade.equals("C")) gradeLabel.setColor(Color.ORANGE);
        else gradeLabel.setColor(Color.RED);

        statsTable.add(gradeLabel).align(Align.right).padBottom(10).row();

        // --- THE SCORE DISPLAY ---
        String scoreString = String.format("%08d", finalScore);
        String firstHalf = scoreString.substring(0, 4);
        String secondHalf = scoreString.substring(4, 8);

        Table scoreNumberTable = new Table();
        Label bigScore = new Label(firstHalf, skin);
        bigScore.setFontScale(3.5f);
        bigScore.setColor(Color.YELLOW);

        Label smallScore = new Label(secondHalf, skin);
        smallScore.setFontScale(2.0f);
        smallScore.setColor(Color.YELLOW);

        scoreNumberTable.add(bigScore).align(Align.bottom);
        scoreNumberTable.add(smallScore).align(Align.bottom).padBottom(8);

        statsTable.add(new Label("RESULT SCORE", skin)).align(Align.right).row();
        statsTable.add(scoreNumberTable).align(Align.right).padBottom(20).row();

        // --- THE NEW ARCADE STATS AGGREGATION ---
//        int totalSCrit = scoreManager.noteStats.sCriticals + scoreManager.releaseStats.sCriticals;
//        int totalCrit = scoreManager.noteStats.criticals + scoreManager.releaseStats.criticals;
//        int totalNear = scoreManager.noteStats.nears + scoreManager.releaseStats.nears;
//        int totalMid = scoreManager.noteStats.mids + scoreManager.releaseStats.mids;
//        int totalFar = scoreManager.noteStats.fars + scoreManager.releaseStats.fars;
//        int totalMiss = scoreManager.noteStats.misses + scoreManager.releaseStats.misses;
//
//        int totalEarly = scoreManager.noteStats.early + scoreManager.releaseStats.early;
//        int totalLate = scoreManager.noteStats.late + scoreManager.releaseStats.late;

        // --- STATS RENDERING ---
        statsTable.add(createStatRow("MAX COMBO", String.valueOf(scoreManager.maxCombo), Color.WHITE)).align(Align.right).padBottom(10).row();
        statsTable.add(createStatRow("S-CRITICAL", String.valueOf(totalSCrit), Color.YELLOW)).align(Align.right).row();
        statsTable.add(createStatRow("CRITICAL", String.valueOf(totalCrit), Color.ORANGE)).align(Align.right).row();
        statsTable.add(createStatRow("NEAR", String.valueOf(totalNear), Color.GREEN)).align(Align.right).row();
        statsTable.add(createStatRow("MID", String.valueOf(totalMid), Color.ROYAL)).align(Align.right).row();
        statsTable.add(createStatRow("FAR", String.valueOf(totalFar), Color.SCARLET)).align(Align.right).row();
        statsTable.add(createStatRow("MISS", String.valueOf(totalMiss), Color.RED)).align(Align.right).padBottom(10).row();

        // --- LASER STATS ---
        statsTable.add(createStatRow("LASER TICKS", String.valueOf(scoreManager.laserTicks), Color.CYAN)).align(Align.right).row();
        statsTable.add(createStatRow("LASER MISS", String.valueOf(scoreManager.laserMisses), Color.MAGENTA)).align(Align.right).padBottom(10).row();

        // --- EARLY / LATE TRACKING ---
        Table timingTable = new Table();
        Label earlyLbl = new Label("EARLY: " + totalEarly, skin);
        earlyLbl.setColor(Color.SKY);
        Label lateLbl = new Label("LATE: " + totalLate, skin);
        lateLbl.setColor(Color.CORAL);
        timingTable.add(earlyLbl).padRight(20);
        timingTable.add(lateLbl);
        statsTable.add(timingTable).align(Align.right).row();

        // Add panels to root
        rootTable.add(metaTable).expand().fill().align(Align.topLeft);
        rootTable.add(statsTable).expand().fill().align(Align.topRight).row();

        // --- BOTTOM: EXIT BUTTON ---
        TextButton exitBtn = new TextButton("Return to Song Select", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SongSelectScreen(game, null, mapFilePath));
            }
        });
        rootTable.add(exitBtn).colspan(2).align(Align.bottomRight).padTop(20);
    }

    private Table createStatRow(String labelText, String valueText, Color color) {
        Table row = new Table();
        Label lbl = new Label(labelText + "  ", skin);
        Label val = new Label(valueText, skin);
        val.setColor(color);
        row.add(lbl).width(150).align(Align.left);
        row.add(val).width(50).align(Align.right);
        return row;
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
