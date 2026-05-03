package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;

public class MainMenuScreen implements Screen {
    private final NodeVoltex game;
    private final Stage stage;
    public static Skin skin; // Public static so other screens can share it easily

    public MainMenuScreen(NodeVoltex game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createBasicSkin(); // Generates default button styles programmatically

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // --- TOP RIGHT: Auth Panel ---
        Table authTable = new Table();
        TextButton loginBtn = new TextButton("Login", skin);
        TextButton registerBtn = new TextButton("Register", skin);
        authTable.add(loginBtn).pad(5).width(100);
        authTable.add(registerBtn).pad(5).width(100);

        // Add to root, align top-right
        rootTable.add(authTable).expandX().align(com.badlogic.gdx.utils.Align.right).pad(10).row();

        // --- CENTER: Main Menu Panel ---
        Table menuTable = new Table();
        TextButton startBtn = new TextButton("Start", skin);
        TextButton optionsBtn = new TextButton("Options", skin);
        TextButton leaderboardBtn = new TextButton("Leaderboard", skin);
        TextButton exitBtn = new TextButton("Exit", skin);

        menuTable.add(startBtn).fillX().pad(10).height(50).row();
        menuTable.add(optionsBtn).fillX().pad(10).height(50).row();
        menuTable.add(leaderboardBtn).fillX().pad(10).height(50).row();
        menuTable.add(exitBtn).fillX().pad(10).height(50).row();

        // Add to root, expand to push it to the center
        rootTable.add(menuTable).expand().align(com.badlogic.gdx.utils.Align.center);

        // --- BUTTON EVENTS ---
        startBtn.addListener(new ClickListener() {
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
    }

    // A helper method to generate a basic UI style without needing external asset files
    // A helper method to generate a basic UI style without needing external asset files
    private void createBasicSkin() {
        if (skin != null) return;
        skin = new Skin();

        // Generate a pure WHITE texture so we can safely tint it any color later
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        // Use default libgdx font
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // Configure TextButton style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        // Configure Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // --- THE FIX: Configure ScrollPane style ---
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        // Leaving the scrollbars null makes them invisible but keeps the scroll functionality
        skin.add("default", scrollStyle);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
