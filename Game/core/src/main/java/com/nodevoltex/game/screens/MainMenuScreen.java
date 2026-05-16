package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
    private Music music;

    private Texture bgTexture;
    private Texture titleTexture;

    // --- Class fields for animation targets ---
    private Image titleImage;
    private TextButton playBtn;
    private TextButton exitBtn;
    private Table rootTable;
    private Image bgImage;
    private Texture nextBgTexture;
    private Image nextBgImage;

    public MainMenuScreen(NodeVoltex game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 1. LOAD ALL TEXTURES FIRST
        // If any of these are missing or below the 'new Image()' lines, the game will crash!
        bgTexture = new Texture(Gdx.files.internal("background_gaussianblurupscaled.jpeg"));
        nextBgTexture = new Texture(Gdx.files.internal("Back.png"));
        titleTexture = new Texture(Gdx.files.internal("title4.png"));

        // 2. CREATE IMAGES USING THOSE TEXTURES SECOND
        bgImage = new Image(bgTexture);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);

        nextBgImage = new Image(nextBgTexture);
        nextBgImage.setFillParent(true);
        nextBgImage.setY(-Gdx.graphics.getHeight());
        stage.addActor(nextBgImage);

        // 3. SET UP YOUR UI TABLES LAST
        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        titleImage = new Image(titleTexture);
        rootTable.add(titleImage).padBottom(50).row();

        // Audio
        com.badlogic.gdx.files.FileHandle audioFile = Gdx.files.internal("worldender.ogg");
        try {
            if (audioFile.exists()) {
                music = Gdx.audio.newMusic(audioFile);
                music.setVolume(com.nodevoltex.game.managers.SettingsManager.getMasterVolume() * com.nodevoltex.game.managers.SettingsManager.getMusicVolume());
                music.setLooping(true);
                music.play();
            }
        } catch (Exception e) {
            System.out.println("CRITICAL: Could not load gameplay audio!");
        }

        TextButton.TextButtonStyle btnStyle = NodeVoltex.skin.get("default", TextButton.TextButtonStyle.class);
        playBtn = new TextButton("START", btnStyle);
        exitBtn = new TextButton("EXIT", btnStyle);

        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.getRoot().setTouchable(Touchable.disabled);

                // --- REMOVED THE MUSIC.STOP() ---

                animateOutUpwards();
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        rootTable.add(playBtn).width(300).height(60).padBottom(20).row();
        rootTable.add(exitBtn).width(300).height(60).row();

        // --- Trigger entry animation when screen is created ---
        // If coming back from SongSelect, we drop down from the top.
        animateInFromTop();
    }

    // --- ANIMATION LOGIC ---

    private void animateOutUpwards() {
        float h = Gdx.graphics.getHeight();

        bgImage.addAction(Actions.moveBy(0, h, 0.9f, Interpolation.pow4In));
        nextBgImage.addAction(Actions.moveBy(0, h, 0.9f, Interpolation.pow4In));

        titleImage.addAction(Actions.moveBy(0, h, 0.7f, Interpolation.pow4In));
        playBtn.addAction(Actions.sequence(Actions.delay(0.1f), Actions.moveBy(0, h, 0.7f, Interpolation.pow4In)));

        exitBtn.addAction(Actions.sequence(
            Actions.delay(0.15f),
            Actions.moveBy(0, h, 0.7f, Interpolation.pow4In),
            Actions.delay(0.05f),
            Actions.run(() -> {
                // Create a NEW screen every time
                game.setScreen(new SongSelectScreen(game, music, null, false));
            })
        ));
    }
    private void animateInFromTop() {
        float h = Gdx.graphics.getHeight();

        // --- Remove background animation ---
        // Because SongSelect handled the transition, this screen just starts with bgImage at 0
        titleImage.addAction(Actions.moveBy(0, h));
        playBtn.addAction(Actions.moveBy(0, h));
        exitBtn.addAction(Actions.moveBy(0, h));

        rootTable.layout();

        titleImage.addAction(Actions.moveBy(0, -h, 0.7f, Interpolation.pow4Out));
        playBtn.addAction(Actions.sequence(Actions.delay(0.1f), Actions.moveBy(0, -h, 0.7f, Interpolation.pow4Out)));
        exitBtn.addAction(Actions.sequence(Actions.delay(0.15f), Actions.moveBy(0, -h, 0.7f, Interpolation.pow4Out)));
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        bgTexture.dispose();
        titleTexture.dispose();
        nextBgTexture.dispose();
    }
}
