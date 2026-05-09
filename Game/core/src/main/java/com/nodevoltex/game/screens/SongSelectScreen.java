package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;

public class SongSelectScreen implements Screen {
    private final NodeVoltex game;
    private final Stage stage;
    private Texture bgTexture;

    public SongSelectScreen(NodeVoltex game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        bgTexture = new Texture(Gdx.files.internal("assets/Back.png"));
        Image bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // 1. Create the panels (Make sure they are in this order!)
        StatsPanel leftPanel = new StatsPanel(NodeVoltex.skin);
        SongListPanel rightPanel = new SongListPanel(game, NodeVoltex.skin, leftPanel);

        // --- PASS THE RIGHT PANEL INTO THE SEARCH BAR ---
        TopSearchBar searchBar = new TopSearchBar(NodeVoltex.skin, rightPanel);

        // 2. Wrap the right side so the search bar sits on top of the list
        Table rightColumn = new Table();
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
        Table backTable = new Table();
        backTable.setFillParent(true);
        backTable.bottom().left(); // Anchor to bottom left

        TextButton backBtn = new TextButton("Back", NodeVoltex.skin);
        // Purple tint to match your mockup
        backBtn.setColor(Color.valueOf("#7E57C2"));
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                rightPanel.stopAudio(); // Stop preview music!
                game.setScreen(new MainMenuScreen(game));
            }
        });

        backTable.add(backBtn).width(150).height(50).pad(20);
        stage.addActor(backTable);

        // --- Auto-focus scrolling and pick a random song on startup ---

        // 1. Give the mouse wheel focus to the right panel immediately
        rightPanel.requestScrollFocus(stage);

        // 2. Pick a random song
        // We wrap this in a postRunnable. This waits exactly 1 frame before firing,
        // which guarantees the Stage has finished calculating your percentage-based layouts.
        // If we don't wait 1 frame, the auto-center math might calculate off of a height of 0!
        Gdx.app.postRunnable(() -> {
            rightPanel.selectRandomSong();
        });
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
    }
}
