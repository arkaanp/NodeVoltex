package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class StatsPanel extends Table {
    private final Skin skin;

    private SongSelectScreen parentScreen;
    public String currentMapPath = "";

    private Label titleLabel;
    private Label artistLabel;
    private Label mapperLabel;
    private Label diffLabel;
    private Table leaderboardTable;
    private ScrollPane leaderboardScrollPane;
    private com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> currentScores = new com.badlogic.gdx.utils.Array<>();

    private boolean sortScoreAscending = true;

    // --- Tab State Memory Variables ---
    private String activeSortTab = "score";
    private String activeScopeTab = "local";

    private Image jacketImage;
    private com.badlogic.gdx.graphics.Texture jacketTexture;
    private String currentJacketPath = "";

    private Label noteCountLabel;
    private Label holdCountLabel;
    private Label laserCountLabel;

    public StatsPanel(Skin skin) {
        this.skin = skin;
        this.top().left();
        buildTopSection();
        buildLeaderboard();
        skin.getFont("default").getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public void setParentScreen(SongSelectScreen screen) {
        this.parentScreen = screen;
    }

    private void buildTopSection() {
        Stack topStack = new Stack();
        Table backgroundsTable = new Table();
        backgroundsTable.top().left();

        Stack headerStack = new Stack();
        Image headerBg = new Image(skin.newDrawable("white", new Color(1f, 0.2f, 0.6f, 0.9f)));
        headerStack.add(headerBg);

        Table textTable = new Table();
        textTable.top().left().pad(15);

        titleLabel = new Label("magical, very magical world", skin);
        titleLabel.setColor(Color.BLACK);
        titleLabel.setFontScale(1f);

        artistLabel = new Label("Camellia", skin);
        artistLabel.setColor(Color.DARK_GRAY);
        artistLabel.setFontScale(1f);

        Table subInfoTable = new Table();
        diffLabel = new Label("EXH 17", skin);
        diffLabel.setColor(Color.RED);
        diffLabel.setFontScale(1f);

        mapperLabel = new Label("mapped by Sotarks", skin);
        mapperLabel.setColor(Color.valueOf("#4A148C"));
        mapperLabel.setFontScale(1f);

        subInfoTable.add(diffLabel).padRight(10);
        subInfoTable.add(mapperLabel);

        Table objectStatsTable = new Table();
        noteCountLabel = new Label("NOTE: 0", skin);
        holdCountLabel = new Label("HOLD: 0", skin);
        laserCountLabel = new Label("LASER: 0", skin);

        noteCountLabel.setColor(Color.DARK_GRAY); holdCountLabel.setColor(Color.DARK_GRAY); laserCountLabel.setColor(Color.DARK_GRAY);
        noteCountLabel.setFontScale(0.8f); holdCountLabel.setFontScale(0.8f); laserCountLabel.setFontScale(0.8f);

        objectStatsTable.add(noteCountLabel).padRight(15);
        objectStatsTable.add(holdCountLabel).padRight(15);
        objectStatsTable.add(laserCountLabel);

        textTable.add(titleLabel).align(Align.left).row();
        textTable.add(artistLabel).align(Align.left).padBottom(2).row();
        textTable.add(subInfoTable).align(Align.left).padBottom(2).row();
        textTable.add(objectStatsTable).align(Align.left);

        headerStack.add(textTable);
        backgroundsTable.add(headerStack).expandX().fillX().padLeft(40).height(150).row();

        // --- Custom Underline Tabs ---
        Table toggleTable = new Table();
        toggleTable.left().pad(10);
        toggleTable.background(skin.newDrawable("white", new Color(0, 0, 0, 0.4f)));

        Label sortedByLabel = new Label("sorted by: ", skin);
        sortedByLabel.setFontScale(0.85f);
        sortedByLabel.setColor(Color.LIGHT_GRAY);
        toggleTable.add(sortedByLabel).padRight(10);

        Table scoreBtn = createStatsTab("score", "sort", () -> { sortScoreAscending = true; refreshLeaderboard(false); });
        Table dateBtn = createStatsTab("date", "sort", () -> { sortScoreAscending = false; refreshLeaderboard(false); });

        toggleTable.add(scoreBtn).padRight(10);
        toggleTable.add(dateBtn).padRight(30);

        Label scopeLabel = new Label("scope: ", skin);
        scopeLabel.setFontScale(0.85f);
        scopeLabel.setColor(Color.LIGHT_GRAY);
        toggleTable.add(scopeLabel).padRight(10);

        Table localBtn = createStatsTab("local", "scope", null);
        Table globalBtn = createStatsTab("global", "scope", null);

        toggleTable.add(localBtn).padRight(10);
        toggleTable.add(globalBtn);

        backgroundsTable.add(toggleTable).expandX().fillX().padLeft(40).height(50).row();
        topStack.add(backgroundsTable);

        Table jacketLayer = new Table();
        jacketLayer.top().right();
        jacketImage = new Image();
        jacketLayer.add(jacketImage).width(180).height(180).padTop(10).padRight(10);
        topStack.add(jacketLayer);

        this.add(topStack).expandX().fillX().row();
    }

    // --- Custom Animated Tab Builder ---
    private Table createStatsTab(final String text, final String group, final Runnable onClick) {
        final Table tab = new Table();
        tab.setTouchable(Touchable.enabled);

        final Label label = new Label(text, skin);
        label.setFontScale(0.85f);

        // White underline for dark background
        final Image underline = new Image(skin.newDrawable("white", Color.WHITE));

        tab.add(label).padBottom(2).row();
        tab.add(underline).growX().height(2);

        final ClickListener listener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (group.equals("sort")) activeSortTab = text;
                else if (group.equals("scope")) activeScopeTab = text;

                if (onClick != null) onClick.run();
            }
        };
        tab.addListener(listener);

        tab.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                boolean isActive = group.equals("sort") ? activeSortTab.equals(text) : activeScopeTab.equals(text);
                if (isActive) {
                    underline.getColor().a = 1.0f;
                    label.setColor(Color.WHITE);
                } else if (listener.isOver()) {
                    underline.getColor().a = 0.4f;
                    label.setColor(Color.LIGHT_GRAY);
                } else {
                    underline.getColor().a = 0.0f;
                    label.setColor(Color.LIGHT_GRAY);
                }
                return false;
            }
        });

        return tab;
    }

    private void buildLeaderboard() {
        leaderboardTable = new Table() {
            @Override
            public void act(float delta) {
                super.act(delta);
                float tanAngle = (float) Math.tan(Math.toRadians(3f));
                float rightWallTopX = StatsPanel.this.getWidth() - 20f;
                float leftWallX = 50f;
                float fixedBoxWidth = rightWallTopX - leftWallX - 5f;

                for (com.badlogic.gdx.scenes.scene2d.Actor child : getChildren()) {
                    child.setWidth(fixedBoxWidth);
                    com.badlogic.gdx.math.Vector2 pos = new com.badlogic.gdx.math.Vector2(0, child.getY() + child.getHeight());
                    localToAscendantCoordinates(StatsPanel.this, pos);
                    float distanceDown = StatsPanel.this.getHeight() - pos.y;
                    float targetX = (rightWallTopX - (distanceDown * tanAngle)) - fixedBoxWidth;

                    if (child.getUserObject() instanceof Float) {
                        targetX -= (Float) child.getUserObject();
                    }
                    child.setX(targetX);
                }
            }
        };

        leaderboardTable.top().right();
        leaderboardScrollPane = new ScrollPane(leaderboardTable, skin);
        leaderboardScrollPane.setScrollingDisabled(true, false);
        leaderboardScrollPane.setFadeScrollBars(false);

        Table scrollContainer = new Table();
        scrollContainer.add(leaderboardScrollPane).expand().fill().pad(10);
        this.add(scrollContainer).expand().fill().padTop(10);
    }

    public void updateSong(String newTitle, String newArtist, String diffText, Color diffColor, String mapperText, String jacketPath, int noteCount, int holdCount, int totalLaserTicks) {
        titleLabel.setText(newTitle); artistLabel.setText(newArtist); mapperLabel.setText("mapped by " + mapperText);
        diffLabel.setText(diffText); diffLabel.setColor(diffColor);
        noteCountLabel.setText("NOTE: " + noteCount); holdCountLabel.setText("HOLD: " + holdCount); laserCountLabel.setText("LASER: " + totalLaserTicks);
        String oldJacket = currentJacketPath;
        currentJacketPath = jacketPath != null ? jacketPath : "";

        if (jacketPath == null || jacketPath.isEmpty()) {
            jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY));
        } else if (jacketPath.equals(oldJacket) && jacketTexture != null) {
            // already loaded
        } else {
            jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY));

            Thread jacketThread = new Thread(() -> {
                try { Thread.sleep(60); } catch (Exception e) {}
                com.badlogic.gdx.files.FileHandle file = Gdx.files.internal(jacketPath);
                if (file.exists()) {
                    try {
                        final com.badlogic.gdx.graphics.Pixmap src = new com.badlogic.gdx.graphics.Pixmap(file);
                        final int targetMax = 180;
                        int srcW = src.getWidth();
                        int srcH = src.getHeight();
                        int dstW = srcW;
                        int dstH = srcH;
                        if (srcW > targetMax || srcH > targetMax) {
                            float ratio = (float) srcW / (float) srcH;
                            if (srcW >= srcH) { dstW = targetMax; dstH = Math.max(1, Math.round(targetMax / ratio)); }
                            else { dstH = targetMax; dstW = Math.max(1, Math.round(targetMax * ratio)); }
                        }

                        final com.badlogic.gdx.graphics.Pixmap scaled = new com.badlogic.gdx.graphics.Pixmap(dstW, dstH, src.getFormat());
                        scaled.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
                        scaled.drawPixmap(src, 0, 0, srcW, srcH, 0, 0, dstW, dstH);
                        src.dispose();

                        Gdx.app.postRunnable(() -> {
                            if (currentJacketPath.equals(jacketPath)) {
                                try {
                                    if (jacketTexture != null) jacketTexture.dispose();
                                    jacketTexture = new com.badlogic.gdx.graphics.Texture(scaled);
                                    jacketImage.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(jacketTexture));
                                } catch (Exception e) {
                                    jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY));
                                } finally {
                                    scaled.dispose();
                                }
                            } else {
                                scaled.dispose();
                            }
                        });
                    } catch (Exception e) {
                        Gdx.app.postRunnable(() -> { if (currentJacketPath.equals(jacketPath)) jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY)); });
                    }
                } else {
                    Gdx.app.postRunnable(() -> { if (currentJacketPath.equals(jacketPath)) jacketImage.setDrawable(skin.newDrawable("white", Color.DARK_GRAY)); });
                }
            });
            jacketThread.setPriority(Thread.MIN_PRIORITY);
            jacketThread.start();
        }
    }

    public void injectScoresAsync(com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> scores, boolean animateScores) {
        currentScores.clear();
        if (scores != null) currentScores.addAll(scores);
        refreshLeaderboard(animateScores);
    }

    private void refreshLeaderboard(boolean animate) {
        leaderboardTable.clear();
        if (currentScores.size == 0) {
            Label emptyLabel = new Label("NO RECORDS YET", skin);
            emptyLabel.setColor(Color.GRAY);
            leaderboardTable.add(emptyLabel).pad(20).center();
            return;
        }

        if (sortScoreAscending) currentScores.sort((a, b) -> Integer.compare(b.score, a.score));
        else currentScores.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        int index = 0;
        float totalCascadeCap = 0.6f;
        float baseDelay = totalCascadeCap / Math.max(1, currentScores.size);
        baseDelay = Math.max(0.02f, Math.min(0.08f, baseDelay));

        for (com.nodevoltex.game.data.SaveData data : currentScores) {
            String dateStr = sdf.format(new java.util.Date(data.timestamp));
            String scoreStr = String.format("%08d", data.score);
            leaderboardTable.add(createAnimatedScoreRow(data, "Guest", scoreStr, data.grade, data.maxCombo + "x", dateStr, index, animate, baseDelay))
                .expandX().right().padBottom(5).row();
            index++;
        }
    }

    public void scroll(float amountY) {
        if (leaderboardScrollPane != null) {
            float newScroll = leaderboardScrollPane.getScrollY() + (amountY * 60f);
            leaderboardScrollPane.setScrollY(newScroll);
        }
    }

    private Table createAnimatedScoreRow(final com.nodevoltex.game.data.SaveData data, String name, String score, String grade, String combo, String date, int delayIndex, boolean animate, float baseDelay) {
        final Table row = new Table();

        final com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.65f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 0.85f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable clickBg = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.35f, 1f));

        row.setBackground(normalBg);
        row.setTouchable(Touchable.enabled);

        final ClickListener listener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (parentScreen != null) {
                    parentScreen.transitionToScoreScreenFromHistory(data, titleLabel.getText().toString(), artistLabel.getText().toString(), mapperLabel.getText().toString(), diffLabel.getText().toString(), currentMapPath);
                }
            }
        };
        row.addListener(listener);

        row.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (listener.isVisualPressed()) row.setBackground(clickBg);
                else if (listener.isOver()) row.setBackground(hoverBg);
                else row.setBackground(normalBg);
                return false;
            }
        });

        if (animate) {
            row.getColor().a = 0.5f;
            row.setUserObject(2500f);

            row.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                float time = 0;
                float delay = delayIndex * baseDelay;
                float duration = Math.max(0.12f, 0.4f * (1f - Math.min(0.8f, (currentScores.size / 30f))));
                @Override
                public boolean act(float delta) {
                    float safeDelta = Math.min(delta, 0.03f);
                    if (delay > 0) { delay -= safeDelta; return false; }

                    time += safeDelta;
                    float progress = com.badlogic.gdx.math.Interpolation.pow3Out.apply(Math.min(time / duration, 1f));

                    row.getColor().a = 0.5f + (0.5f * progress);
                    row.setUserObject(2500f * (1f - progress));

                    return time >= duration;
                }
            });
        } else {
            row.getColor().a = 1.0f;
            row.setUserObject(0f);
        }

        Table profileTable = new Table();
        Image pfp = new Image(skin.newDrawable("white", Color.GRAY));
        profileTable.add(pfp).width(50).height(50).padRight(10);

        Table nameDateTable = new Table();
        nameDateTable.add(new Label(name, skin)).align(Align.left).row();
        nameDateTable.add(new Label(date, skin)).align(Align.left);
        profileTable.add(nameDateTable);

        row.add(profileTable).align(Align.left).expandX();

        Table comboTable = new Table();
        comboTable.add(new Label("Max Combo", skin)).row();
        comboTable.add(new Label(combo, skin));
        row.add(comboTable).align(Align.center).expandX();

        Table scoreTable = new Table();
        Label scoreLabel = new Label(score, skin);
        Label gradeLabel = new Label(grade, skin);

        scoreTable.add(scoreLabel).align(Align.right).row();
        scoreTable.add(gradeLabel).align(Align.right);
        row.add(scoreTable).align(Align.right).expandX().padRight(20);

        return row;
    }
}
