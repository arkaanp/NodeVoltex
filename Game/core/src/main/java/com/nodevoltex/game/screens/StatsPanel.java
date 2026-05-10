package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

public class StatsPanel extends Table {
    private final Skin skin;

    // ... UI Elements ...
    private Label titleLabel;
    private Label artistLabel;
    private Label mapperLabel;
    private Label diffLabel;
    private Table leaderboardTable;
    private ScrollPane leaderboardScrollPane;
    private com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> currentScores = new com.badlogic.gdx.utils.Array<>();
    private boolean sortScoreAscending = true;

    // --- Jacket Image Variables ---
    private Image jacketImage;
    private com.badlogic.gdx.graphics.Texture jacketTexture;

    // --- Object Tracker Labels ---
    private Label noteCountLabel;
    private Label holdCountLabel;
    private Label laserCountLabel;

    public StatsPanel(Skin skin) {
        this.skin = skin;
        this.top().left();

        // --- UPDATED: Replaced the separate header and toggles with a unified Stack ---
        buildTopSection();
        buildLeaderboard();

        skin.getFont("default").getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private void buildTopSection() {
        Stack topStack = new Stack();

        // ==========================================
        // LAYER 1: The Pink Header and Black Toggles
        // ==========================================
        Table backgroundsTable = new Table();
        backgroundsTable.top().left();

        // --- Pink Header ---
        Stack headerStack = new Stack();
        Image headerBg = new Image(skin.newDrawable("white", new Color(1f, 0.2f, 0.6f, 0.9f)));
        headerStack.add(headerBg);

        Table textTable = new Table();
        textTable.top().left().pad(15); // Tightened padding slightly to fit the new row

        titleLabel = new Label("magical, very magical world", skin);
        titleLabel.setColor(Color.BLACK);
        titleLabel.setFontScale(1f); // --- SCALED 75% ---

        artistLabel = new Label("Camellia", skin);
        artistLabel.setColor(Color.DARK_GRAY);
        artistLabel.setFontScale(1f); // --- SCALED 75% ---

        Table subInfoTable = new Table();
        diffLabel = new Label("EXH 17", skin);
        diffLabel.setColor(Color.RED);
        diffLabel.setFontScale(1f); // --- SCALED 75% ---

        mapperLabel = new Label("mapped by Sotarks", skin);
        mapperLabel.setColor(Color.valueOf("#4A148C"));
        mapperLabel.setFontScale(1f); // --- SCALED 75% ---

        subInfoTable.add(diffLabel).padRight(10);
        subInfoTable.add(mapperLabel);

        // --- NEW: Object Trackers Row ---
        Table objectStatsTable = new Table();
        noteCountLabel = new Label("NOTE: 0", skin);
        holdCountLabel = new Label("HOLD: 0", skin);
        laserCountLabel = new Label("LASER: 0", skin);

        noteCountLabel.setColor(Color.DARK_GRAY);
        holdCountLabel.setColor(Color.DARK_GRAY);
        laserCountLabel.setColor(Color.DARK_GRAY);

        noteCountLabel.setFontScale(0.8f); // --- SCALED 75% ---
        holdCountLabel.setFontScale(0.8f); // --- SCALED 75% ---
        laserCountLabel.setFontScale(0.8f); // --- SCALED 75% ---

        objectStatsTable.add(noteCountLabel).padRight(15);
        objectStatsTable.add(holdCountLabel).padRight(15);
        objectStatsTable.add(laserCountLabel);

        // Add everything to the Text Table
        textTable.add(titleLabel).align(Align.left).row();
        textTable.add(artistLabel).align(Align.left).padBottom(2).row();
        textTable.add(subInfoTable).align(Align.left).padBottom(2).row();
        textTable.add(objectStatsTable).align(Align.left); // Add trackers at the bottom

        headerStack.add(textTable);
        backgroundsTable.add(headerStack).expandX().fillX().padLeft(40).height(150).row();

        // --- Black Toggles ---
        Table toggleTable = new Table();
        toggleTable.left().pad(10);
        toggleTable.background(skin.newDrawable("white", new Color(0, 0, 0, 0.4f)));

        Label sortedByLabel = new Label("sorted by: ", skin);
        sortedByLabel.setFontScale(1f); // --- SCALED 75% ---
        toggleTable.add(sortedByLabel).padRight(5);

        TextButton scoreBtn = new TextButton("score", skin);
        TextButton dateBtn = new TextButton("date", skin);
        scoreBtn.getLabel().setFontScale(1f); // --- SCALED BUTTON TEXT 75% ---
        dateBtn.getLabel().setFontScale(1f);

        scoreBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                sortScoreAscending = true; refreshLeaderboard();
            }
        });
        dateBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                sortScoreAscending = false; refreshLeaderboard();
            }
        });

        toggleTable.add(scoreBtn).padRight(5);
        toggleTable.add(dateBtn).padRight(20);

        Label scopeLabel = new Label("scope: ", skin);
        scopeLabel.setFontScale(1f); // --- SCALED 75% ---
        toggleTable.add(scopeLabel).padRight(5);

        TextButton localBtn = new TextButton("local", skin);
        TextButton globalBtn = new TextButton("global", skin);
        localBtn.getLabel().setFontScale(1f);
        globalBtn.getLabel().setFontScale(1f);

        toggleTable.add(localBtn).padRight(5);
        toggleTable.add(globalBtn);

        backgroundsTable.add(toggleTable).expandX().fillX().padLeft(40).height(50).row();
        topStack.add(backgroundsTable);

        // ==========================================
        // LAYER 2: The Album Jacket
        // ==========================================
        Table jacketLayer = new Table();
        jacketLayer.top().right();
        jacketImage = new Image();
        jacketLayer.add(jacketImage).width(180).height(180).padTop(10).padRight(10);
        topStack.add(jacketLayer);

        this.add(topStack).expandX().fillX().row();
    }

    private void buildLeaderboard() {
        leaderboardTable = new Table() {
            @Override
            public void act(float delta) {
                super.act(delta);
                float tanAngle = (float) Math.tan(Math.toRadians(3f));

                // 1. Right wall anchor (with 20px padding)
                float rightWallTopX = StatsPanel.this.getWidth() - 20f;

                // 2. Left wall anchor: 10f original + 40f new screen margin = 50f!
                float leftWallX = 50f;

                // 3. Length of the FIRST box, subtracted by a little (5 pixels) to be safe
                float fixedBoxWidth = rightWallTopX - leftWallX - 5f;

                for (com.badlogic.gdx.scenes.scene2d.Actor child : getChildren()) {
                    child.setWidth(fixedBoxWidth);

                    com.badlogic.gdx.math.Vector2 pos = new com.badlogic.gdx.math.Vector2(0, child.getY() + child.getHeight());
                    localToAscendantCoordinates(StatsPanel.this, pos);
                    float distanceDown = StatsPanel.this.getHeight() - pos.y;

                    // The diagonal line sloping left (/)
                    float diagonalLineX = rightWallTopX - (distanceDown * tanAngle);

                    // Anchor the RIGHT edge to the diagonal line
                    child.setX(diagonalLineX - fixedBoxWidth);
                }
            }
        };

        leaderboardTable.top().right();

        leaderboardTable.add(createScoreRow("stelle123", "09946732", "AAA+", "1,004x", "2026-6-7")).expandX().right().padBottom(5).row();
        leaderboardTable.add(createScoreRow("Guest", "08500000", "A", "450x", "2026-6-8")).expandX().right().padBottom(5).row();

        leaderboardScrollPane = new ScrollPane(leaderboardTable, skin);
        leaderboardScrollPane.setScrollingDisabled(true, false);
        leaderboardScrollPane.setFadeScrollBars(false);

        Table scrollContainer = new Table();
        // NO padLeft here! We want the scrolling area to touch the monitor edge
        scrollContainer.add(leaderboardScrollPane).expand().fill().pad(10);

        this.add(scrollContainer).expand().fill().padTop(10);
    }

    private Table createScoreRow(String name, String score, String grade, String combo, String date) {
        Table row = new Table();

        // --- UPDATED: 65% Opacity background for the individual row! ---
        row.background(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.65f)));
        row.pad(10);

        // Left side (Profile Pic placeholder & Name)
        Table profileTable = new Table();
        Image pfp = new Image(skin.newDrawable("white", Color.GRAY)); // Placeholder for avatar
        profileTable.add(pfp).width(50).height(50).padRight(10);

        Table nameDateTable = new Table();
        nameDateTable.add(new Label(name, skin)).align(Align.left).row();
        nameDateTable.add(new Label(date, skin)).align(Align.left);
        profileTable.add(nameDateTable);

        row.add(profileTable).align(Align.left).expandX();

        // Middle (Combo)
        Table comboTable = new Table();
        comboTable.add(new Label("Max Combo", skin)).row();
        comboTable.add(new Label(combo, skin));
        row.add(comboTable).align(Align.center).expandX();

        // Right side (Score & Grade)
        Table scoreTable = new Table();
        Label scoreLabel = new Label(score, skin);
        Label gradeLabel = new Label(grade, skin);

        scoreTable.add(scoreLabel).align(Align.right).row();
        scoreTable.add(gradeLabel).align(Align.right);
        row.add(scoreTable).align(Align.right).expandX();

        return row;
    }

    // --- UPDATED: Directly applies the values to the UI ---
    public void updateSong(String newTitle, String newArtist, String diffText, Color diffColor, String mapperText, String jacketPath, int noteCount, int holdCount, int totalLaserTicks, com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> scores) {
        titleLabel.setText(newTitle);
        artistLabel.setText(newArtist);
        mapperLabel.setText("mapped by " + mapperText);
        diffLabel.setText(diffText);
        diffLabel.setColor(diffColor);

        // --- NEW: Directly set the text! ---
        noteCountLabel.setText("NOTE: " + noteCount);
        holdCountLabel.setText("HOLD: " + holdCount);
        laserCountLabel.setText("LASER: " + totalLaserTicks);

        // --- NEW: Load the Jacket Image ---
        if (jacketTexture != null) {
            jacketTexture.dispose();
            jacketTexture = null;
        }

        if (jacketPath != null) {
            com.badlogic.gdx.files.FileHandle file = Gdx.files.internal(jacketPath);
            if (file.exists()) {
                jacketTexture = new com.badlogic.gdx.graphics.Texture(file);
                jacketImage.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(jacketTexture));
            } else {
                jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY));
            }
        } else {
            jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY));
        }

        currentScores.clear();
        if (scores != null) currentScores.addAll(scores);
        refreshLeaderboard();
    }

    // --- Dedicated drawing method that respects the sort toggle ---
    private void refreshLeaderboard() {
        leaderboardTable.clear();

        if (currentScores.size == 0) {
            Label emptyLabel = new Label("NO RECORDS YET", skin);
            emptyLabel.setColor(com.badlogic.gdx.graphics.Color.GRAY);
            leaderboardTable.add(emptyLabel).pad(20).center();
            return;
        }

        if (sortScoreAscending) {
            currentScores.sort((a, b) -> Integer.compare(b.score, a.score));
        } else {
            currentScores.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        for (com.nodevoltex.game.data.SaveData data : currentScores) {
            String dateStr = sdf.format(new java.util.Date(data.timestamp));
            String scoreStr = String.format("%08d", data.score);

            // --- THE CUTOFF FIX: Changed .fillX() to .right() here too! ---
            leaderboardTable.add(createScoreRow("Guest", scoreStr, data.grade, "---", dateStr))
                .expandX().right().padBottom(5).row();
        }
    }

    public void scroll(float amountY) {
        if (leaderboardScrollPane != null) {
            float newScroll = leaderboardScrollPane.getScrollY() + (amountY * 60f);
            leaderboardScrollPane.setScrollY(newScroll);
        }
    }
}
