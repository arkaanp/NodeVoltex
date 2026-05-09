package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.nodevoltex.game.NodeVoltex;

public class SongListPanel extends Table {
    private final NodeVoltex game;
    private final Skin skin;
    private final StatsPanel statsPanel;
    private final Table songListTable;
    private final ScrollPane scrollPane;
    private com.badlogic.gdx.audio.Music mainMenuMusic;

    // --- State Variables ---
    private Array<SongData> allSongs = new Array<>();
    private SongData selectedSong = null;
    private String selectedDiffName = "";
    private Actor currentlyExpandedActor = null; // Used to track where to auto-scroll
    private Music previewMusic;

    // --- Data Container Class ---
    private static class SongData {
        String title, artist, mapper, audioPath;
        int novLv = 0, advLv = 0, exhLv = 0, mxmLv = 0;
        String novPath, advPath, exhPath, mxmPath;
        float previewOffsetSeconds = 0f;

        boolean hasDiffs() { return novLv > 0 || advLv > 0 || exhLv > 0 || mxmLv > 0; }
    }

    public SongListPanel(NodeVoltex game, Skin skin, StatsPanel statsPanel, com.badlogic.gdx.audio.Music mainMenuMusic) {
        this.game = game;
        this.skin = skin;
        this.statsPanel = statsPanel;
        this.mainMenuMusic = mainMenuMusic;

        songListTable = new Table();
        songListTable.top();

        scrollPane = new ScrollPane(songListTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        this.add(scrollPane).expand().fill().pad(10);
        loadSongsFromDirectory();
    }

    private void loadSongsFromDirectory() {
        FileHandle songsDir = Gdx.files.internal("assets/songs");
        if (!songsDir.exists() || !songsDir.isDirectory()) return;

        JsonReader jsonReader = new JsonReader();

        for (FileHandle folder : songsDir.list()) {
            if (!folder.isDirectory()) continue;

            SongData data = new SongData();
            data.title = "Unknown Song";
            data.artist = "Unknown Artist";
            data.mapper = "Unknown Mapper"; // Default mapper

            FileHandle[] diffFiles = {
                folder.child("nov.json"), folder.child("adv.json"),
                folder.child("exh.json"), folder.child("mxm.json")
            };

            for (int i = 0; i < diffFiles.length; i++) {
                if (diffFiles[i].exists()) {
                    try {
                        JsonValue root = jsonReader.parse(diffFiles[i]);
                        JsonValue general = root.get("general");

                        data.title = general.getString("title", data.title);
                        data.artist = general.getString("artist", data.artist);
                        data.mapper = general.getString("mapper", data.mapper);
                        data.audioPath = folder.path() + "/" + general.getString("audioFilename", "audio.ogg");

                        // --- Grab the offset (default to 0) and convert ms to seconds ---
                        int offsetMs = general.getInt("previewOffset", 0);
                        data.previewOffsetSeconds = offsetMs / 1000f;

                        int level = general.getInt("level", 0);

                        if (i == 0) { data.novLv = level; data.novPath = diffFiles[i].path(); }
                        if (i == 1) { data.advLv = level; data.advPath = diffFiles[i].path(); }
                        if (i == 2) { data.exhLv = level; data.exhPath = diffFiles[i].path(); }
                        if (i == 3) { data.mxmLv = level; data.mxmPath = diffFiles[i].path(); }
                    } catch (Exception e) {
                        System.out.println("Error parsing: " + diffFiles[i].path());
                    }
                }
            }

            if (data.hasDiffs()) allSongs.add(data);
        }

        refreshSongList(); // Draw the UI based on loaded data
    }

    // Completely rebuilds the list, handling Expanded vs Collapsed states
    private void refreshSongList() {
        songListTable.clearChildren();
        currentlyExpandedActor = null;

        for (SongData song : allSongs) {
            Table item;
            if (song == selectedSong) {
                item = buildExpandedItem(song);
                currentlyExpandedActor = item; // Mark this so we can scroll to it
            } else {
                item = buildCollapsedItem(song);
            }
            songListTable.add(item).expandX().fillX().padBottom(5).row();
        }

        // Auto-Center the scroll pane on the selected song
        if (currentlyExpandedActor != null) {
            Gdx.app.postRunnable(() -> {
                // 1. Force the UI to calculate the new expanded sizes FIRST
                songListTable.layout();
                scrollPane.layout();

                // 2. Find the exact vertical center of our expanded song box
                float itemCenterY = currentlyExpandedActor.getY() + (currentlyExpandedActor.getHeight() / 2f);

                // 3. Find how far down that center point is from the very top of the list
                float distanceFromTop = songListTable.getHeight() - itemCenterY;

                // 4. Subtract half of the visible scroll window to perfectly center it
                float targetScrollY = distanceFromTop - (scrollPane.getHeight() / 2f);

                // 5. Instantly snap the scrollbar to that exact mathematical center
                scrollPane.setScrollY(targetScrollY);
            });
        }
    }

    // View: When the song is NOT clicked
    private Table buildCollapsedItem(SongData song) {
        Table item = new Table();
        item.background(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.7f)));

