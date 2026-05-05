package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nodevoltex.game.NodeVoltex;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class SongSelectScreen implements Screen {
    private final NodeVoltex game;
    private final Stage stage;
    private final Skin skin;

    // Audio Player for Previews
    private Music currentSongPreview;

    // UI Panels to update dynamically
    private Table scoresListTable;
    private Table songDetailsTable;
    private Label novScoreLabel;
    private Label advScoreLabel;
    private Label exhScoreLabel;
    private Label mxmScoreLabel;

    // --- NEW: State variables for playing ---
    private String selectedMapPath = null;
    private TextButton playButton;

    // --- NEW: Score Sorting State ---
    private com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> currentScores = new com.badlogic.gdx.utils.Array<>();
    private boolean sortByAccuracy = true;
    private String currentDiffDisplay = "NONE";

    public SongSelectScreen(NodeVoltex game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = NodeVoltex.skin; // Reuse the skin we built in the Main Menu
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.pad(20);
        stage.addActor(rootTable);

        // --- BACK BUTTON (Top Left) ---
        TextButton backBtn = new TextButton("< Back", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stopPreview();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        rootTable.add(backBtn).align(Align.topLeft).padBottom(10).colspan(2).row();

        // Split screen into Left (Scores) and Right (Songs)
        Table leftPanel = buildLeftPanel();
        Table rightPanel = buildRightPanel();

        // Allocate 40% width to scores, 60% width to songs
        rootTable.add(leftPanel).expand().fill().padRight(10).uniformX();
        rootTable.add(rightPanel).expand().fill().padLeft(10).uniformX().row();

        // --- NEW: The Play Button (Bottom Right) ---
        playButton = new TextButton("PLAY", skin);
        playButton.setVisible(false); // Hidden until a difficulty is selected!

        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedMapPath != null) {
                    stopPreview();
                    game.setScreen(new PlayScreen(game, selectedMapPath));
                }
            }
        });

        // Add an empty cell to skip the left column, then put the Play button in the right column
        rootTable.add();
        rootTable.add(playButton).align(Align.bottomRight).padTop(10).fillX().height(60);
    }

    private Table buildLeftPanel() {
        Table panel = new Table();
        panel.background(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 1f))); // Dark panel background

        // Headers & Toggles
        Label headerLabel = new Label("SCORES", skin);
        TextButton toggleBtn = new TextButton("Local / Global", skin);
        panel.add(headerLabel).pad(10);
        panel.add(toggleBtn).pad(10).row();

        // Sort Buttons
        Table sortTable = new Table();
        TextButton sortAccBtn = new TextButton("Sort: Accuracy", skin);
        TextButton sortDateBtn = new TextButton("Sort: Date", skin);

        // --- NEW: Add Listeners to sort the active list ---
        sortAccBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                sortByAccuracy = true;
                refreshScoreDisplay();
            }
        });

        sortDateBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                sortByAccuracy = false;
                refreshScoreDisplay();
            }
        });

        sortTable.add(sortAccBtn).pad(5);
        sortTable.add(sortDateBtn).pad(5);
        panel.add(sortTable).colspan(2).row();

        // The Scrollable Score List
        // The Scrollable Score List
        scoresListTable = new Table();

        // Initialize our labels with blank placeholders
        novScoreLabel = new Label("NOV: --", skin);
        advScoreLabel = new Label("ADV: --", skin);
        exhScoreLabel = new Label("EXH: --", skin);
        mxmScoreLabel = new Label("MXM: --", skin);

        // Add them to the table
        scoresListTable.add(novScoreLabel).pad(5).align(Align.left).row();
        scoresListTable.add(advScoreLabel).pad(5).align(Align.left).row();
        scoresListTable.add(exhScoreLabel).pad(5).align(Align.left).row();
        scoresListTable.add(mxmScoreLabel).pad(5).align(Align.left).row();

        ScrollPane scrollPane = new ScrollPane(scoresListTable, skin);

        scrollPane.setScrollingDisabled(true, false); // FORCES vertical scrolling only
        scrollPane.setFadeScrollBars(false);          // Stops scrollbars from vanishing
        scrollPane.setOverscroll(false, false);       // Removes the weird "bouncy" edge effect

        // To make the mouse wheel work immediately on the song list, you can add:
        stage.setScrollFocus(scrollPane);

        scrollPane.setFadeScrollBars(false);
        panel.add(scrollPane).expand().fill().colspan(2).pad(10);

        return panel;
    }

    private Table buildRightPanel() {
        Table panel = new Table();
        panel.background(skin.newDrawable("white", new Color(0.15f, 0.1f, 0.1f, 1f)));

        Table sortTable = new Table();
        sortTable.add(new Label("SONG SELECT", skin)).padRight(20);
        sortTable.add(new TextButton("Title", skin)).pad(5);
        sortTable.add(new TextButton("Artist", skin)).pad(5);
        panel.add(sortTable).row();

        Table songListTable = new Table();

        // --- NEW: Dynamic Folder Scanner ---
        loadSongsFromDirectory(songListTable);

        ScrollPane scrollPane = new ScrollPane(songListTable, skin);
        scrollPane.setFadeScrollBars(false);
        panel.add(scrollPane).expand().fill().pad(10);

        return panel;
    }

    private void loadSongsFromDirectory(Table songListTable) {
        FileHandle songsDir = Gdx.files.internal("assets/songs");

        if (!songsDir.exists() || !songsDir.isDirectory()) {
            System.out.println("Could not find assets/songs/ directory!");
            return;
        }

        JsonReader jsonReader = new JsonReader();

        // Loop through every folder inside assets/songs/
        for (FileHandle folder : songsDir.list()) {
            if (!folder.isDirectory()) continue;

            String title = "Unknown Song";
            String artist = "Unknown Artist";
            String audioFilename = "";

            int novLv = 0, advLv = 0, exhLv = 0, mxmLv = 0;
            String novPath = null, advPath = null, exhPath = null, mxmPath = null;

            // Check for each difficulty file
            FileHandle[] diffFiles = {
                folder.child("nov.json"), folder.child("adv.json"),
                folder.child("exh.json"), folder.child("mxm.json")
            };

            for (int i = 0; i < diffFiles.length; i++) {
                FileHandle diffFile = diffFiles[i];
                if (diffFile.exists()) {
                    try {
                        // Surgically parse only the metadata to save memory
                        JsonValue root = jsonReader.parse(diffFile);
                        JsonValue general = root.get("general");

                        title = general.getString("title", title);
                        artist = general.getString("artist", artist);
                        audioFilename = folder.path() + "/" + general.getString("audioFilename", "audio.ogg");

                        int level = general.getInt("level", 0);

                        // Assign data based on which file we just read
                        if (i == 0) { novLv = level; novPath = diffFile.path(); }
                        if (i == 1) { advLv = level; advPath = diffFile.path(); }
                        if (i == 2) { exhLv = level; exhPath = diffFile.path(); }
                        if (i == 3) { mxmLv = level; mxmPath = diffFile.path(); }

                    } catch (Exception e) {
                        System.out.println("Error parsing metadata in: " + diffFile.path());
                    }
                }
            }

            // Only add the song to the UI if at least one difficulty was found
            // Only add the song to the UI if at least one difficulty was found
            if (novLv > 0 || advLv > 0 || exhLv > 0 || mxmLv > 0) {
                addSongToList(songListTable, title, artist, audioFilename,
                    novLv, advLv, exhLv, mxmLv,
                    novPath, advPath, exhPath, mxmPath, folder.path()); // <-- ADD IT HERE!
            }
        }
    }

    private void addSongToList(Table table, String title, String artist, String audioFilename,
                               int novLv, int advLv, int exhLv, int mxmLv,
                               String novPath, String advPath, String exhPath, String mxmPath,
                               String songFolder) {
        Table songItem = new Table();
        songItem.background(skin.newDrawable("white", new Color(0.2f, 0.2f, 0.25f, 1f)));

        Label titleLabel = new Label(title, skin);
        Label artistLabel = new Label("by " + artist, skin);
        artistLabel.setColor(Color.LIGHT_GRAY);

        Table infoTable = new Table();
        infoTable.add(titleLabel).align(Align.left).row();
        infoTable.add(artistLabel).align(Align.left);

        // --- Make the text clickable, but NOT the whole row ---
        infoTable.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        infoTable.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                playPreview(audioFilename);
                //updateScoresPanel(songFolder);
            }
        });

        songItem.add(infoTable).expandX().align(Align.left).pad(10);

        Table diffTable = new Table();
        // --- NEW: Pass 'songFolder' to the buttons so they know what scores to load ---
        diffTable.add(createDiffButton("NOV", novLv, novPath, Color.PURPLE, songFolder)).pad(5);
        diffTable.add(createDiffButton("ADV", advLv, advPath, Color.YELLOW, songFolder)).pad(5);
        diffTable.add(createDiffButton("EXH", exhLv, exhPath, Color.RED, songFolder)).pad(5);
        diffTable.add(createDiffButton("MXM", mxmLv, mxmPath, Color.valueOf("#C0C0C0"), songFolder)).pad(5);
        songItem.add(diffTable).pad(10);

