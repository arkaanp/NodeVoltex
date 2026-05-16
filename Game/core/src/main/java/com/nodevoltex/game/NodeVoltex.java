package com.nodevoltex.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.nodevoltex.game.screens.MainMenuScreen;

public class NodeVoltex extends Game {
    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public com.badlogic.gdx.Screen songSelectScreen;

    public static com.badlogic.gdx.scenes.scene2d.ui.Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        createBasicSkin();

        ObjectMap<String, BitmapFont> fonts = skin.getAll(BitmapFont.class);
        if (fonts != null) {
            for (BitmapFont font : fonts.values()) {
                font.getRegion().getTexture().setFilter(
                    Texture.TextureFilter.Linear,
                    Texture.TextureFilter.Linear
                );
                font.setUseIntegerPositions(false);
            }
        }

        this.setScreen(new MainMenuScreen(this));
    }

    private void createBasicSkin() {
        skin = new com.badlogic.gdx.scenes.scene2d.ui.Skin();

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        skin.add("white", new com.badlogic.gdx.graphics.Texture(pixmap));

        // --- 1. HARVEST ALL CHARACTERS ---
        String allUsedChars = scanBeatmapCharacters();

        // --- 2. USE NOTO SANS FOR EVERYTHING ---
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator generator =
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(Gdx.files.internal("NotoSansJP-Light.ttf"));
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.color = com.badlogic.gdx.graphics.Color.WHITE;
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;

        // Feed it the massive string of English + Japanese characters
        parameter.characters = allUsedChars;

        // --- 3. GENERATE SIZES ---
        parameter.size = 16;
        skin.add("default", generator.generateFont(parameter));

        parameter.size = 32;
        skin.add("medium", generator.generateFont(parameter));

        parameter.size = 64;
        skin.add("large", generator.generateFont(parameter));

        // Huge (150) - ONLY give this the default English characters
        // Rendering Japanese Kanji at size 150 will instantly crash the OpenGL memory.
        parameter.size = 150;
        parameter.characters = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS;
        com.badlogic.gdx.graphics.g2d.BitmapFont fontHuge = generator.generateFont(parameter);
        fontHuge.setUseIntegerPositions(false);
        skin.add("huge", fontHuge);

        generator.dispose(); // CRITICAL: Always dispose

        // --- 4. CREATE LABEL STYLES ---
        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle defaultStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        defaultStyle.font = skin.getFont("default");
        skin.add("default", defaultStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle mediumStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        mediumStyle.font = skin.getFont("medium");
        skin.add("medium", mediumStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle largeStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        largeStyle.font = skin.getFont("large");
        skin.add("large", largeStyle);

        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle hugeStyle = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        hugeStyle.font = skin.getFont("huge");
        skin.add("huge", hugeStyle);

        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle textButtonStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.1f, 0.1f, 0.1f, 0.5f));
        textButtonStyle.down = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.3f, 0.3f, 0.3f, 0.7f));
        textButtonStyle.over = skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.2f, 0.6f));
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        skin.add("default", new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle());

        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle textFieldStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default");
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.WHITE);
        textFieldStyle.cursor.setMinWidth(1f);
        textFieldStyle.selection = skin.newDrawable("white", com.badlogic.gdx.graphics.Color.valueOf("#FF339980"));
        skin.add("default", textFieldStyle);
    }

    // --- BLAZING FAST CHARACTER HARVESTER ---
    private String scanBeatmapCharacters() {
        StringBuilder chars = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
        // Ensure standard Hiragana, Katakana, and common punctuation are permanently loaded
        chars.append("あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんぁぃぅぇぉっゃゅょがぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽ");
        chars.append("アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンァィゥェォッャュョガギグゲゴザジズゼゾダヂヅデドバビブベボパピプペポヴー");
        chars.append("〜！？「」【】『』、。・　");

        // Scan the local 'songs' folder that's set up earlier
        com.badlogic.gdx.files.FileHandle songsDir = Gdx.files.local("songs");

        if (songsDir.exists() && songsDir.isDirectory()) {
            JsonReader reader = new JsonReader();

            for (com.badlogic.gdx.files.FileHandle folder : songsDir.list()) {
                if (folder.isDirectory()) {
                    for (com.badlogic.gdx.files.FileHandle file : folder.list()) {
                        if (file.extension().equals("json")) {
                            try {
                                JsonValue root = reader.parse(file);
                                JsonValue general = root.get("general");
                                if (general != null) {
                                    String combined = general.getString("title", "") +
                                        general.getString("artist", "") +
                                        general.getString("mapper", "");

                                    for (char c : combined.toCharArray()) {
                                        // If the character isn't already in the builder, add it
                                        if (chars.indexOf(String.valueOf(c)) == -1) {
                                            chars.append(c);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Skip broken json files silently
                            }
                            break; // We only need to check 1 diff file per song folder
                        }
                    }
                }
            }
        }
        return chars.toString();
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (skin != null) skin.dispose();
    }
}
