package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.math.Interpolation;
import com.nodevoltex.game.data.Beatmap;
import com.nodevoltex.game.managers.SettingsManager;

public class SongSelectScreen implements Screen {
    private final NodeVoltex game;
    private final Stage stage;
    private Texture bgTexture;
    private Skin skin;

    private StatsPanel leftPanel;
    private Table rightColumn;
    private Table backTable;
    private Image bgImage;

    private Texture prevBgTexture;
    private Image prevBgImage;

    private Table leftLayer;
    private Table rightLayer;

    private SettingsOverlay settingsOverlay;
    private ModOverlay modOverlay;

    // --- slideInFromRight flag ---
    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath, boolean slideInFromRight) {
        this(game, mainMenuMusic, preselectedMapPath, slideInFromRight, null);
    }

    // --- Constructor that accepts difficulty to restore after ScoreScreen ---
    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath, boolean slideInFromRight, String preselectedDifficulty) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());

        // --- Force the Viewport to instantly snap to your true resolution ---
        this.stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        this.skin = NodeVoltex.skin;

        // --- Reset the global font scale so PlayScreen doesn't make the menu massive ---
        this.skin.getFont("default").getData().setScale(1.0f);

        Gdx.input.setInputProcessor(stage);

        bgTexture = new Texture(Gdx.files.internal("Back.png"));
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        prevBgTexture = new Texture(Gdx.files.internal("background_gaussianblurupscaled.jpeg"));
        prevBgImage = new Image(prevBgTexture);
        prevBgImage.setFillParent(true);
        prevBgImage.setY(Gdx.graphics.getHeight());
        stage.addActor(prevBgImage);

        // --- Set difficulty BEFORE creating SongListPanel ---
        if (preselectedDifficulty != null && !preselectedDifficulty.isEmpty()) {
            SongListPanel.setLastPlayedDifficulty(preselectedDifficulty);
        }

        leftPanel = new StatsPanel(NodeVoltex.skin);
        // Link the panel so it can trigger our transitions
        leftPanel.setParentScreen(this);

        SongListPanel rightPanel = new SongListPanel(game, NodeVoltex.skin, leftPanel, mainMenuMusic);
        TopSearchBar searchBar = new TopSearchBar(NodeVoltex.skin, rightPanel);

        // --- 1. RIGHT COLUMN SETUP ---
        rightColumn = new Table();
        rightColumn.top().right(); // Anchor everything inside the column to the top-right

        // The Search Bar has NO padding, so it is allowed to touch the ceiling
        rightColumn.add(searchBar).expandX().fillX().height(75).row();

        // --- padTop(0) makes the list perfectly touch the search bar ---
        rightColumn.add(rightPanel).expand().fill().padTop(0).padBottom(40);

        leftLayer = new Table();
        leftLayer.setFillParent(true);
        leftLayer.left();
        leftLayer.add(leftPanel)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.48f, leftLayer))
            .expandY().fillY()
            .padTop(40).padBottom(40);
        stage.addActor(leftLayer);

        // --- 2. RIGHT LAYER SETUP ---
        rightLayer = new Table();
        rightLayer.setFillParent(true);

        // Force the layer itself to align to the absolute top-right of the screen
        rightLayer.top().right();

        rightLayer.add(rightColumn)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.48f, rightLayer))
            .expandY().fillY(); // Removed padTop(40) and padBottom(40) from here

        stage.addActor(rightLayer);

        backTable = new Table();
        backTable.setFillParent(true);
        backTable.bottom().left();

        TextButton backBtn = new TextButton("Back", NodeVoltex.skin);
        backBtn.setColor(Color.valueOf("#7E57C2"));
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.getRoot().setTouchable(Touchable.disabled);
                rightPanel.stopAudio();
                animateOutDownwards();
            }
        });

        TextButton modsBtn = new TextButton("Mods", NodeVoltex.skin);
        modsBtn.setColor(Color.valueOf("#7a9e35"));
        // --- Hook up the button to open the overlay ---
        modsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                modOverlay.show();
            }
        });

        TextButton optionsBtn = new TextButton("Options", NodeVoltex.skin);
        optionsBtn.setColor(Color.valueOf("#4b1d82"));
        optionsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.open();
            }
        });

        // --- Dynamic Mod Indicator & Wrapper ---
        Table modIndicatorTable = new Table() {
            // Initialize with the opposite of current settings to force an immediate draw on boot
            private boolean lastAuto = !SettingsManager.getModAutoPlay();
            private boolean lastNoLaser = !SettingsManager.getModNoLaser();

            @Override
            public void act(float delta) {
                super.act(delta);
                boolean currentAuto = SettingsManager.getModAutoPlay();
                boolean currentNoLaser = SettingsManager.getModNoLaser();

                // Only rebuild the text if the player actually toggled a setting in the overlay
                if (currentAuto != lastAuto || currentNoLaser != lastNoLaser) {
                    lastAuto = currentAuto;
                    lastNoLaser = currentNoLaser;

                    clearChildren(); // Erase old text

                    if (currentAuto) {
                        Label autoLbl = new Label("AUTOPLAY", skin);
                        autoLbl.setColor(Color.valueOf("#00E5FF")); // Cyan
                        autoLbl.setFontScale(0.75f);
                        add(autoLbl).padRight(8);
                    }
                    if (currentNoLaser) {
                        Label noLaserLbl = new Label("NO LASERS", skin);
                        noLaserLbl.setColor(Color.valueOf("#FF9100")); // Orange
                        noLaserLbl.setFontScale(0.75f);
                        add(noLaserLbl);
                    }
                }
            }
        };

        // Wrap the button and the indicator together into a single vertical column
        Table modsWrapper = new Table();
        modsWrapper.add(modIndicatorTable).left().padBottom(5).row(); // Indicator sits on the top-left
        modsWrapper.add(modsBtn).width(150).height(50); // Button sits underneath

        // --- Add to Main Table ---
        // Crucial: We add .bottom() to all cells so the buttons stay perfectly flush in a flat line
        // even though the modsWrapper is technically taller now
        backTable.add(backBtn).width(150).height(50).padLeft(20).padBottom(20).padRight(10).bottom();
        backTable.add(modsWrapper).padBottom(20).padRight(10).bottom();
        backTable.add(optionsBtn).width(150).height(50).padBottom(20).bottom();

        stage.addActor(backTable);

        settingsOverlay = new SettingsOverlay(stage, skin);
        modOverlay = new ModOverlay(skin, stage);

        // --- Dynamic Entry Vector ---
        if (slideInFromRight) {
            animateInFromRight();
        } else {
            animateInFromBottom();
        }

        stage.addCaptureListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                // --- Block scrolling if EITHER overlay is open ---
                if (settingsOverlay.isOpen() || modOverlay.isOpen()) return false;

                float screenWidth = stage.getWidth();
                if (x < screenWidth / 2f) leftPanel.scroll(amountY);
                else rightPanel.scroll(amountY);
                event.cancel();
                return true;
            }
        });

        Gdx.app.postRunnable(() -> {
            if (preselectedMapPath != null) {
                if (preselectedDifficulty != null && !preselectedDifficulty.isEmpty()) {
                    SongListPanel.GLOBAL_LAST_PLAYED_DIFFICULTY = preselectedDifficulty;
                }
                rightPanel.selectSongByPath(preselectedMapPath);
            }
            else rightPanel.selectRandomSong();
        });
    }

    // --- Exits Song Select to the right, boots Score Screen ---
    public void transitionToScoreScreenFromHistory(com.nodevoltex.game.data.SaveData data, String title, String artist, String mapperText, String diffText, String mapPath) {
        stage.getRoot().setTouchable(Touchable.disabled);
        float w = stage.getWidth();

        // Slide everything OUT to the right smoothly
        leftLayer.addAction(Actions.moveBy(w, 0, 0.6f, Interpolation.pow3In));
        rightLayer.addAction(Actions.sequence(Actions.delay(0.05f), Actions.moveBy(w, 0, 0.6f, Interpolation.pow3In)));
        backTable.addAction(Actions.sequence(Actions.delay(0.1f), Actions.moveBy(w, 0, 0.6f, Interpolation.pow3In)));

        // Package up the basic string data into a mock Metadata object for the ScoreScreen to read
        Beatmap.General mockMeta = new Beatmap.General();
        mockMeta.title = title;
        mockMeta.artist = artist;
        mockMeta.mapper = mapperText.replace("mapped by ", "");

        // --- Safely parse the number out of the String ---
        try {
            mockMeta.level = diffText.contains(" ") ? Integer.parseInt(diffText.split(" ")[1]) : 0;
        } catch (NumberFormatException e) {
            mockMeta.level = 0;
        }

        String diffNameOnly = diffText.contains(" ") ? diffText.split(" ")[0] : diffText;

        // Wait 0.8s for the animation to finish, then hand off to ScoreScreen
        stage.addAction(Actions.sequence(
            Actions.delay(0.8f),
            // --- Pass null for the fresh replay JSON string since we are loading from history ---
            Actions.run(() -> game.setScreen(new ScoreScreen(game, mockMeta, null, data, diffNameOnly, mapPath, true, data.timestamp, null)))
        ));
    }

    // --- Returning from Score Screen ---
    private void animateInFromRight() {
        float w = stage.getWidth();

        // Start everything off-screen to the right
        leftLayer.setX(w);
        rightLayer.setX(w);
        backTable.setX(w);

        // Slide everything in smoothly to 0 (moving left)
        leftLayer.addAction(Actions.moveBy(-w, 0, 0.6f, Interpolation.pow3Out));
        rightLayer.addAction(Actions.sequence(Actions.delay(0.05f), Actions.moveBy(-w, 0, 0.6f, Interpolation.pow3Out)));
        backTable.addAction(Actions.sequence(Actions.delay(0.1f), Actions.moveBy(-w, 0, 0.6f, Interpolation.pow3Out)));
    }

    private void animateInFromBottom() {
        float h = Gdx.graphics.getHeight();

        leftLayer.addAction(Actions.moveBy(0, -h));
        rightLayer.addAction(Actions.moveBy(0, -h));
        backTable.addAction(Actions.moveBy(0, -h));

        leftLayer.getColor().a = 0f;
        rightLayer.getColor().a = 0f;
        backTable.getColor().a = 0f;

        leftLayer.addAction(Actions.sequence(
            Actions.delay(0.05f),
            Actions.parallel(Actions.moveBy(0, h, 0.9f, Interpolation.pow3Out), Actions.fadeIn(0.7f, Interpolation.pow2Out))
        ));

        rightLayer.addAction(Actions.sequence(
            Actions.delay(0.20f),
            Actions.parallel(Actions.moveBy(0, h, 0.9f, Interpolation.pow3Out), Actions.fadeIn(0.8f, Interpolation.pow2Out))
        ));

        backTable.addAction(Actions.sequence(
            Actions.delay(0.25f),
            Actions.parallel(Actions.moveBy(0, h, 0.8f, Interpolation.pow3Out), Actions.fadeIn(0.5f, Interpolation.pow2Out))
        ));
    }

    private void animateOutDownwards() {
        float h = Gdx.graphics.getHeight();

        // Backgrounds slide down seamlessly alongside the UI
        bgImage.addAction(Actions.moveBy(0, -h, 1.1f, Interpolation.pow3In));
        prevBgImage.addAction(Actions.moveBy(0, -h, 1.1f, Interpolation.pow3In));

        // 1. Back Table drops FIRST (Since it came in last)
        backTable.addAction(Actions.sequence(
            Actions.delay(0.0f),
            Actions.parallel(Actions.moveBy(0, -h, 0.8f, Interpolation.pow3In), Actions.fadeOut(0.5f, Interpolation.pow2In))
        ));

        // 2. Right Layer drops SECOND
        rightLayer.addAction(Actions.sequence(
            Actions.delay(0.05f),
            Actions.parallel(Actions.moveBy(0, -h, 0.9f, Interpolation.pow3In), Actions.fadeOut(0.8f, Interpolation.pow2In))
        ));

        // 3. Left Layer drops LAST
        leftLayer.addAction(Actions.sequence(
            Actions.delay(0.10f),
            Actions.parallel(Actions.moveBy(0, -h, 0.9f, Interpolation.pow3In), Actions.fadeOut(0.7f, Interpolation.pow2In))
        ));

        // 4. Detach the screen switch and tie it to the exact maximum duration
        stage.addAction(Actions.sequence(
            Actions.delay(1.1f), // Matches the background's 1.1f exactly
            Actions.run(() -> game.setScreen(new MainMenuScreen(game)))
        ));
    }


    // --- Slide left to enter PlayScreen ---
    public void animateOutToPlayScreen(final String mapPath) {
        stage.getRoot().setTouchable(Touchable.disabled);
        float w = stage.getWidth();

        // Slide everything OUT to the left smoothly
        leftLayer.addAction(Actions.moveBy(-w, 0, 0.6f, Interpolation.pow3In));
        rightLayer.addAction(Actions.sequence(Actions.delay(0.05f), Actions.moveBy(-w, 0, 0.6f, Interpolation.pow3In)));
        backTable.addAction(Actions.sequence(Actions.delay(0.1f), Actions.moveBy(-w, 0, 0.6f, Interpolation.pow3In)));

        // Wait 0.8s for the animation to finish, then hand off to PlayScreen
        stage.addAction(Actions.sequence(
            Actions.delay(0.8f),
            Actions.run(() -> game.setScreen(new PlayScreen(game, mapPath)))
        ));
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (settingsOverlay != null) settingsOverlay.resize(width, height);
        if (modOverlay != null) modOverlay.resize(width, height);
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- Force the OpenGL matrix to snap back to the UI camera ---
        stage.getViewport().apply();

        stage.act(delta);
        stage.draw();
    }
    @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); if (bgTexture != null) bgTexture.dispose(); prevBgTexture.dispose(); }
}