//        songItem.addListener(new ClickListener() {
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                playPreview(audioFilename);
//                updateScoresPanel(title);
//            }
//        });

        table.add(songItem).expandX().fillX().padBottom(5).row();
    }

    // --- NEW: Added 'songFolder' parameter at the end ---
    private TextButton createDiffButton(String diffName, int level, String mapFilePath, Color activeColor, String songFolder) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));

        if (level == 0) {
            style.up = skin.newDrawable("white", Color.DARK_GRAY);
            TextButton btn = new TextButton(diffName + " --", style);
            btn.setTouchable(Touchable.disabled);
            return btn;
        } else {
            style.up = skin.newDrawable("white", activeColor);
            style.down = skin.newDrawable("white", activeColor.cpy().mul(0.7f));

            TextButton btn = new TextButton(diffName + " " + level, style);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    event.stop();

                    // 1. Update the left panel with the scores for this song
                    loadScoresForDifficulty(mapFilePath, diffName);

                    // 2. Lock in the selected map!
                    selectedMapPath = mapFilePath;

                    // 3. Reveal and update the main Play button at the bottom right
                    playButton.setVisible(true);
                    playButton.setText("PLAY " + diffName + " " + level);
                }
            });
            return btn;
        }
    }

    private void playPreview(String filename) {
        stopPreview();
        try {
            // Gdx.audio natively supports .ogg and .mp3
            currentSongPreview = Gdx.audio.newMusic(Gdx.files.internal(filename));
            currentSongPreview.setLooping(true);
            currentSongPreview.setVolume(0.3f);
            currentSongPreview.play();
        } catch (Exception e) {
            System.out.println("Audio file not found or failed to load: " + filename);
        }
    }

    private void stopPreview() {
        if (currentSongPreview != null) {
            currentSongPreview.stop();
            currentSongPreview.dispose();
            currentSongPreview = null;
        }
    }

