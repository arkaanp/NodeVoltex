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

    // --- OUR NEW GLOBAL UI SKIN ---
    public static com.badlogic.gdx.scenes.scene2d.ui.Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 1. Build the UI Skin FIRST so it exists in memory
        createBasicSkin();

        // 2. Boot the Main Menu SECOND
        this.setScreen(new MainMenuScreen(this));
    }

    private void createBasicSkin() {
        skin = new com.badlogic.gdx.scenes.scene2d.ui.Skin();

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));

        // --- NEW: FREETYPE FONT GENERATION ---
        // 1. Load the raw .ttf file
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Montserrat-Light.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // 2. Customize the font parameters
        parameter.size = 16; // Set your desired font size here
        parameter.color = com.badlogic.gdx.graphics.Color.WHITE;

        // Optional: You can add borders, shadows, or filters right here!
        // parameter.borderWidth = 1f;
        // parameter.borderColor = com.badlogic.gdx.graphics.Color.BLACK;
        // parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        // parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;

        // 3. Generate the BitmapFont and immediately dispose of the generator
        com.badlogic.gdx.graphics.g2d.BitmapFont customFont = generator.generateFont(parameter);
        generator.dispose(); // CRITICAL: Always dispose to prevent memory leaks

        // 4. Inject it into your skin as the default font!
        skin.add("default", customFont);


        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle textButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();

        // --- NEW: Semi-Transparent Button Colors (R, G, B, Alpha) ---
        // 0.6f means 60% opacity. Tweak this last number to make it more or less see-through!

        // Normal state: Dark grey, 50% opacity
        textButtonStyle.up = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.1f, 0.1f, 0.1f, 0.5f));

        // Pressed state: Slightly lighter grey, 70% opacity so it "pops" when clicked
        textButtonStyle.down = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.3f, 0.3f, 0.3f, 0.7f));

        // Hover state (Mouse over): Medium grey, 60% opacity
        textButtonStyle.over = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.2f, 0.6f));

        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle labelStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        skin.add("default", labelStyle);

        skin.add("default", new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle());

        // --- NEW: Add TextField Style ---
        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle textFieldStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle();

        // 1. Set the font and text color
        textFieldStyle.font = skin.getFont("default");
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;

        // 2. CRITICAL: A TextField must have a cursor, or it will crash when you click it!
        // We can just use a thin slice of your "white" texture colored white.
        textFieldStyle.cursor = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.WHITE);
        // Make the cursor actually look like a cursor (1 pixel wide)
        textFieldStyle.cursor.setMinWidth(1f);

        // 3. Selection background (what it looks like when you highlight text to copy/paste)
        textFieldStyle.selection = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.valueOf("#FF339980")); // Semi-transparent pink

        // 4. Inject it into the skin
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
