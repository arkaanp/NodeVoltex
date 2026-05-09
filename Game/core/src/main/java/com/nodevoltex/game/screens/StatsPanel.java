package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

public class StatsPanel extends Table {
    private final Skin skin;

    // UI Elements we will want to update later when a song is clicked
    private Label titleLabel;
    private Label artistLabel;
    private Label mapperLabel;
    private Label diffLabel;
    private Table leaderboardTable;
    private com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> currentScores = new com.badlogic.gdx.utils.Array<>();
    private boolean sortScoreAscending = true; // true = score, false = date
    private ScrollPane leaderboardScrollPane;

    public StatsPanel(Skin skin) {
        this.skin = skin;
        this.top().left(); // Anchor everything to the top left

        buildHeader();
        buildSortingToggles();
        buildLeaderboard();
    }

    private void buildHeader() {
        // 1. Create the Stack to layer text over the background
        Stack headerStack = new Stack();

        // 2. The Background Layer (The Slanted Pink Box)
        // NOTE: For now, it uses a solid pink color. To get the slanted look from your mockup,
        // replace this with an Image loaded from a transparent .png of a slanted pink shape.
        Image headerBg = new Image(skin.newDrawable("white", new Color(1f, 0.2f, 0.6f, 0.9f)));
        headerStack.add(headerBg);

        // 3. The Text Layer
        Table textTable = new Table();
        textTable.top().left().pad(20);

        // Initialize labels with placeholder text (we'll update these dynamically later)
        titleLabel = new Label("magical, very magical world", skin);
        // If you have a larger font, use it here: new Label("...", skin, "large-font", Color.BLACK);
        titleLabel.setColor(Color.BLACK);

        artistLabel = new Label("Camellia", skin);
        artistLabel.setColor(Color.DARK_GRAY);

        // Sub-info (Difficulty, Mapper)
        Table subInfoTable = new Table();
        diffLabel = new Label("EXH 17", skin);
        diffLabel.setColor(Color.RED);
        mapperLabel = new Label("mapped by Sotarks", skin);
        mapperLabel.setColor(Color.valueOf("#4A148C")); // Deep Purple

        subInfoTable.add(diffLabel).padRight(10);
        subInfoTable.add(mapperLabel);

        // Add everything to the text table
        textTable.add(titleLabel).align(Align.left).row();
        textTable.add(artistLabel).align(Align.left).padBottom(5).row();
        textTable.add(subInfoTable).align(Align.left);

        // Add the text table on top of the background image in the stack
        headerStack.add(textTable);

        // Add the finished stack to the main panel
        this.add(headerStack).expandX().fillX().height(150).row();
    }

    private void buildSortingToggles() {
        Table toggleTable = new Table();
        toggleTable.left().pad(10);
        toggleTable.background(skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.4f)));

        toggleTable.add(new Label("sorted by: ", skin)).padRight(5);

        // --- NEW: Clickable Score Sorting Buttons ---
        TextButton scoreBtn = new TextButton("score", skin);
        TextButton dateBtn = new TextButton("date", skin);

        scoreBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                sortScoreAscending = true;
                refreshLeaderboard();
            }
        });

        dateBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                sortScoreAscending = false;
                refreshLeaderboard();
            }
        });

        toggleTable.add(scoreBtn).padRight(5);
        toggleTable.add(dateBtn).padRight(20);

        toggleTable.add(new Label("scope: ", skin)).padRight(5);
        toggleTable.add(new TextButton("local", skin)).padRight(5);
        toggleTable.add(new TextButton("global", skin));

        this.add(toggleTable).expandX().fillX().row();
    }

    private void buildLeaderboard() {
        // --- NEW: 3-Degree Diagonal Table scaled to the 8th Box ---
        leaderboardTable = new Table() {
            @Override
            public void act(float delta) {
                super.act(delta);
                float tanAngle = (float) Math.tan(Math.toRadians(3f));

                // 1. Define the starting anchors at the very top
                float rightWallTopX = StatsPanel.this.getWidth() - 20f;
                float leftWallX = 10f; // Left side of "sorted by"

                // 2. Calculate the theoretical depth of the 8th box.
                // A standard row with a profile pic is usually ~70px tall + 5px padding.
                float estimatedRowHeight = 75f;
                float depthOf8thBox = 8f * estimatedRowHeight;

                // 3. Find exactly where the 3-degree diagonal line is at that depth
                float rightWall8thBoxX = rightWallTopX - (depthOf8thBox * tanAngle);

                // 4. Lock the width! (Rightmost X of 8th box - Leftmost X of sorted by)
                // We subtract 5f as a tiny safety margin so it never visually touches the edge.
                float safetyMargin = 5f;
                float fixedBoxWidth = rightWall8thBoxX - leftWallX - safetyMargin;

                for (com.badlogic.gdx.scenes.scene2d.Actor child : getChildren()) {
                    // 5. Force every single box to be this exact width
                    child.setWidth(fixedBoxWidth);

                    // Find where the TOP of this specific row is vertically
                    com.badlogic.gdx.math.Vector2 pos = new com.badlogic.gdx.math.Vector2(0, child.getY() + child.getHeight());
                    localToAscendantCoordinates(StatsPanel.this, pos);

                    float distanceDown = StatsPanel.this.getHeight() - pos.y;

                    // 6. The diagonal line sloping left (/)
                    float diagonalLineX = rightWallTopX - (distanceDown * tanAngle);

                    // 7. Anchor the RIGHT edge of the box to the diagonal line
                    child.setX(diagonalLineX - fixedBoxWidth);
                }
            }
        };

        leaderboardTable.top().right();

        // Placeholder scores (Notice we use .right() instead of .fillX() now!)
        leaderboardTable.add(createScoreRow("stelle123", "09946732", "AAA+", "1,004x", "2026-6-7")).expandX().right().padBottom(5).row();
        leaderboardTable.add(createScoreRow("Guest", "08500000", "A", "450x", "2026-6-8")).expandX().right().padBottom(5).row();

        leaderboardScrollPane = new ScrollPane(leaderboardTable, skin);
        leaderboardScrollPane.setScrollingDisabled(true, false);
        leaderboardScrollPane.setFadeScrollBars(false);

        Table scrollContainer = new Table();
        scrollContainer.add(leaderboardScrollPane).expand().fill().pad(10); // Update this line too!

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
    public void updateSong(String newTitle, String newArtist, String diffText, String mapperText, com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> scores) {
        titleLabel.setText(newTitle);
        artistLabel.setText(newArtist);
        mapperLabel.setText("mapped by " + mapperText);
        diffLabel.setText(diffText);
        diffLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);

        // Save the loaded scores into our state variable
        currentScores.clear();
        if (scores != null) {
            currentScores.addAll(scores);
        }

        // Draw the leaderboard
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
