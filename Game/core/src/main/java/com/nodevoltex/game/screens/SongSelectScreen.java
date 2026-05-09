package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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

    // --- NEW: Independent Layers replacing rootTable ---
    private Table leftLayer;
    private Table rightLayer;

    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin;
        Gdx.input.setInputProcessor(stage);

        // Background setup
        bgTexture = new Texture(Gdx.files.internal("assets/Back.png"));
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        // Main Menu background
        prevBgTexture = new Texture(Gdx.files.internal("assets/background_gaussianblurupscaled.jpeg"));
        prevBgImage = new Image(prevBgTexture);
        prevBgImage.setFillParent(true);
        prevBgImage.setY(Gdx.graphics.getHeight());
        stage.addActor(prevBgImage);

        // --- CREATE PANELS ---
        leftPanel = new StatsPanel(NodeVoltex.skin);

        SongListPanel rightPanel = new SongListPanel(game, NodeVoltex.skin, leftPanel, mainMenuMusic);
        TopSearchBar searchBar = new TopSearchBar(NodeVoltex.skin, rightPanel);

        rightColumn = new Table();
        rightColumn.add(searchBar).expandX().fillX().height(60).row();
        rightColumn.add(rightPanel).expand().fill();

        // --- INDEPENDENT LEFT LAYER ---
        leftLayer = new Table();
        leftLayer.setFillParent(true);
        leftLayer.left(); // Anchor to the left edge
        leftLayer.add(leftPanel)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.40f, leftLayer))
            .expandY().fillY()
            .padLeft(40).padTop(40).padBottom(40);
        stage.addActor(leftLayer);

        // --- INDEPENDENT RIGHT LAYER ---
        rightLayer = new Table();
        rightLayer.setFillParent(true);
        rightLayer.right(); // Anchor to the right edge
        rightLayer.add(rightColumn)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.45f, rightLayer))
            .expandY().fillY()
            .padRight(40).padTop(40).padBottom(40);
        stage.addActor(rightLayer);

        // --- FLOATING BACK BUTTON ---
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

        backTable.add(backBtn).width(150).height(50).pad(20);
        stage.addActor(backTable);

        // --- TRIGGER ENTRY ANIMATION ---
        animateInFromBottom();

        // --- GLOBAL HOVERLESS SCROLL SYSTEM ---
        stage.addCaptureListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
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

        // --- PICK SELECTED/RANDOM SONG ---
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

        // Target the independent layers!
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

        // Target the independent layers!
        leftLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 0.7f, Interpolation.pow3In),
            Actions.sequence(
                Actions.delay(0.35f),
                Actions.alpha(0.4f, 0.35f, Interpolation.linear)
            )
        ));

        rightLayer.addAction(Actions.sequence(
            Actions.delay(0.05f),
            Actions.parallel(
                Actions.moveBy(0, -h, 0.7f, Interpolation.pow3In),
                Actions.sequence(
                    Actions.delay(0.35f),
                    Actions.alpha(0.4f, 0.35f, Interpolation.linear)
                )
            )
        ));

        backTable.addAction(Actions.sequence(
            Actions.delay(0.1f),
            Actions.parallel(
                Actions.moveBy(0, -h, 0.7f, Interpolation.pow3In),
                Actions.sequence(
                    Actions.delay(0.35f),
                    Actions.alpha(0.4f, 0.35f, Interpolation.linear)
                )
            ),
            Actions.delay(0.2f),
            Actions.run(() -> game.setScreen(new MainMenuScreen(game)))
        ));
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        if (bgTexture != null) bgTexture.dispose();
        prevBgTexture.dispose();
    }
}
