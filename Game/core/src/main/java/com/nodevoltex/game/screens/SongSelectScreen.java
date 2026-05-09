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
import com.badlogic.gdx.utils.Align;
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

    private Table rootTable;

    public SongSelectScreen(NodeVoltex game, com.badlogic.gdx.audio.Music mainMenuMusic, String preselectedMapPath) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin;
        Gdx.input.setInputProcessor(stage);

        // 2. Update the background setup in the constructor
        bgTexture = new Texture(Gdx.files.internal("assets/Back.png"));
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        // --- NEW: Load the Main Menu background and place it directly above ---
        prevBgTexture = new Texture(Gdx.files.internal("assets/background_gaussianblurupscaled.jpeg"));
        prevBgImage = new Image(prevBgTexture);
        prevBgImage.setFillParent(true);
        prevBgImage.setY(Gdx.graphics.getHeight()); // Start above the screen
        stage.addActor(prevBgImage);

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        leftPanel = new StatsPanel(NodeVoltex.skin);

        // --- Pass mainMenuMusic into the right panel ---
        SongListPanel rightPanel = new SongListPanel(game, NodeVoltex.skin, leftPanel, mainMenuMusic);
        TopSearchBar searchBar = new TopSearchBar(NodeVoltex.skin, rightPanel);

        rightColumn = new Table();
        rightColumn.add(searchBar).expandX().fillX().height(60).row();
        rightColumn.add(rightPanel).expand().fill();

        // 3. Add to root table with percentage widths
        rootTable.add(leftPanel)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.40f, rootTable))
            .expandY().fillY()
            .padLeft(40).padTop(40).padBottom(40);

        rootTable.add().expandX(); // Spacer

        rootTable.add(rightColumn)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.45f, rootTable))
            .expandY().fillY()
            .padRight(40).padTop(40).padBottom(40);

        // 4. --- NEW: Floating Back Button ---
        backTable = new Table();
        backTable.setFillParent(true);
        backTable.bottom().left();

        TextButton backBtn = new TextButton("Back", NodeVoltex.skin);
        backBtn.setColor(Color.valueOf("#7E57C2"));
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 1. Lock screen
                stage.getRoot().setTouchable(Touchable.disabled);
                rightPanel.stopAudio();

                // 2. Trigger the reverse Flow animation
                animateOutDownwards();
            }
        });

        backTable.add(backBtn).width(150).height(50).pad(20);
        stage.addActor(backTable);

        // --- NEW: Trigger entry animation flowing UP ---
        animateInFromBottom();

        // --- Auto-focus scrolling and pick a random song on startup ---

        // 1. Give the mouse wheel focus to the right panel immediately
        rightPanel.requestScrollFocus(stage);

        // 2. Pick a random song
        // We wrap this in a postRunnable. This waits exactly 1 frame before firing,
        // which guarantees the Stage has finished calculating your percentage-based layouts.
        // If we don't wait 1 frame, the auto-center math might calculate off of a height of 0!
        //System.out.println("BATON PASS 2 (SongSelect): Received -> " + preselectedMapPath);
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

        // 1. Move only the UI down (Leave bgImage alone!)
        rootTable.addAction(Actions.moveBy(0, -h));
        backTable.addAction(Actions.moveBy(0, -h));

        // 2. Animate the UI up into place over the already-placed background
        rootTable.addAction(Actions.moveBy(0, h, 0.7f, Interpolation.pow4Out));
        backTable.addAction(Actions.sequence(Actions.delay(0.15f), Actions.moveBy(0, h, 0.7f, Interpolation.pow4Out)));
    }

    private void animateOutDownwards() {
        float h = Gdx.graphics.getHeight();

        bgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));
        prevBgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));

        // Animate the entire UI down together
        rootTable.addAction(Actions.moveBy(0, -h, 0.7f, Interpolation.pow4In));

        backTable.addAction(Actions.sequence(
            Actions.delay(0.15f),
            Actions.moveBy(0, -h, 0.7f, Interpolation.pow4In),
            Actions.delay(0.15f),
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