        // Do this in BOTH buildCollapsedItem and buildExpandedItem
        Table textTable = new Table();
        textTable.left(); // Force internal contents left
        textTable.add(new Label(song.title, skin)).align(Align.left).padBottom(2).row();

        Label artistLabel = new Label(song.artist, skin);
        artistLabel.setColor(Color.LIGHT_GRAY);
        textTable.add(artistLabel).align(Align.left);

        // --- ADD .left() HERE ---
        item.add(textTable).expandX().fillX().left().pad(10).padLeft(20).row();

        Table diffBars = new Table();
        Color novCol = song.novLv > 0 ? Color.valueOf("#599F00") : Color.DARK_GRAY;
        Color advCol = song.advLv > 0 ? Color.valueOf("#FFBD59") : Color.DARK_GRAY;
        Color exhCol = song.exhLv > 0 ? Color.RED : Color.DARK_GRAY;
        Color mxmCol = song.mxmLv > 0 ? Color.valueOf("#1800AD") : Color.DARK_GRAY;

        diffBars.add(new Image(skin.newDrawable("white", novCol))).height(6).expandX().fillX().padRight(4);
        diffBars.add(new Image(skin.newDrawable("white", advCol))).height(6).expandX().fillX().padRight(4);
        diffBars.add(new Image(skin.newDrawable("white", exhCol))).height(6).expandX().fillX().padRight(4);
        diffBars.add(new Image(skin.newDrawable("white", mxmCol))).height(6).expandX().fillX();

        item.add(diffBars).expandX().fillX().padLeft(20).padRight(20).padBottom(5);

