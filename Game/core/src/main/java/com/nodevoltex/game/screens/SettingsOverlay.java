package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import com.nodevoltex.game.utils.TextureLoader;

public class SettingsOverlay {
    private final Stage stage;
    private final Skin skin;

    private void pickAndUploadPfp() {
        new Thread(() -> {
            try {
                // Use Swing FileChooser for a standard desktop picker
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                fileChooser.setDialogTitle("Select Profile Picture");
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "gif"));

                int result = fileChooser.showOpenDialog(null);
                if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                    java.io.File selectedFile = fileChooser.getSelectedFile();
                    final com.badlogic.gdx.files.FileHandle fileHandle = Gdx.files.absolute(selectedFile.getAbsolutePath());

                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText("Uploading...");
                        com.nodevoltex.game.networking.NetworkManager.uploadProfilePicture(fileHandle, new com.nodevoltex.game.networking.NetworkManager.NetworkCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                statusLabel.setText("Upload Success!");
                                rebuildSettings();
                            }

                            @Override
                            public void onError(String message) {
                                statusLabel.setText("Upload Failed: " + message);
                            }
                        });
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Image darkOverlay;
    private Table settingsPanel;
    private boolean isSettingsOpen = false;
    private ScrollPane settingsScrollPane;

    // Track the headers for the camera
    private Label gameplayHeader;
    private Label inputHeader;
    private Label audioHeader;
    private Label accountHeader;

    private Label statusLabel;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField userField;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField passField;
    private Image pfpImage;

    public SettingsOverlay(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        initSettingsUI();
    }

