package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;

public class MainMenuScreen implements Screen {

    private final NodeVoltex game;
    private final Stage stage;

    // --- NEW: Graphic Textures ---
    private Texture bgTexture;
    private Texture titleTexture;

    public MainMenuScreen(NodeVoltex game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 1. Load your images from the assets folder
        bgTexture = new Texture(Gdx.files.internal("assets/background_gaussianblurupscaled.jpeg"));
        titleTexture = new Texture(Gdx.files.internal("assets/title.png"));

        Table rootTable = new Table();
        rootTable.setFillParent(true);

        // 2. Set the background image of the entire screen
        rootTable.setBackground(new TextureRegionDrawable(new TextureRegion(bgTexture)));
        stage.addActor(rootTable);

        // 3. Create the Title Image
        Image titleImage = new Image(titleTexture);

        // Optional: If your title image is too big, you can scale it down
        // titleImage.setScaling(com.badlogic.gdx.utils.Scaling.fit);

        // Add the Title Image to the top of the screen
        rootTable.add(titleImage).padBottom(50).row();

        // 4. Create your Menu Buttons
        TextButton.TextButtonStyle btnStyle = NodeVoltex.skin.get("default", TextButton.TextButtonStyle.class);

        TextButton playBtn = new TextButton("START", btnStyle);
        TextButton exitBtn = new TextButton("EXIT", btnStyle);

        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SongSelectScreen(game));
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Add the buttons below the title
        rootTable.add(playBtn).width(300).height(60).padBottom(20).row();
        rootTable.add(exitBtn).width(300).height(60).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        // --- CRITICAL: Always dispose of textures to prevent memory leaks! ---
        bgTexture.dispose();
        titleTexture.dispose();
    }
}
