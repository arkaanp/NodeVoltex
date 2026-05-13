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

    // --- NEW: The Clean Settings Overlay Manager ---
    private SettingsOverlay settingsOverlay;

    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath) {
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

        leftPanel = new StatsPanel(NodeVoltex.skin);

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
        modsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Mods button clicked");
            }
        });

        TextButton optionsBtn = new TextButton("Options", NodeVoltex.skin);
        optionsBtn.setColor(Color.valueOf("#4b1d82"));
        optionsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Delegate to our new class
                settingsOverlay.open();
            }
        });

        backTable.add(backBtn).width(150).height(50).padLeft(20).padBottom(20).padRight(10);
        backTable.add(modsBtn).width(150).height(50).padBottom(20).padRight(10);
        backTable.add(optionsBtn).width(150).height(50).padBottom(20);

        stage.addActor(backTable);

        // Instantiating the overlay AFTER other UI ensures it draws on top
        settingsOverlay = new SettingsOverlay(stage, skin);

        animateInFromBottom();

        stage.addCaptureListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                // Check state through the manager
                if (settingsOverlay.isOpen()) {
                    return false;
                }

                float screenWidth = stage.getWidth();
                if (x < screenWidth / 2f) {
                    leftPanel.scroll(amountY);
                } else {
                    rightPanel.scroll(amountY);
                }
                event.cancel();
                return true;
            }
        });

        Gdx.app.postRunnable(() -> {
            if (preselectedMapPath != null) {
                rightPanel.selectSongByPath(preselectedMapPath);
            } else {
                rightPanel.selectRandomSong();
            }
        });
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
            Actions.parallel(
                Actions.moveBy(0, h, 0.9f, Interpolation.pow3Out),
                Actions.fadeIn(0.7f, Interpolation.pow2Out)
            )
        ));

        rightLayer.addAction(Actions.sequence(
            Actions.delay(0.20f),
            Actions.parallel(
                Actions.moveBy(0, h, 0.9f, Interpolation.pow3Out),
                Actions.fadeIn(0.8f, Interpolation.pow2Out)
            )
        ));

        backTable.addAction(Actions.sequence(
            Actions.delay(0.25f),
            Actions.parallel(
                Actions.moveBy(0, h, 0.8f, Interpolation.pow3Out),
                Actions.fadeIn(0.5f, Interpolation.pow2Out)
            )
        ));
    }

    private void animateOutDownwards() {
        float h = Gdx.graphics.getHeight();

        bgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));
        prevBgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));

        leftLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
            Actions.sequence(
                Actions.delay(0.5f),
                Actions.alpha(1.0f, 0.5f, Interpolation.linear)
            )
        ));

        rightLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
            Actions.sequence(
                Actions.delay(0.5f),
                Actions.alpha(1.0f, 0.5f, Interpolation.linear)
            )
        ));

        backTable.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
                Actions.sequence(
                    Actions.delay(0.5f),
                    Actions.alpha(1.0f, 0.5f, Interpolation.linear)
                )
            ),
            Actions.run(() -> game.setScreen(new MainMenuScreen(game)))
        ));
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // Let the overlay handle its own dynamic resizing logic
        if (settingsOverlay != null) {
            settingsOverlay.resize(width, height);
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
        prevBgTexture.dispose();
    }
}