        item.setTouchable(Touchable.enabled);
        item.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                handleSongSelection(song);
            }
        });
        return item;
    }

    // View: When the song IS clicked (Accordion open)
    private Table buildExpandedItem(SongData song) {
        // 1. The Invisible Wrapper
        // This holds the original song box AND the dropdown difficulties below it.
        Table wrapper = new Table();

        // 2. The Original Black Box (Exactly as it is when collapsed)
        Table headerBox = new Table();
        headerBox.background(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.7f)));

        Table textTable = new Table();
        textTable.left();
        textTable.add(new Label(song.title, skin)).align(Align.left).padBottom(2).row();

        Label artistLabel = new Label(song.artist, skin);
        artistLabel.setColor(Color.LIGHT_GRAY);
        textTable.add(artistLabel).align(Align.left);

        headerBox.add(textTable).expandX().fillX().left().pad(10).padLeft(20).row();

        // The thin colored strips from the collapsed view
        Table thinBars = new Table();
        Color novCol = song.novLv > 0 ? Color.valueOf("#599F00") : Color.DARK_GRAY;
        Color advCol = song.advLv > 0 ? Color.valueOf("#FFBD59") : Color.DARK_GRAY;
        Color exhCol = song.exhLv > 0 ? Color.RED : Color.DARK_GRAY;
        Color mxmCol = song.mxmLv > 0 ? Color.valueOf("#1800AD") : Color.DARK_GRAY;

        thinBars.add(new Image(skin.newDrawable("white", novCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", advCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", exhCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", mxmCol))).height(6).expandX().fillX();
        headerBox.add(thinBars).expandX().fillX().padLeft(20).padRight(20).padBottom(5);

        // Add the untouched header to the top of the wrapper
        wrapper.add(headerBox).expandX().fillX().row();

        // 3. The Difficulties Dropdown
        Table diffDropdown = new Table();
        if (song.novLv > 0) diffDropdown.add(createDiffRow(song, "NOV", song.novLv, Color.valueOf("#599F00"), song.novPath)).expandX().fillX().padBottom(2).row();
        if (song.advLv > 0) diffDropdown.add(createDiffRow(song, "ADV", song.advLv, Color.valueOf("#FFBD59"), song.advPath)).expandX().fillX().padBottom(2).row();
        if (song.exhLv > 0) diffDropdown.add(createDiffRow(song, "EXH", song.exhLv, Color.RED, song.exhPath)).expandX().fillX().padBottom(2).row();
        if (song.mxmLv > 0) diffDropdown.add(createDiffRow(song, "MXM", song.mxmLv, Color.valueOf("#1800AD"), song.mxmPath)).expandX().fillX().padBottom(2).row();

        // Add the dropdown BELOW the header.
        // padLeft(30) indents the diffs to the right.
        // padTop(2) adds a tiny gap so it doesn't touch the header.
        wrapper.add(diffDropdown).expandX().fillX().padLeft(30).padTop(2);

        return wrapper;
    }

    // The individual clickable difficulties inside an expanded song
    private Table createDiffRow(SongData song, String diffName, int level, Color color, String mapPath) {
        Table row = new Table();
        boolean isSelected = selectedDiffName.equals(diffName);

        // Opacity: Bright (0.8f) for selected, Dark/Muted (0.3f) for unselected
        float alpha = isSelected ? 0.8f : 0.3f;
        row.background(skin.newDrawable("white", new Color(color.r, color.g, color.b, alpha)));

        Label diffLabel = new Label(diffName + " " + level, skin);
        diffLabel.setColor(Color.WHITE);

        // --- FIX 1: Align text to the LEFT and indent it to match the title/artist ---
        // Added .expandX() and simplified align to .left()
        row.add(diffLabel).expandX().left().pad(8).padLeft(20);

        row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        row.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                // --- FIX 2: Check if this difficulty is already selected ---
                if (selectedDiffName.equals(diffName)) {
                    // If it is already selected, pressing it again plays the map!
                    stopAudio(); // Crucial: shut off the preview music before loading the game
                    if (mapPath != null) {
                        game.setScreen(new PlayScreen(game, mapPath));
                    } else {
                        System.out.println("Warning: mapPath is null, cannot start map.");
                    }
                } else {
                    // If it is NOT selected, select it and update the UI
                    selectedDiffName = diffName;
                    updateStatsPanel(song, diffName, level, mapPath);
                    refreshSongList();
                }
            }
        });

        return row;
    }

    // Fired when you click a collapsed song
    private void handleSongSelection(SongData song) {
        selectedSong = song;

        // 1. Default to the highest difficulty
        int defaultLv = 0;
        String defaultPath = null;

        if (song.mxmLv > 0) { selectedDiffName = "MXM"; defaultLv = song.mxmLv; defaultPath = song.mxmPath; }
        else if (song.exhLv > 0) { selectedDiffName = "EXH"; defaultLv = song.exhLv; defaultPath = song.exhPath; }
        else if (song.advLv > 0) { selectedDiffName = "ADV"; defaultLv = song.advLv; defaultPath = song.advPath; }
        else if (song.novLv > 0) { selectedDiffName = "NOV"; defaultLv = song.novLv; defaultPath = song.novPath; }

        // --- Pass the offset into the audio player ---
        playAudio(song.audioPath, song.previewOffsetSeconds);

        updateStatsPanel(song, selectedDiffName, defaultLv, defaultPath);
        refreshSongList();

        // 4. Trigger UI Expansion & Auto-Scroll
        refreshSongList();
    }

    private void updateStatsPanel(SongData song, String diffName, int level, String mapPath) {
        Array<com.nodevoltex.game.data.SaveData> loadedScores = new Array<>();

        if (mapPath != null) {
            String safeFileName = mapPath.replace("/", "_").replace("\\", "_") + "_save.json";
            FileHandle saveFile = Gdx.files.local("assets/scores/" + safeFileName);

            if (saveFile.exists()) {
                try {
                    com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                    com.nodevoltex.game.data.ScoreHistory history = json.fromJson(com.nodevoltex.game.data.ScoreHistory.class, saveFile);
                    if (history != null && history.plays != null) {
                        loadedScores.addAll(history.plays);
                    }
                } catch (Exception e) {
                    System.out.println("Could not parse score history.");
                }
            }
        }

        // Pass the mapper string along with everything else
        statsPanel.updateSong(song.title, song.artist, diffName + " " + level, song.mapper, loadedScores);
    }

    private void playAudio(String audioPath, float offsetSeconds) {
        if (mainMenuMusic != null) {
            mainMenuMusic.stop();
            mainMenuMusic.dispose();
            mainMenuMusic = null;
        }

        if (previewMusic != null) {
            previewMusic.stop();
            previewMusic.dispose();
            previewMusic = null;
        }

        try {
            if (audioPath != null) {
                com.badlogic.gdx.files.FileHandle file = Gdx.files.internal(audioPath);
                if (file.exists()) {
                    previewMusic = Gdx.audio.newMusic(file);
                    previewMusic.setLooping(true);
                    previewMusic.setVolume(0.3f);

                    previewMusic.play();

                    // --- NEW: Jump to the drop immediately after starting! ---
                    if (offsetSeconds > 0) {
                        previewMusic.setPosition(offsetSeconds);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to play audio: " + audioPath);
        }
    }

    public void stopAudio() {
        // Catch-all to make sure everything shuts up if you hit the Back button
        if (mainMenuMusic != null) {
            mainMenuMusic.stop();
            mainMenuMusic.dispose();
            mainMenuMusic = null;
        }
        if (previewMusic != null) {
            previewMusic.stop();
            previewMusic.dispose();
            previewMusic = null;
        }
    }

    // --- NEW: Sorts the maps and redraws the list ---
    public void sortSongs(String criteria) {
        if (criteria.equals("title")) {
            allSongs.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
        } else if (criteria.equals("artist")) {
            allSongs.sort((a, b) -> a.artist.compareToIgnoreCase(b.artist));
        } else if (criteria.equals("mapper")) {
            allSongs.sort((a, b) -> a.mapper.compareToIgnoreCase(b.mapper));
        }
        refreshSongList();

        // Scroll back to the top when a new sort is applied
        Gdx.app.postRunnable(() -> scrollPane.setScrollY(0));
    }

    // --- NEW: Method to capture mouse wheel focus ---
    public void requestScrollFocus(com.badlogic.gdx.scenes.scene2d.Stage stage) {
        stage.setScrollFocus(this.scrollPane);
    }

    // --- NEW: Method to pick a random song ---
    public void selectRandomSong() {
        if (allSongs.size > 0) {
            // Pick a random number between 0 and the last index of the array
            int randomIndex = com.badlogic.gdx.math.MathUtils.random(allSongs.size - 1);

            // Trigger the exact same logic as if the user clicked it!
            handleSongSelection(allSongs.get(randomIndex));
        }
    }

    public void selectSongByPath(String targetPath) {
        if (targetPath == null || targetPath.trim().isEmpty()) {
            selectRandomSong();
            return;
        }

        String target = targetPath.replace("\\", "/").toLowerCase();

        // 1. FIGURE OUT THE EXACT DIFFICULTY FIRST based on the file extension
        String expectedDiff = "NOV"; // Default fallback
        if (target.endsWith("mxm.json") || target.contains("/mxm.json")) expectedDiff = "MXM";
        else if (target.endsWith("exh.json") || target.contains("/exh.json")) expectedDiff = "EXH";
        else if (target.endsWith("adv.json") || target.contains("/adv.json")) expectedDiff = "ADV";

        // 2. Extract the folder name
        String[] targetParts = target.split("/");
        String targetFolder = targetParts.length >= 2 ? targetParts[targetParts.length - 2] : target;

        for (SongData song : allSongs) {
            String nov = song.novPath != null ? song.novPath.replace("\\", "/").toLowerCase() : "";
            String adv = song.advPath != null ? song.advPath.replace("\\", "/").toLowerCase() : "";
            String exh = song.exhPath != null ? song.exhPath.replace("\\", "/").toLowerCase() : "";
            String mxm = song.mxmPath != null ? song.mxmPath.replace("\\", "/").toLowerCase() : "";

            // 3. Check if ANY of the paths match our target folder
            if ((!nov.isEmpty() && nov.contains("/" + targetFolder + "/")) ||
                (!adv.isEmpty() && adv.contains("/" + targetFolder + "/")) ||
                (!exh.isEmpty() && exh.contains("/" + targetFolder + "/")) ||
                (!mxm.isEmpty() && mxm.contains("/" + targetFolder + "/"))) {

                // We found the song!
                handleSongSelection(song); // Expand the UI box
                selectedDiffName = expectedDiff; // Force the UI to highlight the correct difficulty

                // 4. Safely pull the correct level and path for the StatsPanel update
                String safePath = song.novPath;
                int matchedLevel = song.novLv;

                if (expectedDiff.equals("MXM") && song.mxmPath != null) { safePath = song.mxmPath; matchedLevel = song.mxmLv; }
                else if (expectedDiff.equals("EXH") && song.exhPath != null) { safePath = song.exhPath; matchedLevel = song.exhLv; }
                else if (expectedDiff.equals("ADV") && song.advPath != null) { safePath = song.advPath; matchedLevel = song.advLv; }

                updateStatsPanel(song, expectedDiff, matchedLevel, safePath);
                refreshSongList();
                return;
            }
        }

        System.out.println("WARNING: Could not find any song matching folder: " + targetFolder);
        selectRandomSong();
    }

    @Override
    public void act(float delta) {
        // 1. Let the ScrollPane and normal Table layout do their math first
        super.act(delta);

        // 2. Calculate the slope ratio for a 5-degree incline
        // (tan(5) is roughly 0.087. We use this to find X based on Y)
        float angleInDegrees = 5f;
        float tanAngle = (float) Math.tan(Math.toRadians(angleInDegrees));

        // 3. Iterate through every song box inside your scrollable list.
        if (songListTable != null) {
            for (com.badlogic.gdx.scenes.scene2d.Actor songBox : songListTable.getChildren()) {

                // A. Find where this specific box is currently drawn on the screen
                com.badlogic.gdx.math.Vector2 pos = new com.badlogic.gdx.math.Vector2(0, songBox.getY());
                songListTable.localToAscendantCoordinates(this, pos);

                // B. Calculate how far down the screen the box is from the top of the panel
                // (Assuming your search bar is at the top of this panel)
                float distanceDown = this.getHeight() - pos.y;

                // C. Calculate the required X offset to match the line.
                // NOTE: If the boxes slide the wrong direction ( \ instead of / ),
                // simply add a minus sign here: distanceDown * -tanAngle;
                float targetX = distanceDown * tanAngle;

                // D. Force the box to slide over to the diagonal line!
                songBox.setX(targetX);
            }
        }
    }

    // --- NEW: Manual Scroll Driver ---
    public void scroll(float amountY) {
        if (scrollPane != null) {
            // 75f is slightly faster for the song list since it's much longer
            float newScroll = scrollPane.getScrollY() + (amountY * 75f);
            scrollPane.setScrollY(newScroll);
        }
    }
}
