package com.nodevoltex.game; // Make sure this matches your actual package name!

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.nodevoltex.game.screens.MainMenuScreen;

public class NodeVoltex extends Game {
    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public com.badlogic.gdx.Screen songSelectScreen;

    // --- OUR NEW GLOBAL UI SKIN ---
    public static com.badlogic.gdx.scenes.scene2d.ui.Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 1. Build the UI Skin FIRST so it exists in memory
        createBasicSkin();

        // --- THE FIX: Force Global Smooth Text ---
        com.badlogic.gdx.utils.ObjectMap<String, com.badlogic.gdx.graphics.g2d.BitmapFont> fonts = skin.getAll(com.badlogic.gdx.graphics.g2d.BitmapFont.class);

        if (fonts != null) {
            for (com.badlogic.gdx.graphics.g2d.BitmapFont font : fonts.values()) {
                // 1. Double check the graphics card is using smooth bilinear filtering
                font.getRegion().getTexture().setFilter(
                    com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                    com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
                );

                // 2. Stop LibGDX from forcing text onto rigid pixel grids.
                // This permanently stops the pixelation/blurriness after transitioning screens!
                font.setUseIntegerPositions(false);
            }
        }
        // -----------------------------------------

        // 2. Boot the Main Menu SECOND
        this.setScreen(new MainMenuScreen(this));
    }

    private void createBasicSkin() {
        skin = new com.badlogic.gdx.scenes.scene2d.ui.Skin();

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));

        // --- 1. PREPARE THE FONT GENERATOR ---
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Montserrat-Light.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.color = com.badlogic.gdx.graphics.Color.WHITE;

        // This ensures the edges of the font blend smoothly
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;

        // --- 2. GENERATE SIZES ---
        // Default (Size 16)
        parameter.size = 16;
        com.badlogic.gdx.graphics.g2d.BitmapFont fontDefault = generator.generateFont(parameter);
        skin.add("default", fontDefault);

        // Medium (Size 32 - for standard titles)
        parameter.size = 32;
        com.badlogic.gdx.graphics.g2d.BitmapFont fontMedium = generator.generateFont(parameter);
        skin.add("medium", fontMedium);

        // Large (Size 64 - for big score numbers)
        parameter.size = 64;
        com.badlogic.gdx.graphics.g2d.BitmapFont fontLarge = generator.generateFont(parameter);
        skin.add("large", fontLarge);

        // Huge (Size 150 - for the massive Grade letter)
        parameter.size = 150;
        com.badlogic.gdx.graphics.g2d.BitmapFont fontHuge = generator.generateFont(parameter);
        fontHuge.setUseIntegerPositions(false); // Keeps the massive letter perfectly smooth
        skin.add("huge", fontHuge);

        generator.dispose(); // CRITICAL: Only dispose AFTER generating all 4 sizes!

        // --- 3. CREATE LABEL STYLES ---
        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle defaultStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        defaultStyle.font = fontDefault;
        skin.add("default", defaultStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle mediumStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        mediumStyle.font = fontMedium;
        skin.add("medium", mediumStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle largeStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        largeStyle.font = fontLarge;
        skin.add("large", largeStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle hugeStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        hugeStyle.font = fontHuge;
        skin.add("huge", hugeStyle);

        // --- THE REST OF YOUR SKIN LOGIC ---
        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle textButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.1f, 0.1f, 0.1f, 0.5f));
        textButtonStyle.down = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.3f, 0.3f, 0.3f, 0.7f));
        textButtonStyle.over = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.2f, 0.6f));
        textButtonStyle.font = fontDefault;
        skin.add("default", textButtonStyle);

        skin.add("default", new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle());

        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle textFieldStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle();
        textFieldStyle.font = fontDefault;
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.WHITE);
        textFieldStyle.cursor.setMinWidth(1f);
        textFieldStyle.selection = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.valueOf("#FF339980"));
        skin.add("default", textFieldStyle);
    }

    @Override
    public void render() {
        // CRITICAL: In LibGDX, this line is what actually tells your active Screen to draw!
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (skin != null) {
            skin.dispose();
        }
    }
}