    private void initSettingsUI() {
        darkOverlay = new Image(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.75f)));
        darkOverlay.setFillParent(true);
        darkOverlay.getColor().a = 0f;
        darkOverlay.setTouchable(Touchable.disabled);
        darkOverlay.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { if (isSettingsOpen) close(); }
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

            // --- Build the content FIRST so the headers exist ---
            Table settingsContainer = new Table();
            settingsContainer.top().left().pad(30);
            buildSettingsContent(settingsContainer);

            Table navBar = new Table();
            navBar.top();
            navBar.background(skin.newDrawable("white", new Color(0.10f, 0.10f, 0.12f, 1f)));

            // Pass the headers directly into the nav buttons
            navBar.add(createNavButton("Account", accountHeader)).expandX().fillX().height(50).row();
            navBar.add(createNavButton("Gameplay", gameplayHeader)).expandX().fillX().height(50).row();
            navBar.add(createNavButton("Input", inputHeader)).expandX().fillX().height(50).row();
            navBar.add(createNavButton("Audio", audioHeader)).expandX().fillX().height(50).row();

        settingsScrollPane = new ScrollPane(settingsContainer, skin);
        settingsScrollPane.setFadeScrollBars(false);
        settingsScrollPane.setScrollingDisabled(true, false);

        // --- Stop the ScrollPane from stealing your slider drags ---
        settingsScrollPane.setCancelTouchFocus(false);

        settingsScrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (stage != null) stage.setScrollFocus(settingsScrollPane);
            }
        });

        contentSplit.add(navBar).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.22f, contentSplit)).expandY().fillY();
        contentSplit.add(settingsScrollPane).expand().fill();

        settingsPanel.add(contentSplit).expand().fill().row();

        TextButton exitSettingsBtn = new TextButton("Exit", skin);
        exitSettingsBtn.setColor(Color.valueOf("#da142b"));
        exitSettingsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { if (isSettingsOpen) close(); }
        });
        settingsPanel.add(exitSettingsBtn).expandX().fillX().height(70).pad(0);

        stage.addActor(settingsPanel);

        stage.addCaptureListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                com.badlogic.gdx.scenes.scene2d.Actor target = event.getTarget();
                com.badlogic.gdx.scenes.scene2d.Actor keyboardFocus = stage.getKeyboardFocus();

                // If a text field currently has focus, and we clicked on anything else
                if (keyboardFocus instanceof com.badlogic.gdx.scenes.scene2d.ui.TextField) {
                    if (target != keyboardFocus && (target == null || !target.isDescendantOf(keyboardFocus))) {
                        // Rip focus away from the text box
                        stage.setKeyboardFocus(null);
                    }
                }

                // Return false so we don't accidentally swallow the click.
                // We just wanted to peek at it
                return false;
            }
        });
    }

    public void open() {
        if (isSettingsOpen) return;
        isSettingsOpen = true;

        darkOverlay.setTouchable(Touchable.enabled);
        darkOverlay.addAction(Actions.alpha(0.75f, 0.4f, Interpolation.pow3Out));
        settingsPanel.addAction(Actions.moveTo(0, 0, 0.4f, Interpolation.pow3Out));

        if (settingsScrollPane != null) {
            stage.setScrollFocus(settingsScrollPane);
        }
    }

    public void close() {
        if (!isSettingsOpen) return;
        isSettingsOpen = false;

        darkOverlay.setTouchable(Touchable.disabled);
        darkOverlay.addAction(Actions.alpha(0f, 0.4f, Interpolation.pow3In));
        settingsPanel.addAction(Actions.moveTo(-settingsPanel.getWidth(), 0, 0.4f, Interpolation.pow3In));
    }

    public boolean isOpen() {
        return isSettingsOpen;
    }

    public void resize(int width, int height) {
        if (settingsPanel != null) {
            float newWidth = width * 0.40f;
            settingsPanel.setSize(newWidth, height);
            if (!isSettingsOpen) settingsPanel.setPosition(-newWidth, 0);
            else settingsPanel.setPosition(0, 0);
        }
    }

    private void buildSettingsContent(Table container) {
        // --- 1. Persistent Login Refresh ---
        if (!com.nodevoltex.game.managers.SettingsManager.getAuthToken().isEmpty() &&
            com.nodevoltex.game.managers.SettingsManager.getUserName().equals("GUEST")) {
            com.nodevoltex.game.networking.NetworkManager.fetchUserProfile(new com.nodevoltex.game.networking.NetworkManager.NetworkCallback<Void>() {
                @Override public void onSuccess(Void result) { rebuildSettings(); }
                @Override public void onError(String message) {}
            });
        }

        accountHeader = new Label("Account", skin);
        accountHeader.setFontScale(1.2f);
        accountHeader.setColor(Color.WHITE);
        container.add(accountHeader).left().padBottom(20).row();

        String currentPfpUrl = com.nodevoltex.game.managers.SettingsManager.getProfilePictureUrl();
        pfpImage = new Image();
        com.nodevoltex.game.utils.TextureLoader.loadIntoImage(currentPfpUrl, pfpImage, null);

        Table accountInfo = new Table();
        accountInfo.left();

        accountInfo.add(pfpImage).size(110, 110).padRight(20);

        Table nameTable = new Table();
        nameTable.left();

        String rawName = com.nodevoltex.game.managers.SettingsManager.getUserName();
        boolean isGuest = rawName.equals("GUEST");

        Label nameLbl = new Label(isGuest ? "Not Logged In" : rawName, skin);
        nameLbl.setFontScale(1.3f);
        nameLbl.setColor(isGuest ? Color.GRAY : Color.WHITE);
        nameTable.add(nameLbl).left().padBottom(5).row();

        float vf = com.nodevoltex.game.managers.SettingsManager.getVolforce();
        statusLabel = new Label(isGuest ? "Please login to access online features." : String.format(java.util.Locale.US, "VF %.3f", vf), skin);
        statusLabel.setFontScale(0.85f);
        statusLabel.setColor(Color.LIGHT_GRAY);
        nameTable.add(statusLabel).left().padBottom(15).row();

        // responsive button style for interactables
        TextButton.TextButtonStyle interactableStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        interactableStyle.up = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 1f));
        interactableStyle.over = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.38f, 1f));
        interactableStyle.down = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.5f, 1f));
        interactableStyle.fontColor = Color.WHITE;

        if (!isGuest) {
            TextButton uploadBtn = new TextButton("Change PFP", interactableStyle);
            uploadBtn.getLabel().setFontScale(0.85f);
            uploadBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    pickAndUploadPfp();
                }
            });
            nameTable.add(uploadBtn).width(120).height(35).left();
        }

        accountInfo.add(nameTable).left();
        container.add(accountInfo).left().padBottom(25).row();

        if (isGuest) {
            buildLoginForm(container, interactableStyle);
        } else {
            TextButton logoutBtn = new TextButton("Logout", interactableStyle);
            logoutBtn.getLabel().setFontScale(0.85f);
            logoutBtn.getLabel().setColor(Color.valueOf("#ff5555"));
            logoutBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    com.nodevoltex.game.managers.SettingsManager.logout();
                    rebuildSettings();
                }
            });
            container.add(logoutBtn).width(120).height(40).left().padBottom(30).row();
        }

        container.add(new Image(skin.newDrawable("white", new Color(1, 1, 1, 0.1f)))).expandX().fillX().height(2).padBottom(30).row();

        gameplayHeader = new Label("Gameplay", skin);
        gameplayHeader.setFontScale(1.2f);
        gameplayHeader.setColor(Color.WHITE);
        container.add(gameplayHeader).left().padBottom(20).row();

        // --- DISPLAY MODE TOGGLE ---
        Table displayTable = new Table();
        displayTable.left();

        Label displayLbl = new Label("Display Mode", skin);
        displayLbl.setFontScale(0.85f);

        TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        toggleStyle.up = skin.newDrawable("white", new Color(0.18f, 0.18f, 0.22f, 1f));
        toggleStyle.fontColor = Color.CYAN;

        final String[] modeNames = {"Windowed", "Fullscreen", "Borderless"};
        final TextButton displayBtn = new TextButton(modeNames[com.nodevoltex.game.managers.SettingsManager.getDisplayMode()], toggleStyle);

        displayBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Cycle through 0 -> 1 -> 2 -> 0
                int nextMode = (com.nodevoltex.game.managers.SettingsManager.getDisplayMode() + 1) % 3;
                com.nodevoltex.game.managers.SettingsManager.setDisplayMode(nextMode);
                displayBtn.setText(modeNames[nextMode]);
                applyDisplayMode(nextMode);
            }
        });

        displayTable.add(displayLbl).padRight(15);
        displayTable.add(displayBtn).width(120).height(35);
        container.add(displayTable).left().padBottom(15).row();

        // 1. Sliders
        container.add(createSliderRow("Scroll speed", 0.01f, 3.0f, 0.01f, com.nodevoltex.game.managers.SettingsManager.getScrollSpeed(), "", "speed")).expandX().fillX().padBottom(15).row();
        container.add(createSliderRow("Global offset", -200f, 200f, 1f, com.nodevoltex.game.managers.SettingsManager.getGlobalOffset(), "ms", "offset")).expandX().fillX().padBottom(30).row();
        container.add(createSliderRow("Hit Position Y", 50f, 300f, 1f, com.nodevoltex.game.managers.SettingsManager.getPlayfieldHitPosY(), "px", "hitpos")).expandX().fillX().padBottom(15).row();
        container.add(createSliderRow("Playfield Width", 200f, 500f, 1f, com.nodevoltex.game.managers.SettingsManager.getPlayfieldWidth(), "px", "width")).expandX().fillX().padBottom(30).row();
        container.add(createSliderRow("BG Brightness", 0.1f, 1.0f, 0.05f, com.nodevoltex.game.managers.SettingsManager.getBackgroundBrightness(), "", "bgbright")).expandX().fillX().padBottom(15).row();
        container.add(createSliderRow("Combo Offset Y", 50f, 600f, 5f, com.nodevoltex.game.managers.SettingsManager.getJudgmentComboTopOffset(), "px", "judg")).expandX().fillX().padBottom(30).row();

        // --- UR Bar Toggle ---
        Table urToggleTable = new Table();
        urToggleTable.left();
        Label urLbl = new Label("Show Unstable Rate Bar", skin);
        urLbl.setFontScale(0.85f);

        TextButton.TextButtonStyle urToggleStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        urToggleStyle.up = skin.newDrawable("white", new Color(0.18f, 0.18f, 0.22f, 1f));
        urToggleStyle.fontColor = com.nodevoltex.game.managers.SettingsManager.isShowURBar() ? Color.CYAN : Color.GRAY;

        final TextButton urBtn = new TextButton(com.nodevoltex.game.managers.SettingsManager.isShowURBar() ? "ON" : "OFF", urToggleStyle);
        urBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean newState = !com.nodevoltex.game.managers.SettingsManager.isShowURBar();
                com.nodevoltex.game.managers.SettingsManager.setShowURBar(newState);
                urBtn.setText(newState ? "ON" : "OFF");
                urBtn.getStyle().fontColor = newState ? Color.CYAN : Color.GRAY;
            }
        });

        urToggleTable.add(urLbl).padRight(15);
        urToggleTable.add(urBtn).width(60).height(35);
        container.add(urToggleTable).left().padBottom(30).row();
        // ----------------------------------------

        inputHeader = new Label("Input", skin);
        inputHeader.setFontScale(1.2f);
        inputHeader.setColor(Color.WHITE);
        container.add(inputHeader).left().padBottom(20).row();

        KeyConfigPanel keyPanel = new KeyConfigPanel(skin);
        container.add(keyPanel).expandX().fillX().padBottom(30).row();

        // --- Interactive Retry Key Mapper ---
        Table retryKeyTable = new Table();
        retryKeyTable.left();

        Label retryLbl = new Label("Quick Retry Key", skin);
        retryLbl.setFontScale(0.85f);

        TextButton.TextButtonStyle flatStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        flatStyle.up = skin.newDrawable("white", new Color(0.18f, 0.18f, 0.22f, 1f));
        flatStyle.fontColor = Color.CYAN;

        final TextButton retryBtn = new TextButton(com.nodevoltex.game.managers.SettingsManager.getRetryKeyString(), flatStyle);

        retryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                retryBtn.setText("_");
                retryBtn.setColor(Color.WHITE);
                stage.setKeyboardFocus(retryBtn); // Steal keyboard focus
            }
        });

        // Listen for the physical keypress
        retryBtn.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (retryBtn.getText().toString().equals("_")) {
                    String keyName = com.badlogic.gdx.Input.Keys.toString(keycode).toUpperCase();
                    if (keyName.equals("SPACE")) keyName = "SPC";

                    if (keyName.equals("ESCAPE")) { // Abort mapping
                        retryBtn.setText(com.nodevoltex.game.managers.SettingsManager.getRetryKeyString());
                    } else { // Save new key
                        com.nodevoltex.game.managers.SettingsManager.setRetryKeyString(keyName);
                        retryBtn.setText(keyName);
                    }

                    retryBtn.setColor(Color.CYAN);
                    stage.setKeyboardFocus(null);
                    return true;
                }
                return false;
            }
        });

        // Cancel if they click somewhere else while mapping
        retryBtn.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            private boolean listenerAdded = false;
            private com.badlogic.gdx.scenes.scene2d.InputListener cancelListener = new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (retryBtn.getText().toString().equals("_") && event.getTarget() != retryBtn) {
                        retryBtn.setText(com.nodevoltex.game.managers.SettingsManager.getRetryKeyString());
                        retryBtn.setColor(Color.CYAN);
                        if (stage != null) stage.setKeyboardFocus(null);
                    }
                    return false;
                }
            };
            @Override
            public boolean act(float delta) {
                if (!listenerAdded && stage != null) {
                    stage.addCaptureListener(cancelListener);
                    listenerAdded = true;
                }
                return false;
            }
        });

        retryKeyTable.add(retryLbl).padRight(15);
        retryKeyTable.add(retryBtn).width(60).height(35);

        container.add(retryKeyTable).left().padBottom(15).row();

        // --- Retry Hold Time Slider ---
        container.add(createSliderRow("Hold to Retry Time", 0.1f, 2.0f, 0.01f, com.nodevoltex.game.managers.SettingsManager.getRetryHoldTime(), "s", "retryhold")).expandX().fillX().padBottom(30).row();

        audioHeader = new Label("Audio", skin);
        audioHeader.setFontScale(1.2f);
        audioHeader.setColor(Color.WHITE);
        container.add(audioHeader).left().padBottom(20).row();

        Label volSubheader = new Label("Volume (takes effect after a new song is played)", skin);
        volSubheader.setFontScale(0.85f);
        volSubheader.setColor(Color.GRAY);
        container.add(volSubheader).left().padBottom(10).row();

        // Volumes are multiplied by 100 so the UI shows "30%" instead of "0.3%"
        container.add(createSliderRow("Master", 0f, 100f, 1f, com.nodevoltex.game.managers.SettingsManager.getMasterVolume() * 100f, "%", "master")).expandX().fillX().padBottom(10).row();
        container.add(createSliderRow("Effect", 0f, 100f, 1f, com.nodevoltex.game.managers.SettingsManager.getEffectVolume() * 100f, "%", "effect")).expandX().fillX().padBottom(10).row();
        container.add(createSliderRow("Music", 0f, 100f, 1f, com.nodevoltex.game.managers.SettingsManager.getMusicVolume() * 100f, "%", "music")).expandX().fillX().padBottom(30).row();
    }

    private void buildLoginForm(final Table container, TextButton.TextButtonStyle btnStyle) {
        Label userLbl = new Label("Username", skin);
        userLbl.setFontScale(0.85f);
        userLbl.setColor(Color.LIGHT_GRAY);
        container.add(userLbl).left().padBottom(5).row();

        userField = createInteractiveTextField(false);
        container.add(userField).expandX().fillX().height(35).padBottom(10).row();

        Label passLbl = new Label("Password", skin);
        passLbl.setFontScale(0.85f);
        passLbl.setColor(Color.LIGHT_GRAY);
        container.add(passLbl).left().padBottom(5).row();

        passField = createInteractiveTextField(true);
        container.add(passField).expandX().fillX().height(35).padBottom(20).row();

        Table btnTable = new Table();
        TextButton loginBtn = new TextButton("Login", btnStyle);
        loginBtn.getLabel().setFontScale(0.85f);
        loginBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                statusLabel.setText("Logging in...");
                com.nodevoltex.game.networking.NetworkManager.login(userField.getText(), passField.getText(), new com.nodevoltex.game.networking.NetworkManager.NetworkCallback<String>() {
                    @Override public void onSuccess(String t) {
                        statusLabel.setText("Login Success!");
                        com.nodevoltex.game.networking.NetworkManager.fetchUserProfile(new com.nodevoltex.game.networking.NetworkManager.NetworkCallback<Void>() {
                            @Override public void onSuccess(Void result) { rebuildSettings(); }
                            @Override public void onError(String message) { rebuildSettings(); }
                        });
                    }
                    @Override public void onError(String m) { statusLabel.setText("Login Failed: " + m); }
                });
            }
        });

        TextButton regBtn = new TextButton("Register", btnStyle);
        regBtn.getLabel().setFontScale(0.85f);
        regBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                statusLabel.setText("Registering...");
                com.nodevoltex.game.networking.NetworkManager.register(userField.getText(), passField.getText(), new com.nodevoltex.game.networking.NetworkManager.NetworkCallback<String>() {
                    @Override public void onSuccess(String t) { statusLabel.setText("Reg Success! Please Login."); }
                    @Override public void onError(String m) { statusLabel.setText("Reg Failed: " + m); }
                });
            }
        });

        btnTable.add(loginBtn).width(100).height(40).padRight(10);
        btnTable.add(regBtn).width(100).height(40);
        container.add(btnTable).left().row();
    }

    private void rebuildSettings() {
        // Redraw only the UI components inside the settings container
        settingsPanel.clearChildren();

        Table contentSplit = new Table();
        Table settingsContainer = new Table();
        settingsContainer.top().left().pad(30);
        buildSettingsContent(settingsContainer);

        Table navBar = new Table();
        navBar.top();
        navBar.background(skin.newDrawable("white", new Color(0.10f, 0.10f, 0.12f, 1f)));
        navBar.add(createNavButton("Account", accountHeader)).expandX().fillX().height(50).row();
        navBar.add(createNavButton("Gameplay", gameplayHeader)).expandX().fillX().height(50).row();
        navBar.add(createNavButton("Input", inputHeader)).expandX().fillX().height(50).row();
        navBar.add(createNavButton("Audio", audioHeader)).expandX().fillX().height(50).row();

        settingsScrollPane = new ScrollPane(settingsContainer, skin);
        settingsScrollPane.setFadeScrollBars(false);
        settingsScrollPane.setScrollingDisabled(true, false);
        settingsScrollPane.setCancelTouchFocus(false);
        settingsScrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (stage != null) stage.setScrollFocus(settingsScrollPane);
            }
        });

        contentSplit.add(navBar).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.22f, contentSplit)).expandY().fillY();
        contentSplit.add(settingsScrollPane).expand().fill();

        settingsPanel.add(contentSplit).expand().fill().row();

        TextButton exitSettingsBtn = new TextButton("Exit", skin);
        exitSettingsBtn.setColor(Color.valueOf("#da142b"));
        exitSettingsBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { if (isSettingsOpen) close(); }
        });
        settingsPanel.add(exitSettingsBtn).expandX().fillX().height(70).pad(0);
    }

    private void saveSetting(String type, float val) {
        if (type.equals("speed")) com.nodevoltex.game.managers.SettingsManager.saveGameplay(val, com.nodevoltex.game.managers.SettingsManager.getGlobalOffset());
        else if (type.equals("offset")) com.nodevoltex.game.managers.SettingsManager.saveGameplay(com.nodevoltex.game.managers.SettingsManager.getScrollSpeed(), val);
        else if (type.equals("master")) com.nodevoltex.game.managers.SettingsManager.saveVolumes(val / 100f, com.nodevoltex.game.managers.SettingsManager.getMusicVolume(), com.nodevoltex.game.managers.SettingsManager.getEffectVolume());
        else if (type.equals("effect")) com.nodevoltex.game.managers.SettingsManager.saveVolumes(com.nodevoltex.game.managers.SettingsManager.getMasterVolume(), com.nodevoltex.game.managers.SettingsManager.getMusicVolume(), val / 100f);
        else if (type.equals("music")) com.nodevoltex.game.managers.SettingsManager.saveVolumes(com.nodevoltex.game.managers.SettingsManager.getMasterVolume(), val / 100f, com.nodevoltex.game.managers.SettingsManager.getEffectVolume());
        else if (type.equals("hitpos")) com.nodevoltex.game.managers.SettingsManager.savePlayfield(val, com.nodevoltex.game.managers.SettingsManager.getPlayfieldWidth());
        else if (type.equals("width")) com.nodevoltex.game.managers.SettingsManager.savePlayfield(com.nodevoltex.game.managers.SettingsManager.getPlayfieldHitPosY(), val);
        else if (type.equals("bgbright")) com.nodevoltex.game.managers.SettingsManager.saveUI(val, com.nodevoltex.game.managers.SettingsManager.getJudgmentComboTopOffset());
        else if (type.equals("judg")) com.nodevoltex.game.managers.SettingsManager.saveUI(com.nodevoltex.game.managers.SettingsManager.getBackgroundBrightness(), val);
            // --- Hook up the new Retry Timer Slider ---
        else if (type.equals("retryhold")) com.nodevoltex.game.managers.SettingsManager.setRetryHoldTime(val);
    }

        private TextButton createNavButton(String text, final Actor targetHeader) {
            TextButton btn = new TextButton(text, skin);
            btn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
            btn.getLabelCell().padLeft(20);
            btn.getLabel().setColor(Color.WHITE);

            // --- Smooth Scrolling Logic ---
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (settingsScrollPane != null && targetHeader != null) {
                        Gdx.app.postRunnable(() -> {
                            settingsScrollPane.validate(); // Force height calculation

                            // 1. Apply exact song list centering math
                            com.badlogic.gdx.math.Vector2 tempPos = new com.badlogic.gdx.math.Vector2(0, targetHeader.getHeight() / 2f);
                            targetHeader.localToAscendantCoordinates(settingsScrollPane.getActor(), tempPos);

                            float distanceToTop = settingsScrollPane.getActor().getHeight() - tempPos.y;
                            float targetScroll = distanceToTop - (settingsScrollPane.getHeight() / 2f);

                            // 2. Kill any ongoing scrolls so it doesn't glitch if players spam click
                            settingsScrollPane.clearActions();

                            // 3. Smoothly animate the ScrollPane's Y position
                            float startScroll = settingsScrollPane.getScrollY();
                            settingsScrollPane.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                                float time = 0;
                                float duration = 0.35f; // Duration of the smooth slide
                                @Override
                                public boolean act(float delta) {
                                    float safeDelta = Math.min(delta, 0.03f);
                                    time += safeDelta;
                                    float progress = Math.min(time / duration, 1f);

                                    // Pow3Out matches the slick UI transition speed
                                    float eased = Interpolation.pow3Out.apply(progress);
                                    settingsScrollPane.setScrollY(startScroll + (targetScroll - startScroll) * eased);

                                    return time >= duration;
                                }
                            });
                        });
                    }
                }
            });

            return btn;
        }

    private Table createSliderRow(String titleText, float min, float max, float step, float defaultVal, String suffix, final String settingType) {
        Table row = new Table();

        final com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = skin.newDrawable("white", new Color(0.18f, 0.18f, 0.22f, 1f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = skin.newDrawable("white", new Color(0.24f, 0.24f, 0.28f, 1f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable clickBg = skin.newDrawable("white", new Color(0.30f, 0.30f, 0.35f, 1f));

        row.setBackground(normalBg);
        row.pad(15);
        row.setTouchable(Touchable.enabled);

        final ClickListener rowClickListener = new ClickListener();
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
        titleLabel.setFontScale(1f);
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
                float val = slider.getValue();
                saveSetting(settingType, val); // Instantly saves to disk when dragging

                if (!valueField.hasKeyboardFocus()) {
                    valueField.setText(String.format(java.util.Locale.US, formatStr, val) + suffix);
                }
            }
        });

        valueField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor, boolean focused) {
                if (!focused) applyTextFieldValue(valueField, slider, min, max, suffix, step);
            }
        });

        valueField.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
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

    private void applyTextFieldValue(com.badlogic.gdx.scenes.scene2d.ui.TextField valueField, com.badlogic.gdx.scenes.scene2d.ui.Slider slider, float min, float max, String suffix, float step) {
        String rawText = valueField.getText().replace("%", "").replace("ms", "").trim();
        final String formatStr = (step % 1 == 0) ? "%.0f" : "%.2f";

        try {
            float parsed = Float.parseFloat(rawText);
            parsed = Math.max(min, Math.min(max, parsed));
            slider.setValue(parsed);

            // can't easily reach 'settingType' from here unless we pass it down.
            // Luckily, slider.setValue(parsed) triggers the ChangeListener above automatically, so it's already saved

            valueField.setText(String.format(java.util.Locale.US, formatStr, parsed) + suffix);
        } catch (NumberFormatException e) {
            float val = slider.getValue();
            valueField.setText(String.format(java.util.Locale.US, formatStr, val) + suffix);
        }
    }

    private com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle getCustomSliderStyle() {
        com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle style = new com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle();
        int sliderHeight = 14;

        com.badlogic.gdx.graphics.Pixmap bgPixmap = new com.badlogic.gdx.graphics.Pixmap(100, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0.1f, 0.1f, 0.15f, 1f));
        bgPixmap.fill();
        style.background = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(bgPixmap));
        bgPixmap.dispose();

        com.badlogic.gdx.graphics.Pixmap fillPixmap = new com.badlogic.gdx.graphics.Pixmap(10, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        fillPixmap.setColor(Color.valueOf("#5e42a6"));
        fillPixmap.fill();
        style.knobBefore = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(fillPixmap));
        fillPixmap.dispose();

        com.badlogic.gdx.graphics.Pixmap knobPixmap = new com.badlogic.gdx.graphics.Pixmap(6, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.valueOf("#8C6DF0"));
        knobPixmap.fill();
        style.knob = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(knobPixmap));
        knobPixmap.dispose();

        com.badlogic.gdx.graphics.Pixmap knobOverPixmap = new com.badlogic.gdx.graphics.Pixmap(7, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        knobOverPixmap.setColor(Color.valueOf("#BCA8FF"));
        knobOverPixmap.fill();
        style.knobOver = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(knobOverPixmap));
        knobOverPixmap.dispose();

        com.badlogic.gdx.graphics.Pixmap knobDownPixmap = new com.badlogic.gdx.graphics.Pixmap(7, sliderHeight, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        knobDownPixmap.setColor(Color.WHITE);
        knobDownPixmap.fill();
        style.knobDown = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.Texture(knobDownPixmap));
        knobDownPixmap.dispose();

        return style;
    }

    // --- THE CUSTOM ARCADE KEY MAPPER COMPONENT ---
    private class KeyConfigPanel extends Table {
        private String[] pri = new String[10];
        private String[] alt = new String[10];
        private final String[] codes = {"LL", "LR", "RL", "RR", "BT1", "BT2", "BT3", "BT4", "FXL", "FXR"};

        private boolean isPrimary = true;
        private int editingIndex = -1;

        private TextButton btnLL, btnRL, btnBT1, btnBT2, btnBT3, btnBT4, btnFX1, btnFX2, tabPri, tabAlt;

        public KeyConfigPanel(Skin skin) {
            this.setTouchable(Touchable.enabled);

            // 1. Instantly read the saved keys from the hard drive
            for (int i = 0; i < 10; i++) {
                pri[i] = com.nodevoltex.game.managers.SettingsManager.getKeyString(codes[i], true);
                alt[i] = com.nodevoltex.game.managers.SettingsManager.getKeyString(codes[i], false);
            }

            TextButton.TextButtonStyle flatStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
            flatStyle.up = skin.newDrawable("white", Color.WHITE);
            // --- Strip default skin animations to guarantee absolute hover immunity ---
            flatStyle.down = null;
            flatStyle.over = null;
            flatStyle.fontColor = Color.BLACK;

            btnLL = new TextButton("", flatStyle); btnRL = new TextButton("", flatStyle);
            btnBT1 = new TextButton("", flatStyle); btnBT2 = new TextButton("", flatStyle);
            btnBT3 = new TextButton("", flatStyle); btnBT4 = new TextButton("", flatStyle);
            btnFX1 = new TextButton("", flatStyle); btnFX2 = new TextButton("", flatStyle);

            setupButton(btnLL, 0); setupButton(btnRL, 2);
            setupButton(btnBT1, 4); setupButton(btnBT2, 5);
            setupButton(btnBT3, 6); setupButton(btnBT4, 7);
            setupButton(btnFX1, 8); setupButton(btnFX2, 9);

            TextButton.TextButtonStyle tabStyle = new TextButton.TextButtonStyle(flatStyle);
            tabStyle.fontColor = Color.WHITE;

            tabPri = new TextButton("Primary", tabStyle);
            tabAlt = new TextButton("Alternate", tabStyle);

            tabPri.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { isPrimary = true; editingIndex = -1; refreshDisplay(); }
            });
            tabAlt.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { isPrimary = false; editingIndex = -1; refreshDisplay(); }
            });

            this.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (editingIndex != -1) {
                        String keyName = com.badlogic.gdx.Input.Keys.toString(keycode).toUpperCase();
                        if (keyName.equals("SPACE")) keyName = "SPC";

                        if (keyName.equals("ESCAPE")) {
                            editingIndex = -1;
                            if (getStage() != null) getStage().setKeyboardFocus(null);
                            refreshDisplay();
                            return true;
                        }

                        String[] currentArr = isPrimary ? pri : alt;
                        currentArr[editingIndex] = keyName;

                        // 2. Instantly save the newly mapped key to the hard drive
                        com.nodevoltex.game.managers.SettingsManager.saveKey(codes[editingIndex], isPrimary, keyName);

                        if (editingIndex == 0) editingIndex = 1;
                        else if (editingIndex == 2) editingIndex = 3;
                        else {
                            editingIndex = -1;
                            if (getStage() != null) getStage().setKeyboardFocus(null);
                        }

                        refreshDisplay();
                        return true;
                    }
                    return false;
                }
            });

            this.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                private boolean listenerAdded = false;
                private com.badlogic.gdx.scenes.scene2d.InputListener cancelListener = new com.badlogic.gdx.scenes.scene2d.InputListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        if (editingIndex != -1) {
                            if (!(event.getTarget() instanceof TextButton && event.getTarget().isDescendantOf(KeyConfigPanel.this))) {
                                editingIndex = -1;
                                if (getStage() != null) getStage().setKeyboardFocus(null);
                                refreshDisplay();
                            }
                        }
                        return false;
                    }
                };

                @Override
                public boolean act(float delta) {
                    if (!listenerAdded && getStage() != null) {
                        getStage().addCaptureListener(cancelListener);
                        listenerAdded = true;
                    }
                    return false;
                }
            });

            Table layout = new Table();
            Table laserRow = new Table();
            laserRow.add(btnLL).width(130).height(40).padRight(20);
            laserRow.add(btnRL).width(130).height(40);
            layout.add(laserRow).padBottom(10).row();

            Table btRow = new Table();
            btRow.defaults().width(60).height(80).padLeft(5).padRight(5);
            btRow.add(btnBT1); btRow.add(btnBT2); btRow.add(btnBT3); btRow.add(btnBT4);
            layout.add(btRow).padBottom(10).row();

            Table fxRow = new Table();
            fxRow.defaults().width(120).height(40).padLeft(5).padRight(5);
            fxRow.add(btnFX1); fxRow.add(btnFX2);
            layout.add(fxRow).row();

            Table tabsRow = new Table();
            tabsRow.add(tabPri).width(100).height(35).padRight(10);
            tabsRow.add(tabAlt).width(100).height(35);

            this.add(layout).padBottom(20).row();
            this.add(tabsRow).left().row();

            refreshDisplay();
        }

        private Color getBaseColor(int startIndex) {
            if (startIndex == 0 || startIndex == 1) return Color.valueOf("#00BFFF"); // Cyan
            if (startIndex == 2 || startIndex == 3) return Color.valueOf("#FF007F"); // Pink
            if (startIndex >= 4 && startIndex <= 7) return Color.valueOf("#D3D3D3"); // Light Gray
            if (startIndex == 8 || startIndex == 9) return Color.valueOf("#FFA500"); // Orange
            return Color.WHITE;
        }

        private boolean isEditingThisButton(int startIndex) {
            if (startIndex == 0) return editingIndex == 0 || editingIndex == 1;
            if (startIndex == 2) return editingIndex == 2 || editingIndex == 3;
            return editingIndex == startIndex;
        }

        private void setupButton(final TextButton btn, final int startIndex) {
            final ClickListener btnListener = new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    editingIndex = startIndex;
                    if (getStage() != null) getStage().setKeyboardFocus(KeyConfigPanel.this);
                    refreshDisplay();
                }
            };
            btn.addListener(btnListener);

            // --- Proper brightness hierarchy + Absolute immunity ---
            btn.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                @Override
                public boolean act(float delta) {
                    Color base = getBaseColor(startIndex);

                    if (isEditingThisButton(startIndex)) {
                        // PRIORITY 1: Actively Editing.
                        // Locked to 1.0f (maximum brightness). Ignores hover entirely
                        btn.setColor(base.r, base.g, base.b, 1.0f);
                    } else if (btnListener.isOver()) {
                        // PRIORITY 2: Hovering.
                        // Brighter than idle, letting the user know it's clickable.
                        btn.setColor(base.r, base.g, base.b, 0.85f);
                    } else {
                        // PRIORITY 3: Idle.
                        // Dimmed out so it doesn't distract the eyes.
                        btn.setColor(base.r, base.g, base.b, 0.65f);
                    }
                    return false;
                }
            });
        }

        private void refreshDisplay() {
            String[] arr = isPrimary ? pri : alt;

            btnLL.setText((editingIndex == 0 ? "_" : arr[0]) + " / " + (editingIndex == 1 ? "_" : arr[1]));
            btnRL.setText((editingIndex == 2 ? "_" : arr[2]) + " / " + (editingIndex == 3 ? "_" : arr[3]));
            btnBT1.setText(editingIndex == 4 ? "_" : arr[4]);
            btnBT2.setText(editingIndex == 5 ? "_" : arr[5]);
            btnBT3.setText(editingIndex == 6 ? "_" : arr[6]);
            btnBT4.setText(editingIndex == 7 ? "_" : arr[7]);
            btnFX1.setText(editingIndex == 8 ? "_" : arr[8]);
            btnFX2.setText(editingIndex == 9 ? "_" : arr[9]);

            tabPri.setColor(Color.valueOf(isPrimary ? "#5e42a6" : "#333333"));
            tabAlt.setColor(Color.valueOf(!isPrimary ? "#5e42a6" : "#333333"));
        }
    }

    public static void applyDisplayMode(int mode) {
        if (mode == 0) {
            // Windowed (Standard 720p default)
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(1280, 720);
        } else if (mode == 1) {
            // Exclusive Fullscreen (Takes absolute control of the monitor)
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (mode == 2) {
            // Borderless Fullscreen (Windowed mode stretched to monitor size without borders)
            Gdx.graphics.setUndecorated(true);
            Gdx.graphics.setWindowedMode(Gdx.graphics.getDisplayMode().width, Gdx.graphics.getDisplayMode().height);
        }
    }

    private com.badlogic.gdx.scenes.scene2d.ui.TextField createInteractiveTextField(boolean isPassword) {
        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle baseStyle = skin.get(com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle.class);
        final com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle customStyle = new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle(baseStyle);

        // Define the 3 states
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable idleBg = skin.newDrawable("white", new Color(0.12f, 0.12f, 0.15f, 1f)); // Dark gray
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = skin.newDrawable("white", new Color(0.18f, 0.18f, 0.22f, 1f)); // Lighter gray
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable focusBg = skin.newDrawable("white", new Color(0.25f, 0.25f, 0.3f, 1f));  // Brightest outline

        customStyle.background = idleBg;
        customStyle.focusedBackground = focusBg;
        customStyle.fontColor = Color.WHITE;

        final com.badlogic.gdx.scenes.scene2d.ui.TextField field = new com.badlogic.gdx.scenes.scene2d.ui.TextField("", customStyle);
        if (isPassword) {
            field.setPasswordMode(true);
            field.setPasswordCharacter('*');
        }

        final ClickListener hoverListener = new ClickListener();
        field.addListener(hoverListener);

        field.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (field.hasKeyboardFocus()) {
                    // LibGDX handles focusedBackground natively
                } else if (hoverListener.isOver()) {
                    customStyle.background = hoverBg;
                } else {
                    customStyle.background = idleBg;
                }
                return false;
            }
        });

        return field;
    }
}