//    private void updateScoresPanel(String songTitle) {
//        scoresListTable.clear();
//        scoresListTable.add(new Label("Displaying scores for: " + songTitle, skin)).pad(10).row();
//        scoresListTable.add(new Label("1. NodePlayer - 9,995,000 (S)", skin)).pad(5).row();
//        scoresListTable.add(new Label("2. VoltexPro - 9,800,000 (AAA)", skin)).pad(5).row();
//        scoresListTable.add(new Label("3. Guest - 8,500,000 (A)", skin)).pad(5).row();
//    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    // 1. Loads the JSON file for the SPECIFIC difficulty clicked
    private void loadScoresForDifficulty(String mapFilePath, String diffName) {
        String safeFileName = mapFilePath.replace("/", "_").replace("\\", "_") + "_save.json";
        FileHandle saveFile = Gdx.files.local("assets/scores/" + safeFileName);
        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();

        currentScores.clear();
        currentDiffDisplay = diffName;

        if (saveFile.exists()) {
            try {
                com.nodevoltex.game.data.ScoreHistory history = json.fromJson(com.nodevoltex.game.data.ScoreHistory.class, saveFile);
                if (history != null && history.plays != null) {
                    currentScores.addAll(history.plays);
                }
            } catch (Exception e) {
                System.out.println("Could not parse score history.");
            }
        }

        // Triggers the UI to draw the newly loaded scores
        refreshScoreDisplay();
    }

    // 2. Sorts the loaded scores and draws them to the left panel
    private void refreshScoreDisplay() {
        scoresListTable.clear();
        scoresListTable.add(new Label("Scores for " + currentDiffDisplay, skin)).padBottom(10).row();

        if (currentScores.size == 0) {
            scoresListTable.add(new Label("NO RECORDS YET", skin)).pad(10);
            return;
        }

        // --- THE SORTING ALGORITHM ---
        if (sortByAccuracy) {
            // Sort by Score (Highest first)
            currentScores.sort((a, b) -> Integer.compare(b.score, a.score));
        } else {
            // Sort by Date (Newest first)
            currentScores.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        }

        // --- DRAW THE LIST ---
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (int i = 0; i < currentScores.size; i++) {
            com.nodevoltex.game.data.SaveData data = currentScores.get(i);
            String dateStr = sdf.format(new java.util.Date(data.timestamp));

            // Format: "1. 09500000 [AA+]  |  2024-10-25 14:30"
            String scoreText = (i + 1) + ". " + String.format("%08d", data.score) + " [" + data.grade + "]  |  " + dateStr;
            scoresListTable.add(new Label(scoreText, skin)).align(Align.left).pad(5).row();
        }
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void show() {} @Override public void pause() {} @Override public void resume() {}

    @Override public void hide() {
        stopPreview(); // Ensure audio doesn't keep playing if we minimize the game
    }

    @Override public void dispose() {
        stage.dispose();
        stopPreview();
    }
}
