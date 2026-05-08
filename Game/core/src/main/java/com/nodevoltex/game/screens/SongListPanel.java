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

        boolean hasDiffs() { return novLv > 0 || advLv > 0 || exhLv > 0 || mxmLv > 0; }
    }

    public SongListPanel(NodeVoltex game, Skin skin, StatsPanel statsPanel) {
        this.game = game;
        this.skin = skin;
        this.statsPanel = statsPanel;

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
                        // --- NEW: Parse the mapper and audio ---
                        data.mapper = general.getString("mapper", data.mapper);
                        data.audioPath = folder.path() + "/" + general.getString("audioFilename", "audio.ogg");

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
                // The last argument 'true' tells LibGDX to perfectly center it vertically!
                scrollPane.scrollTo(0, currentlyExpandedActor.getY(), currentlyExpandedActor.getWidth(), currentlyExpandedActor.getHeight(), false, true);
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

        // 2. Play Audio
        playAudio(song.audioPath);

        // 3. Update Left Panel with JSON data
        updateStatsPanel(song, selectedDiffName, defaultLv, defaultPath);

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

    private void playAudio(String audioPath) {
        if (previewMusic != null) {
            previewMusic.stop();
            previewMusic.dispose();
            previewMusic = null;
        }
        try {
            if (audioPath != null) {
                FileHandle file = Gdx.files.internal(audioPath);
                if (file.exists()) {
                    previewMusic = Gdx.audio.newMusic(file);
                    previewMusic.setLooping(true);
                    previewMusic.setVolume(0.3f);
                    previewMusic.play();
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to play audio: " + audioPath);
        }
    }

    public void stopAudio() {
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
}
