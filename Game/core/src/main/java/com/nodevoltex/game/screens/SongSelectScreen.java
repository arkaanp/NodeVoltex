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

    // --- NEW: Independent Layers replacing rootTable ---
    private Table leftLayer;
    private Table rightLayer;

    // --- SETTINGS DRAWER VARIABLES ---
    private Image darkOverlay;
    private Table settingsPanel;
    private boolean isSettingsOpen = false;
    private com.badlogic.gdx.scenes.scene2d.ui.ScrollPane settingsScrollPane; // <-- ADD THIS!

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
        // Add 40px right padding ONLY to the search bar so it doesn't touch the glass!
        rightColumn.add(searchBar).expandX().fillX().padRight(40).height(60).row();
        // The list panel gets NO padding so it stretches to the absolute edge
        rightColumn.add(rightPanel).expand().fill();

        // --- INDEPENDENT LEFT LAYER ---
        leftLayer = new Table();
        leftLayer.setFillParent(true);
        leftLayer.left();
        leftLayer.add(leftPanel)
            // Increased to 48% width to fill the newly opened space
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.48f, leftLayer))
            .expandY().fillY()
            .padTop(40).padBottom(40); // REMOVED padLeft!
        stage.addActor(leftLayer);

        // --- INDEPENDENT RIGHT LAYER ---
        rightLayer = new Table();
        rightLayer.setFillParent(true);
        rightLayer.right();
        rightLayer.add(rightColumn)
            .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.48f, rightLayer))
            .expandY().fillY()
            .padTop(40).padBottom(40); // REMOVED padRight!
        stage.addActor(rightLayer);

        // --- FLOATING BOTTOM BAR (Back, Mods, Options) ---
        backTable = new Table();
        backTable.setFillParent(true);
        backTable.bottom().left();

        TextButton backBtn = new TextButton("Back", NodeVoltex.skin);
        backBtn.setColor(Color.valueOf("#7E57C2")); // Lighter Purple
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stage.getRoot().setTouchable(Touchable.disabled);
                rightPanel.stopAudio();
                animateOutDownwards();
            }
        });

        TextButton modsBtn = new TextButton("Mods", NodeVoltex.skin);
        modsBtn.setColor(Color.valueOf("#7a9e35")); // Greenish
        modsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Safe dummy button for now - No crashing!
                System.out.println("Mods button clicked");
            }
        });

        TextButton optionsBtn = new TextButton("Options", NodeVoltex.skin);
        optionsBtn.setColor(Color.valueOf("#4b1d82"));
        optionsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // --- FIRE THE ANIMATION ---
                openSettings();
            }
        });

        // Add them in a row. Left padding on the first button, right padding between them!
        backTable.add(backBtn).width(150).height(50).padLeft(20).padBottom(20).padRight(10);
        backTable.add(modsBtn).width(150).height(50).padBottom(20).padRight(10);
        backTable.add(optionsBtn).width(150).height(50).padBottom(20);

        stage.addActor(backTable);

        // --- TRIGGER ENTRY ANIMATION ---
        initSettingsUI();
        animateInFromBottom();

        // --- GLOBAL HOVERLESS SCROLL SYSTEM ---
        stage.addCaptureListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                // --- THE FIX: Stop stealing the scroll wheel if settings are open! ---
                // Returning false lets the event safely pass down to our settings scroll pane.
                if (isSettingsOpen) {
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

        // 1. The Background moves down in 1.0s with pow2In acceleration
        bgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));
        prevBgImage.addAction(Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In));

        // 2. Left Layer syncs the movement perfectly
        leftLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In), // Exact same speed and easing
            Actions.sequence(
                Actions.delay(0.5f), // Wait until halfway down...
                Actions.alpha(1.0f, 0.5f, Interpolation.linear) // ...then fade out completely
            )
        ));

        // 3. Right Layer syncs perfectly
        rightLayer.addAction(Actions.parallel(
            Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
            Actions.sequence(
                Actions.delay(0.5f),
                Actions.alpha(1.0f, 0.5f, Interpolation.linear)
            )
        ));

        // 4. Back Button syncs perfectly, and changes the screen the moment the animation finishes
        backTable.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0, -h, 1.0f, Interpolation.pow2In),
                Actions.sequence(
                    Actions.delay(0.5f),
                    Actions.alpha(1.0f, 0.5f, Interpolation.linear)
                )
            ),
            // Fire the screen change the exact frame the 1.0s movement completes
            Actions.run(() -> game.setScreen(new MainMenuScreen(game)))
        ));
    }

    // --- THE OVERLAY MANAGERS ---

    private void showSettingsWindow() {
        // 1. Create a Modal Window (Blocks clicks to the song list underneath)
        final com.badlogic.gdx.scenes.scene2d.ui.Window settingsWindow = new com.badlogic.gdx.scenes.scene2d.ui.Window("GAME SETTINGS", skin);
        settingsWindow.setModal(true);
        settingsWindow.setMovable(false);
        // Give it a solid dark background
        settingsWindow.setBackground(skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.1f, 0.1f, 0.15f, 0.95f)));

        settingsWindow.padTop(40).padLeft(20).padRight(20).padBottom(20);
        settingsWindow.defaults().pad(10).left();

        // --- VOLUME SETTINGS ---
        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Master Volume:", skin));
        com.badlogic.gdx.scenes.scene2d.ui.Slider masterSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.05f, false, skin);
        masterSlider.setValue(0.5f); // Set to your actual saved preference!
        settingsWindow.add(masterSlider).width(200).row();

        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Music Volume:", skin));
        com.badlogic.gdx.scenes.scene2d.ui.Slider musicSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.05f, false, skin);
        musicSlider.setValue(0.8f);
        settingsWindow.add(musicSlider).width(200).row();

        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Laser/Hit Volume:", skin));
        com.badlogic.gdx.scenes.scene2d.ui.Slider sfxSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.05f, false, skin);
        sfxSlider.setValue(1.0f);
        settingsWindow.add(sfxSlider).width(200).row();

        // Add a visual separator
        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(skin.newDrawable("white", com.badlogic.gdx.graphics.Color.DARK_GRAY))).colspan(2).fillX().height(2).padTop(10).padBottom(10).row();

        // --- OFFSET & SPEED SETTINGS ---
        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Global Offset (ms):", skin));
        com.badlogic.gdx.scenes.scene2d.ui.Table offsetTable = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        com.badlogic.gdx.scenes.scene2d.ui.TextButton offsetMinus = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("<", skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label offsetValue = new com.badlogic.gdx.scenes.scene2d.ui.Label("0", skin);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton offsetPlus = new com.badlogic.gdx.scenes.scene2d.ui.TextButton(">", skin);
        offsetTable.add(offsetMinus).width(40);
        offsetTable.add(offsetValue).width(60).align(com.badlogic.gdx.utils.Align.center);
        offsetTable.add(offsetPlus).width(40);
        settingsWindow.add(offsetTable).row();

        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Scroll Speed:", skin));
        com.badlogic.gdx.scenes.scene2d.ui.Table speedTable = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        com.badlogic.gdx.scenes.scene2d.ui.TextButton speedMinus = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("<", skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label speedValue = new com.badlogic.gdx.scenes.scene2d.ui.Label("3.5", skin);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton speedPlus = new com.badlogic.gdx.scenes.scene2d.ui.TextButton(">", skin);
        speedTable.add(speedMinus).width(40);
        speedTable.add(speedValue).width(60).align(com.badlogic.gdx.utils.Align.center);
        speedTable.add(speedPlus).width(40);
        settingsWindow.add(speedTable).row();

        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(skin.newDrawable("white", com.badlogic.gdx.graphics.Color.DARK_GRAY))).colspan(2).fillX().height(2).padTop(10).padBottom(10).row();

        // --- KEY BINDINGS ---
        settingsWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Key Bindings:", skin));
        com.badlogic.gdx.scenes.scene2d.ui.TextButton keyConfigBtn = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("Configure Keys...", skin);
        settingsWindow.add(keyConfigBtn).fillX().row();

        // --- CLOSE BUTTON ---
        com.badlogic.gdx.scenes.scene2d.ui.TextButton closeBtn = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("SAVE & CLOSE", skin);
        closeBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                // TODO: Save your settings to LibGDX Preferences here!
                settingsWindow.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.2f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
                ));
            }
        });

        settingsWindow.add(closeBtn).colspan(2).fillX().padTop(20).height(50);

        // 2. Animate it popping into the center of the screen
        settingsWindow.pack();
        settingsWindow.setPosition(
            stage.getWidth() / 2f - settingsWindow.getWidth() / 2f,
            stage.getHeight() / 2f - settingsWindow.getHeight() / 2f
        );
        settingsWindow.getColor().a = 0f;
        settingsWindow.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.2f));

        stage.addActor(settingsWindow);
    }

    private void showModWindow() {
        final com.badlogic.gdx.scenes.scene2d.ui.Window modWindow = new com.badlogic.gdx.scenes.scene2d.ui.Window("MODIFIERS", skin);
        modWindow.setModal(true);
        modWindow.setMovable(false);
        modWindow.setBackground(skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0.15f, 0.1f, 0.1f, 0.95f))); // Slight red tint for mods
        modWindow.padTop(40).padLeft(20).padRight(20).padBottom(20);

        com.badlogic.gdx.scenes.scene2d.ui.Label placeholder = new com.badlogic.gdx.scenes.scene2d.ui.Label("Modifiers configuration coming soon...", skin);
        placeholder.setAlignment(com.badlogic.gdx.utils.Align.center);
        modWindow.add(placeholder).pad(20).row();

        com.badlogic.gdx.scenes.scene2d.ui.TextButton closeBtn = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("CLOSE", skin);
        closeBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                modWindow.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(0.2f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
                ));
            }
        });
        modWindow.add(closeBtn).fillX().height(50);

        modWindow.pack();
        modWindow.setPosition(
            stage.getWidth() / 2f - modWindow.getWidth() / 2f,
            stage.getHeight() / 2f - modWindow.getHeight() / 2f
        );
        modWindow.getColor().a = 0f;
        modWindow.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn(0.2f));

        stage.addActor(modWindow);
    }

    private void initSettingsUI() {
        darkOverlay = new Image(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.75f)));
        darkOverlay.setFillParent(true);
        darkOverlay.getColor().a = 0f;
        darkOverlay.setTouchable(Touchable.disabled);
        darkOverlay.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { if (isSettingsOpen) closeSettings(); }
        });
        stage.addActor(darkOverlay);

        settingsPanel = new Table();
        settingsPanel.setBackground(skin.newDrawable("white", new Color(0.15f, 0.15f, 0.18f, 1f)));

        float panelWidth = stage.getWidth() * 0.40f;
        settingsPanel.setSize(panelWidth, stage.getHeight());
        settingsPanel.setPosition(-panelWidth, 0);
        settingsPanel.setTouchable(Touchable.enabled);
        settingsPanel.addListener(new ClickListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) { return true; }
        });

        Table contentSplit = new Table();

        Table navBar = new Table();
        navBar.top();
        navBar.background(skin.newDrawable("white", new Color(0.10f, 0.10f, 0.12f, 1f)));

        navBar.add(createNavButton("Gameplay")).expandX().fillX().height(50).row();
        navBar.add(createNavButton("Input")).expandX().fillX().height(50).row();
        navBar.add(createNavButton("Audio")).expandX().fillX().height(50).row();

        Table settingsContainer = new Table();
        settingsContainer.top().left().pad(30);
        buildSettingsContent(settingsContainer);

        settingsScrollPane = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(settingsContainer, skin);
        settingsScrollPane.setFadeScrollBars(false);
        settingsScrollPane.setScrollingDisabled(true, false);

        // --- THE FIX: Instantly steal mouse wheel focus the second you hover the right panel! ---
        settingsScrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                // Force the Stage to send all scroll wheel inputs to this menu!
                if (stage != null) stage.setScrollFocus(settingsScrollPane);
            }
        });

        contentSplit.add(navBar).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.22f, contentSplit)).expandY().fillY();
        contentSplit.add(settingsScrollPane).expand().fill();

        settingsPanel.add(contentSplit).expand().fill().row();

        TextButton exitSettingsBtn = new TextButton("<<<", skin);
        exitSettingsBtn.setColor(Color.valueOf("#da142b"));
        exitSettingsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { if (isSettingsOpen) closeSettings(); }
        });
        settingsPanel.add(exitSettingsBtn).expandX().fillX().height(70).pad(0);

        stage.addActor(settingsPanel);
    }

    private void openSettings() {
        if (isSettingsOpen) return;
        isSettingsOpen = true;

        darkOverlay.setTouchable(Touchable.enabled);
        darkOverlay.addAction(Actions.alpha(0.75f, 0.4f, Interpolation.pow3Out));
        settingsPanel.addAction(Actions.moveTo(0, 0, 0.4f, Interpolation.pow3Out));

        // --- THE FIX: Instantly grant mouse wheel focus to the settings menu! ---
        if (settingsScrollPane != null) {
            stage.setScrollFocus(settingsScrollPane);
        }
    }

    private void closeSettings() {
        if (!isSettingsOpen) return;
        isSettingsOpen = false;

        // Turn off the background click catcher immediately
        darkOverlay.setTouchable(Touchable.disabled);

        // 1. Fade out the dark overlay back to normal brightness
        darkOverlay.addAction(Actions.alpha(0f, 0.4f, Interpolation.pow3In));

        // 2. Slide the panel back off-screen to the left
        settingsPanel.addAction(Actions.moveTo(-settingsPanel.getWidth(), 0, 0.4f, Interpolation.pow3In));
    }

    // --- UI HELPER METHODS ---

    private void buildSettingsContent(Table container) {
        // --- GAMEPLAY SECTION ---
        Label gameplayHeader = new Label("Gameplay", skin);
        gameplayHeader.setFontScale(1f);
        gameplayHeader.setColor(Color.WHITE);
        container.add(gameplayHeader).left().padBottom(20).row();

        // --- THE FIX: Min 0.01, Max 2.0, Step 0.01, Default 1.0 ---
        container.add(createSliderRow("Scroll speed", 0.01f, 2.0f, 0.01f, 1.0f, "")).expandX().fillX().padBottom(15).row();
        container.add(createSliderRow("Global offset", -200f, 200f, 1f, 0f, "ms")).expandX().fillX().padBottom(30).row();

        // --- INPUT SECTION ---
        Label inputHeader = new Label("Input", skin);
        inputHeader.setFontScale(1f);
        inputHeader.setColor(Color.WHITE);
        container.add(inputHeader).left().padBottom(20).row();

        TextButton keyConfigBtn = new TextButton("Configure Key Bindings...", skin);
        keyConfigBtn.setColor(Color.valueOf("#5e42a6"));
        keyConfigBtn.getLabel().setFontScale(0.85f);
        container.add(keyConfigBtn).expandX().fillX().height(50).padBottom(30).row();

        // --- AUDIO SECTION ---
        Label audioHeader = new Label("Audio", skin);
        audioHeader.setFontScale(1f);
        audioHeader.setColor(Color.WHITE);
        container.add(audioHeader).left().padBottom(20).row();

        Label volSubheader = new Label("Volume", skin);
        volSubheader.setFontScale(0.85f);
        volSubheader.setColor(Color.GRAY);
        container.add(volSubheader).left().padBottom(10).row();

        container.add(createSliderRow("Master", 0f, 100f, 1f, 100f, "%")).expandX().fillX().padBottom(10).row();
        container.add(createSliderRow("Effect", 0f, 100f, 1f, 80f, "%")).expandX().fillX().padBottom(10).row();
        container.add(createSliderRow("Music", 0f, 100f, 1f, 100f, "%")).expandX().fillX().padBottom(30).row();
    }

    private TextButton createNavButton(String text) {
        TextButton btn = new TextButton(text, skin);
        btn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
        btn.getLabelCell().padLeft(20);
        btn.getLabel().setColor(Color.WHITE);
        return btn;
    }

    // --- THE FIX: Solid, Fully Reactive Hover Rows ---
    private Table createSliderRow(String titleText, float min, float max, float step, float defaultVal, String suffix) {
        Table row = new Table();

        final com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = skin.newDrawable("white", new Color(0.18f, 0.18f, 0.22f, 1f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = skin.newDrawable("white", new Color(0.24f, 0.24f, 0.28f, 1f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable clickBg = skin.newDrawable("white", new Color(0.30f, 0.30f, 0.35f, 1f));

        row.setBackground(normalBg);
        row.pad(15);
        row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

        final com.badlogic.gdx.scenes.scene2d.utils.ClickListener rowClickListener = new com.badlogic.gdx.scenes.scene2d.utils.ClickListener();
        row.addListener(rowClickListener);

        row.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (rowClickListener.isVisualPressed()) row.setBackground(clickBg);
                else if (rowClickListener.isOver()) row.setBackground(hoverBg);
                else row.setBackground(normalBg);
                return false;
            }
        });

        Table textTable = new Table();
        textTable.left();
        Label titleLabel = new Label(titleText, skin);
        titleLabel.setFontScale(0.85f);
        textTable.add(titleLabel).left().row();

        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle fieldStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle();
        fieldStyle.font = skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class).font;
        fieldStyle.fontColor = Color.GRAY;

        com.badlogic.gdx.graphics.Pixmap bgPix = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        bgPix.setColor(new Color(0, 0, 0, 0f));
        bgPix.fill();
        fieldStyle.background = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(bgPix));
        bgPix.dispose();

        com.badlogic.gdx.graphics.Pixmap cursorPix = new com.badlogic.gdx.graphics.Pixmap(2, 14, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        cursorPix.setColor(Color.WHITE);
        cursorPix.fill();
        fieldStyle.cursor = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(cursorPix));
        cursorPix.dispose();

        // --- THE FIX: Smart 2-Decimal Formatting ---
        final String formatStr = (step % 1 == 0) ? "%.0f" : "%.2f";
        String initialValStr = String.format(java.util.Locale.US, formatStr, defaultVal);

        final com.badlogic.gdx.scenes.scene2d.ui.TextField valueField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(initialValStr + suffix, fieldStyle);

        valueField.setTextFieldFilter((textField, c) -> Character.isDigit(c) || c == '.' || c == '-' || c == '%');
        textTable.add(valueField).width(80).left();

        row.add(textTable).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.35f, row)).left();

        com.badlogic.gdx.scenes.scene2d.ui.Slider slider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(min, max, step, false, getCustomSliderStyle());
        slider.setValue(defaultVal);

        slider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (!valueField.hasKeyboardFocus()) {
                    float val = slider.getValue();
                    valueField.setText(String.format(java.util.Locale.US, formatStr, val) + suffix);
                }
            }
        });

        // Notice we now pass the 'step' variable so the helper method knows how to format it!
        valueField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor, boolean focused) {
                if (!focused) applyTextFieldValue(valueField, slider, min, max, suffix, step);
            }
        });

        valueField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean keyDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                    applyTextFieldValue(valueField, slider, min, max, suffix, step);
                    stage.setKeyboardFocus(null);
                    return true;
                }
                return false;
            }
        });

        row.add(slider).expandX().fillX().padLeft(20);
        return row;
    }

    // --- THE FIX: Format logic added for manual typing ---
    private void applyTextFieldValue(com.badlogic.gdx.scenes.scene2d.ui.TextField valueField, com.badlogic.gdx.scenes.scene2d.ui.Slider slider, float min, float max, String suffix, float step) {
        String rawText = valueField.getText().replace("%", "").replace("ms", "").trim();
        final String formatStr = (step % 1 == 0) ? "%.0f" : "%.2f";

        try {
            float parsed = Float.parseFloat(rawText);
            parsed = Math.max(min, Math.min(max, parsed));
            slider.setValue(parsed);
            valueField.setText(String.format(java.util.Locale.US, formatStr, parsed) + suffix);
        } catch (NumberFormatException e) {
            float val = slider.getValue();
            valueField.setText(String.format(java.util.Locale.US, formatStr, val) + suffix);
        }
    }

    private com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle getCustomSliderStyle() {
        com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle style = new com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle();
        int sliderHeight = 14;

        // 1. The Track
        com.badlogic.gdx.graphics.Pixmap bgPixmap = new com.badlogic.gdx.graphics.Pixmap(100, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0.1f, 0.1f, 0.15f, 1f));
        bgPixmap.fill();
        style.background = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(bgPixmap));
        bgPixmap.dispose();

        // 2. The Purple Fill
        com.badlogic.gdx.graphics.Pixmap fillPixmap = new com.badlogic.gdx.graphics.Pixmap(10, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        fillPixmap.setColor(Color.valueOf("#5e42a6"));
        fillPixmap.fill();
        style.knobBefore = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(fillPixmap));
        fillPixmap.dispose();

        // 3. NORMAL KNOB (Width: 6)
        com.badlogic.gdx.graphics.Pixmap knobPixmap = new com.badlogic.gdx.graphics.Pixmap(6, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.valueOf("#8C6DF0"));
        knobPixmap.fill();
        style.knob = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(knobPixmap));
        knobPixmap.dispose();

        // 4. HOVER KNOB (Width: 7 - Extremely subtle 1px bump, lighter color)
        com.badlogic.gdx.graphics.Pixmap knobOverPixmap = new com.badlogic.gdx.graphics.Pixmap(7, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        knobOverPixmap.setColor(Color.valueOf("#BCA8FF"));
        knobOverPixmap.fill();
        style.knobOver = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(knobOverPixmap));
        knobOverPixmap.dispose();

        // 5. DOWN KNOB (Width: 7 - Same subtle size as hover, pure white for click feedback)
        com.badlogic.gdx.graphics.Pixmap knobDownPixmap = new com.badlogic.gdx.graphics.Pixmap(7, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        knobDownPixmap.setColor(Color.WHITE);
        knobDownPixmap.fill();
        style.knobDown = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(knobDownPixmap));
        knobDownPixmap.dispose();

        return style;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);

        // --- THE FIX: True Responsive Design! ---
        // Dynamically recalculates the 40% panel width if the player resizes the game window!
        if (settingsPanel != null) {
            float newWidth = width * 0.40f;
            settingsPanel.setSize(newWidth, height);

            // Keep it hidden if closed, or snap it to the edge if open
            if (!isSettingsOpen) settingsPanel.setPosition(-newWidth, 0);
            else settingsPanel.setPosition(0, 0);
        }
    }

    @Override
    public void show() {
        // MUST be here so the screen regains mouse clicks every time you return to it!
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // MUST be here to clear the Main Menu from the frame buffer!
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
