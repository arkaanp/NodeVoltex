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

// --- IMPORTS FOR GHOST SCORE ---
import com.nodevoltex.game.data.Beatmap;

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

    // --- THE FIX: Added slideInFromRight flag! ---
    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath, boolean slideInFromRight) {
        this(game, mainMenuMusic, preselectedMapPath, slideInFromRight, null);
    }

    // --- NEW: Constructor that accepts difficulty to restore after ScoreScreen ---
    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath, boolean slideInFromRight, String preselectedDifficulty) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin;
        Gdx.input.setInputProcessor(stage);

        bgTexture = new Texture(Gdx.files.internal("assets/Back.png"));
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        prevBgTexture = new Texture(Gdx.files.internal("assets/background_gaussianblurupscaled.jpeg"));
        prevBgImage = new Image(prevBgTexture);
        prevBgImage.setFillParent(true);
        prevBgImage.setY(Gdx.graphics.getHeight());
        stage.addActor(prevBgImage);

        // --- FIX: Set difficulty BEFORE creating SongListPanel! ---
        if (preselectedDifficulty != null && !preselectedDifficulty.isEmpty()) {
            SongListPanel.setLastPlayedDifficulty(preselectedDifficulty);
        }

        leftPanel = new StatsPanel(NodeVoltex.skin);
        // Link the panel so it can trigger our transitions!
        leftPanel.setParentScreen(this);

        SongListPanel rightPanel = new SongListPanel(game, NodeVoltex.skin, leftPanel, mainMenuMusic);
        TopSearchBar searchBar = new TopSearchBar(NodeVoltex.skin, rightPanel);

        rightColumn = new Table();
        rightColumn.add(searchBar).expandX().fillX().padRight(40).height(60).row();
        rightColumn.add(rightPanel).expand().fill();

        leftLayer = new Table();
        leftLayer.setFillParent(true);
        leftLayer.left();
        leftLayer.add(leftPanel)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.48f, leftLayer))
            .expandY().fillY()
            .padTop(40).padBottom(40);
        stage.addActor(leftLayer);

        rightLayer = new Table();
        rightLayer.setFillParent(true);
        rightLayer.right();
        rightLayer.add(rightColumn)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.48f, rightLayer))
            .expandY().fillY()
            .padTop(40).padBottom(40);
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

        TextButton optionsBtn = new TextButton("Options", NodeVoltex.skin);
        optionsBtn.setColor(Color.valueOf("#4b1d82"));
        optionsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.open();
            }
        });

        backTable.add(backBtn).width(150).height(50).padLeft(20).padBottom(20).padRight(10);
        backTable.add(modsBtn).width(150).height(50).padBottom(20).padRight(10);
        backTable.add(optionsBtn).width(150).height(50).padBottom(20);

        stage.addActor(backTable);

        settingsOverlay = new SettingsOverlay(stage, skin);

        // --- THE FIX: Dynamic Entry Vector ---
        if (slideInFromRight) {
            animateInFromRight();
        } else {
            animateInFromBottom();
        }

        stage.addCaptureListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (settingsOverlay.isOpen()) return false;

                float screenWidth = stage.getWidth();
                if (x < screenWidth / 2f) leftPanel.scroll(amountY);
                else rightPanel.scroll(amountY);
                event.cancel();
                return true;
            }
        });

        Gdx.app.postRunnable(() -> {
            if (preselectedMapPath != null) rightPanel.selectSongByPath(preselectedMapPath);
            else rightPanel.selectRandomSong();
        });
    }

    // --- NEW: Exits Song Select to the right, boots Score Screen ---
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

        // --- THE FIX: Safely parse the number out of the String! ---
        try {
            mockMeta.level = diffText.contains(" ") ? Integer.parseInt(diffText.split(" ")[1]) : 0;
        } catch (NumberFormatException e) {
            mockMeta.level = 0;
        }

        String diffNameOnly = diffText.contains(" ") ? diffText.split(" ")[0] : diffText;

        // Wait 0.8s for the animation to finish, then hand off to ScoreScreen
        stage.addAction(Actions.sequence(
            Actions.delay(0.8f),
            Actions.run(() -> game.setScreen(new ScoreScreen(game, mockMeta, null, data, diffNameOnly, mapPath, true)))
        ));
    }

    // --- NEW: Returning from Score Screen ---
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

        bgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));
        prevBgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));

        leftLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
            Actions.sequence(Actions.delay(0.5f), Actions.alpha(1.0f, 0.5f, Interpolation.linear))
        ));

        rightLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
            Actions.sequence(Actions.delay(0.5f), Actions.alpha(1.0f, 0.5f, Interpolation.linear))
        ));

        backTable.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
                Actions.sequence(Actions.delay(0.5f), Actions.alpha(1.0f, 0.5f, Interpolation.linear))
            ),
            Actions.run(() -> game.setScreen(new MainMenuScreen(game)))
        ));
    }

    @Override public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (settingsOverlay != null) settingsOverlay.resize(width, height);
    }
    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) { Gdx.gl.glClearColor(0, 0, 0, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); stage.act(delta); stage.draw(); }
    @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); if (bgTexture != null) bgTexture.dispose(); prevBgTexture.dispose(); }
}
