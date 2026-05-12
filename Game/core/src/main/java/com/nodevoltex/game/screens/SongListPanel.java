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
    private Actor currentlyExpandedActor = null;
    private Music previewMusic;

    // --- NEW: Prevents spam-clicking during animations ---
    private boolean isTransitioning = false;
    private String currentPreviewPath = "";

    // --- NEW: Camera Tracking Variables ---
    private Actor cameraTarget = null;
    private float cameraLerpTime = 0f;

    // --- NEW: Zero-Allocation Vectors for 60fps math! ---
    private final com.badlogic.gdx.math.Vector2 tempPos1 = new com.badlogic.gdx.math.Vector2();
    private final com.badlogic.gdx.math.Vector2 tempPos2 = new com.badlogic.gdx.math.Vector2();

    // --- THE LAG FIX: Global Data Cache ---
    private static Array<SongData> GLOBAL_SONG_CACHE = null;

    // --- THE MEMORY FIX: Remembers your last song! ---
    private static String GLOBAL_LAST_PLAYED_PATH = null;

    // --- THE TWEAK FIX: Anti-Spam Timer ---
    private long lastSelectionTime = 0;

    // --- Data Container Class ---
    private static class SongData {
        String title, artist, mapper, audioPath;
        int novLv = 0, advLv = 0, exhLv = 0, mxmLv = 0;
        String novPath, advPath, exhPath, mxmPath;
        float previewOffsetSeconds = 0f;
        String jacketPath;

        // --- RAM Cache for Instant UI Loading ---
        int novNotes = 0, novHolds = 0, novLasers = 0;
        int advNotes = 0, advHolds = 0, advLasers = 0;
        int exhNotes = 0, exhHolds = 0, exhLasers = 0;
        int mxmNotes = 0, mxmHolds = 0, mxmLasers = 0;

        boolean hasDiffs() { return novLv > 0 || advLv > 0 || exhLv > 0 || mxmLv > 0; }
    }

    // --- NEW: The JSON structure for our permanent cache files ---
    public static class MapStatsCache {
        public int notes = 0;
        public int holds = 0;
        public int lasers = 0;
    }

    public SongListPanel(NodeVoltex game, Skin skin, StatsPanel statsPanel, com.badlogic.gdx.audio.Music mainMenuMusic) {
        this.game = game;
        this.skin = skin;
        this.statsPanel = statsPanel;
        this.mainMenuMusic = mainMenuMusic;

        // --- THE HOLY GRAIL FIX: Override the Table's core layout engine! ---
        songListTable = new Table() {
            @Override
            public void layout() {
                // 1. Let LibGDX calculate all the heights and vertical Y coordinates normally
                super.layout();

                // 2. Immediately inject our 5-degree slant before it draws to the screen!
                float tanAngle = (float) Math.tan(Math.toRadians(5f));
                applySlant(this, tanAngle);
            }
        };
        songListTable.top();

        scrollPane = new ScrollPane(songListTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // --- THE FIX: Break the camera lock if the user clicks and drags the list! ---
        scrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                cameraTarget = null;
                return super.touchDown(event, x, y, pointer, button);
            }
        });

        this.add(scrollPane).expand().fill().pad(10);
        loadSongsFromDirectory();
    }

    // --- UPDATED: The TRUE 0ms Global Cache ---
    private void loadSongsFromDirectory() {

        // 1. Check the GLOBAL cache! If it exists, skip the hard drive completely!
        // --- THE FIX: If the songs are already loaded in RAM, skip the hard drive completely! ---
        if (GLOBAL_SONG_CACHE != null && GLOBAL_SONG_CACHE.size > 0) {
            allSongs.addAll(GLOBAL_SONG_CACHE);
            refreshSongList(false);

            if (selectedSong == null) {
                // If we are returning from the Main Menu, load the exact song we had open last!
                if (GLOBAL_LAST_PLAYED_PATH != null) selectSongByPath(GLOBAL_LAST_PLAYED_PATH);
                else selectRandomSong();
            }
            return;
        }

        // 2. Otherwise, run the heavy folder-scanning task on a background thread
        new Thread(() -> {
            com.badlogic.gdx.files.FileHandle songsDir = Gdx.files.internal("assets/songs");
            if (!songsDir.exists() || !songsDir.isDirectory()) return;

            com.badlogic.gdx.utils.JsonReader jsonReader = new com.badlogic.gdx.utils.JsonReader();

            for (com.badlogic.gdx.files.FileHandle folder : songsDir.list()) {
                if (!folder.isDirectory()) continue;

                SongData data = new SongData();
                data.title = "Unknown Song";
                data.artist = "Unknown Artist";
                data.mapper = "Unknown Mapper";

                com.badlogic.gdx.files.FileHandle[] diffFiles = {
                    folder.child("nov.json"), folder.child("adv.json"),
                    folder.child("exh.json"), folder.child("mxm.json")
                };

                for (int i = 0; i < diffFiles.length; i++) {
                    if (diffFiles[i].exists()) {
                        try {
                            com.badlogic.gdx.utils.JsonValue root = jsonReader.parse(diffFiles[i]);
                            com.badlogic.gdx.utils.JsonValue general = root.get("general");

                            data.title = general.getString("title", data.title);
                            data.artist = general.getString("artist", data.artist);
                            data.mapper = general.getString("mapper", data.mapper);
                            data.audioPath = folder.path() + "/" + general.getString("audioFilename", "audio.ogg");
                            data.jacketPath = folder.path() + "/" + general.getString("jacketFilename", "jak.png");

                            int offsetMs = general.getInt("previewOffset", 0);
                            data.previewOffsetSeconds = offsetMs / 1000f;

                            int level = general.getInt("level", 0);

                            int notes = general.getInt("noteCount", 0);
                            int holds = general.getInt("holdCount", 0);
                            int lasers = general.getInt("laserCount", 0);

                            if (notes == 0 && holds == 0 && lasers == 0) {
                                String safeCacheName = folder.name() + "_" + diffFiles[i].nameWithoutExtension() + "_stats.json";
                                com.badlogic.gdx.files.FileHandle cacheFile = Gdx.files.local("cache/stats/" + safeCacheName);

                                if (cacheFile.exists()) {
                                    try {
                                        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                                        MapStatsCache cache = json.fromJson(MapStatsCache.class, cacheFile);
                                        notes = cache.notes; holds = cache.holds; lasers = cache.lasers;
                                    } catch(Exception e) {}
                                }

                                if (notes == 0 && holds == 0 && lasers == 0) {
                                    int[] calculatedStats = calculateMapStatsFromTree(root);
                                    notes = calculatedStats[0]; holds = calculatedStats[1]; lasers = calculatedStats[2];

                                    try {
                                        MapStatsCache newCache = new MapStatsCache();
                                        newCache.notes = notes; newCache.holds = holds; newCache.lasers = lasers;
                                        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                                        cacheFile.writeString(json.prettyPrint(newCache), false);
                                    } catch (Exception e) {}
                                }
                            }

                            if (i == 0) { data.novLv = level; data.novPath = diffFiles[i].path(); data.novNotes = notes; data.novHolds = holds; data.novLasers = lasers; }
                            if (i == 1) { data.advLv = level; data.advPath = diffFiles[i].path(); data.advNotes = notes; data.advHolds = holds; data.advLasers = lasers; }
                            if (i == 2) { data.exhLv = level; data.exhPath = diffFiles[i].path(); data.exhNotes = notes; data.exhHolds = holds; data.exhLasers = lasers; }
                            if (i == 3) { data.mxmLv = level; data.mxmPath = diffFiles[i].path(); data.mxmNotes = notes; data.mxmHolds = holds; data.mxmLasers = lasers; }
                        } catch (Exception e) {}
                    }
                }

                if (data.hasDiffs()) allSongs.add(data);
            }

            Gdx.app.postRunnable(() -> {
                // 3. Save the parsed arrays globally so we never parse again!
                GLOBAL_SONG_CACHE = new Array<>();
                GLOBAL_SONG_CACHE.addAll(allSongs);

                if (allSongs.size > 0 && selectedSong == null) {
                    // Open the last played song on first boot!
                    if (GLOBAL_LAST_PLAYED_PATH != null) selectSongByPath(GLOBAL_LAST_PLAYED_PATH);
                    else selectRandomSong();
                } else {
                    refreshSongList(false);
                }
            });

        }).start();
    }

    // Completely rebuilds the list, handling Expanded vs Collapsed states
    // --- UPDATED: Added boolean animateCascade ---
    private void refreshSongList(boolean animateCascade) {
        songListTable.clearChildren();
        currentlyExpandedActor = null;

        float fixedWidth = this.getWidth() - 100f;
        if (fixedWidth <= 0) fixedWidth = 800f;

        for (SongData song : allSongs) {
            Table item;
            if (song == selectedSong) {
                // Pass the flag down!
                item = buildExpandedItem(song, fixedWidth, animateCascade);
                currentlyExpandedActor = item;
            } else {
                item = buildCollapsedItem(song);
            }
            songListTable.add(item).width(fixedWidth).left().padBottom(5).row();
        }

        // --- THE FIX: Only lock the camera if we are animating a new song! ---
        if (currentlyExpandedActor != null && animateCascade) {
            cameraTarget = (Actor) currentlyExpandedActor.getUserObject();
            //cameraLerpTime = 0f;
        }
    }

    // View: When the song is NOT clicked
    private Table buildCollapsedItem(SongData song) {
        Table item = new Table();
        item.setName("slantHeader");
        item.background(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.7f)));

        Table textTable = new Table();
        textTable.left();
        textTable.add(new Label(song.title, skin)).align(Align.left).padBottom(2).row();

        Label artistLabel = new Label(song.artist, skin);
        artistLabel.setColor(Color.LIGHT_GRAY);
        textTable.add(artistLabel).align(Align.left);

        item.add(textTable).expandX().fillX().left().pad(10).padLeft(20).row();

        Table thinBars = new Table();
        Color novCol = song.novLv > 0 ? getColorForLevel(song.novLv) : Color.DARK_GRAY;
        Color advCol = song.advLv > 0 ? getColorForLevel(song.advLv) : Color.DARK_GRAY;
        Color exhCol = song.exhLv > 0 ? getColorForLevel(song.exhLv) : Color.DARK_GRAY;
        Color mxmCol = song.mxmLv > 0 ? getColorForLevel(song.mxmLv) : Color.DARK_GRAY;

        thinBars.add(new Image(skin.newDrawable("white", novCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", advCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", exhCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", mxmCol))).height(6).expandX().fillX();

        item.add(thinBars).expandX().fillX().padLeft(20).padRight(20).padBottom(5);

        item.setTouchable(Touchable.enabled);
        item.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                // --- THE FIX: Pass null for default highest difficulty ---
                handleSongSelection(song, null);
            }
        });
        return item;
    }

    // --- UPDATED: Added boolean animate ---
    // View: When the song IS clicked (Accordion open)
    private Table buildExpandedItem(SongData song, float fixedWidth, boolean animate) {
        Table wrapper = new Table();

        Table headerBox = new Table();
        headerBox.setName("slantHeader");
        headerBox.background(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.7f)));

        Table textTable = new Table();
        textTable.left();
        textTable.add(new Label(song.title, skin)).align(Align.left).padBottom(2).row();

        Label artistLabel = new Label(song.artist, skin);
        artistLabel.setColor(Color.LIGHT_GRAY);
        textTable.add(artistLabel).align(Align.left);

        headerBox.add(textTable).expandX().fillX().left().pad(10).padLeft(20).row();

        Table thinBars = new Table();
        Color novCol = song.novLv > 0 ? getColorForLevel(song.novLv) : Color.DARK_GRAY;
        Color advCol = song.advLv > 0 ? getColorForLevel(song.advLv) : Color.DARK_GRAY;
        Color exhCol = song.exhLv > 0 ? getColorForLevel(song.exhLv) : Color.DARK_GRAY;
        Color mxmCol = song.mxmLv > 0 ? getColorForLevel(song.mxmLv) : Color.DARK_GRAY;

        thinBars.add(new Image(skin.newDrawable("white", novCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", advCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", exhCol))).height(6).expandX().fillX().padRight(4);
        thinBars.add(new Image(skin.newDrawable("white", mxmCol))).height(6).expandX().fillX();
        headerBox.add(thinBars).expandX().fillX().padLeft(20).padRight(20).padBottom(5);

        // LOCK WIDTH
        wrapper.add(headerBox).width(fixedWidth).left().row();

        // --- THE FIX: Start the counter at 0! ---
        int cascadeIndex = 0;

        // --- THE FIX: Pass the boolean animate flag to the rows! ---
        if (song.novLv > 0) wrapper.add(createAnimatedDiffRow(song, "NOV", song.novLv, getColorForLevel(song.novLv), song.novPath, cascadeIndex++, fixedWidth, animate)).left().row();
        if (song.advLv > 0) wrapper.add(createAnimatedDiffRow(song, "ADV", song.advLv, getColorForLevel(song.advLv), song.advPath, cascadeIndex++, fixedWidth, animate)).left().row();
        if (song.exhLv > 0) wrapper.add(createAnimatedDiffRow(song, "EXH", song.exhLv, getColorForLevel(song.exhLv), song.exhPath, cascadeIndex++, fixedWidth, animate)).left().row();
        if (song.mxmLv > 0) wrapper.add(createAnimatedDiffRow(song, "MXM", song.mxmLv, getColorForLevel(song.mxmLv), song.mxmPath, cascadeIndex++, fixedWidth, animate)).left().row();

        // Hide the headerBox inside the Wrapper so the Camera can track it!
        wrapper.setUserObject(headerBox);
        return wrapper;
    }

    // --- TRUE CASCADE ANIMATION ---
    // --- UPDATED: Added boolean animate flag! ---
    private com.badlogic.gdx.scenes.scene2d.ui.Container<Table> createAnimatedDiffRow(SongData song, String diffName, int level, Color color, String mapPath, int delayIndex, float fixedWidth, boolean animate) {
        Table row = new Table();
        boolean isSelected = selectedDiffName.equals(diffName);
        float alpha = isSelected ? 0.8f : 0.3f;
        row.background(skin.newDrawable("white", new Color(color.r, color.g, color.b, alpha)));

        Label diffLabel = new Label(diffName + " " + level, skin);
        if (level >= 1 && level <= 12) diffLabel.setColor(Color.BLACK);
        else diffLabel.setColor(Color.WHITE);

        row.add(diffLabel).expandX().left().pad(8).padLeft(20);

        row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        row.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (selectedDiffName.equals(diffName)) {
                    stopAudio();
                    if (mapPath != null) game.setScreen(new PlayScreen(game, mapPath));
                } else {
                    selectedDiffName = diffName;

                    // --- THE FIX: Change this to TRUE! ---
                    // This forces the StatsPanel to cascade the scores every time you click a diff!
                    updateStatsPanel(song, diffName, level, mapPath, true);

                    // (Keep this false so the pink UI doesn't close and reopen)
                    refreshSongList(false);
                }
            }
        });

        float exactHeight = 35f;

        com.badlogic.gdx.scenes.scene2d.ui.Container<Table> clipWrapper = new com.badlogic.gdx.scenes.scene2d.ui.Container<>(row);
        clipWrapper.setName("slantDiff");
        clipWrapper.align(com.badlogic.gdx.utils.Align.top);

        clipWrapper.prefWidth(fixedWidth - 30f);
        clipWrapper.minHeight(0f);

        // --- THE FIX: Initial State Logic ---
        if (animate) {
            clipWrapper.setUserObject(800f);
            clipWrapper.prefHeight(0f);

            clipWrapper.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                float time = 0;
                float delay = delayIndex * 0.1f;
                float duration = 0.45f;
                @Override
                public boolean act(float delta) {
                    float safeDelta = Math.min(delta, 0.03f);
                    if (delay > 0) { delay -= safeDelta; return false; }

                    time += safeDelta;
                    float progress = com.badlogic.gdx.math.Interpolation.pow3Out.apply(Math.min(time / duration, 1f));

                    clipWrapper.prefHeight(exactHeight * progress);
                    clipWrapper.invalidateHierarchy();
                    clipWrapper.setUserObject(800f * (1f - progress));
                    return time >= duration;
                }
            });
        } else {
            // If we are just switching diffs, render it instantly fully open!
            clipWrapper.setUserObject(0f);
            clipWrapper.prefHeight(exactHeight);
        }

        return clipWrapper;
    }

    // Fired when you click a collapsed song
    // --- THE NEW ANIMATION MANAGER ---
    // --- THE ULTIMATE PARALLEL UI MANAGER ---
    // --- THE ULTIMATE PARALLEL UI MANAGER ---
    // --- THE SEQUENTIAL UI MANAGER (Prioritizing Smooth Closing) ---
    // --- THE ATOMIC BUNDLE MANAGER (Embracing the Freeze) ---
    // --- THE SEQUENTIAL UI MANAGER (Close -> Load -> Open) ---
    private void handleSongSelection(SongData song, String forceDiff) {
        if (isTransitioning || selectedSong == song) return;

        // --- THE TWEAK FIX: Anti-Spam Filter ---
        // If two commands try to pick a song within 100ms of each other on startup, destroy the second command!
        if (System.currentTimeMillis() - lastSelectionTime < 100) return;
        lastSelectionTime = System.currentTimeMillis();

        // 1. Calculate the target difficulty instantly
        int defaultLv = 0; String defaultPath = null; String targetDiff = "";

        if (forceDiff != null) {
            targetDiff = forceDiff;
            if (forceDiff.equals("MXM")) { defaultLv = song.mxmLv; defaultPath = song.mxmPath; }
            else if (forceDiff.equals("EXH")) { defaultLv = song.exhLv; defaultPath = song.exhPath; }
            else if (forceDiff.equals("ADV")) { defaultLv = song.advLv; defaultPath = song.advPath; }
            else if (forceDiff.equals("NOV")) { defaultLv = song.novLv; defaultPath = song.novPath; }
        } else {
            if (song.mxmLv > 0) { targetDiff = "MXM"; defaultLv = song.mxmLv; defaultPath = song.mxmPath; }
            else if (song.exhLv > 0) { targetDiff = "EXH"; defaultLv = song.exhLv; defaultPath = song.exhPath; }
            else if (song.advLv > 0) { targetDiff = "ADV"; defaultLv = song.advLv; defaultPath = song.advPath; }
            else if (song.novLv > 0) { targetDiff = "NOV"; defaultLv = song.novLv; defaultPath = song.novPath; }
        }

        final String finalDiff = targetDiff;
        final int finalLv = defaultLv;
        final String finalPath = defaultPath;

        isTransitioning = true; // Lock the UI!

        // 2. Define the Loading & Opening Task
        Runnable loadAndOpenTask = new Runnable() {
            @Override
            public void run() {
                playAudio(song.audioPath, song.previewOffsetSeconds);
                updateStatsPanel(song, finalDiff, finalLv, finalPath, true);

                selectedSong = song;
                selectedDiffName = finalDiff;

                // --- THE MEMORY FIX: Save this song to memory so we don't lose it! ---
                GLOBAL_LAST_PLAYED_PATH = finalPath != null ? finalPath : song.novPath;

                refreshSongList(true);
                isTransitioning = false;
            }
        };

        // ... (Keep the rest of your closing animation Action code exactly the same below here) ...

        // 3. Play the Reverse Cascade (Closing Animation)
        if (currentlyExpandedActor != null) {

            com.badlogic.gdx.utils.Array<com.badlogic.gdx.scenes.scene2d.Actor> diffRows = new com.badlogic.gdx.utils.Array<>();
            for (com.badlogic.gdx.scenes.scene2d.Actor child : ((Table) currentlyExpandedActor).getChildren()) {
                if ("slantDiff".equals(child.getName())) diffRows.add(child);
            }

            if (diffRows.size > 0) {
                float duration = 0.25f;
                float maxDelay = 0f;

                for (int i = 0; i < diffRows.size; i++) {
                    com.badlogic.gdx.scenes.scene2d.Actor diffRow = diffRows.get(i);
                    if (diffRow instanceof com.badlogic.gdx.scenes.scene2d.ui.Container) {
                        com.badlogic.gdx.scenes.scene2d.ui.Container clipWrapper = (com.badlogic.gdx.scenes.scene2d.ui.Container) diffRow;
                        float startHeight = clipWrapper.getPrefHeight();

                        final float rowDelay = (diffRows.size - 1 - i) * 0.05f;
                        maxDelay = Math.max(maxDelay, rowDelay);

                        clipWrapper.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                            float time = 0;
                            float currentDelay = rowDelay;
                            @Override
                            public boolean act(float delta) {
                                // --- THE FIX: Capped Safe Delta ---
                                // This completely prevents the "moves altogether" bug!
                                float safeDelta = Math.min(delta, 0.03f);

                                if (currentDelay > 0) { currentDelay -= safeDelta; return false; }

                                time += safeDelta;
                                float progress = com.badlogic.gdx.math.Interpolation.pow3In.apply(Math.min(time / duration, 1f));

                                clipWrapper.prefHeight(startHeight * (1f - progress));
                                clipWrapper.invalidateHierarchy();
                                clipWrapper.setUserObject(800f * progress);

                                return time >= duration;
                            }
                        });
                    }
                }

                float totalWaitTime = maxDelay + duration;

                // 4. WAIT FOR THE CLOSING ANIMATION TO PERFECTLY FINISH, THEN LOAD!
                this.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(totalWaitTime),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.run(loadAndOpenTask)
                ));
            } else {
                loadAndOpenTask.run(); // Failsafe
            }
        } else {
            loadAndOpenTask.run(); // First boot
        }
    }

    // --- THE DATA SWAPPER ---
    private void finalizeSongSelection(SongData song, String forceDiff) {
        selectedSong = song;

        int defaultLv = 0;
        String defaultPath = null;

        // Use forced difficulty if provided, otherwise default to highest
        if (forceDiff != null) {
            selectedDiffName = forceDiff;
            if (forceDiff.equals("MXM")) { defaultLv = song.mxmLv; defaultPath = song.mxmPath; }
            else if (forceDiff.equals("EXH")) { defaultLv = song.exhLv; defaultPath = song.exhPath; }
            else if (forceDiff.equals("ADV")) { defaultLv = song.advLv; defaultPath = song.advPath; }
            else if (forceDiff.equals("NOV")) { defaultLv = song.novLv; defaultPath = song.novPath; }
        } else {
            if (song.mxmLv > 0) { selectedDiffName = "MXM"; defaultLv = song.mxmLv; defaultPath = song.mxmPath; }
            else if (song.exhLv > 0) { selectedDiffName = "EXH"; defaultLv = song.exhLv; defaultPath = song.exhPath; }
            else if (song.advLv > 0) { selectedDiffName = "ADV"; defaultLv = song.advLv; defaultPath = song.advPath; }
            else if (song.novLv > 0) { selectedDiffName = "NOV"; defaultLv = song.novLv; defaultPath = song.novPath; }
        }

        playAudio(song.audioPath, song.previewOffsetSeconds);
        updateStatsPanel(song, selectedDiffName, defaultLv, defaultPath, true);
        refreshSongList(true); // Triggers the OPEN cascade for the new song

        isTransitioning = false; // Unlock the UI!
    }

    // --- 100% Instant, No I/O Lag ---
    // --- 100% Non-Blocking Async Stats Panel! ---
    // --- UPDATED: Staggered JSON Parsing ---
    private void updateStatsPanel(SongData song, String diffName, int level, String mapPath, boolean animateScores) {

        int displayNotes = 0, displayHolds = 0, totalLaserTicks = 0;
        if (diffName.equals("NOV")) { displayNotes = song.novNotes; displayHolds = song.novHolds; totalLaserTicks = song.novLasers; }
        else if (diffName.equals("ADV")) { displayNotes = song.advNotes; displayHolds = song.advHolds; totalLaserTicks = song.advLasers; }
        else if (diffName.equals("EXH")) { displayNotes = song.exhNotes; displayHolds = song.exhHolds; totalLaserTicks = song.exhLasers; }
        else if (diffName.equals("MXM")) { displayNotes = song.mxmNotes; displayHolds = song.mxmHolds; totalLaserTicks = song.mxmLasers; }

        statsPanel.updateSong(song.title, song.artist, diffName + " " + level, getColorForLevel(level),
            song.mapper, song.jacketPath, displayNotes, displayHolds, totalLaserTicks);

        if (mapPath != null) {
            Thread jsonThread = new Thread(() -> {
                // 1. STAGGER: Execute last so the CPU is completely free!
                try { Thread.sleep(90); } catch (Exception e) {}

                com.badlogic.gdx.utils.Array<com.nodevoltex.game.data.SaveData> loadedScores = new com.badlogic.gdx.utils.Array<>();
                String safeFileName = mapPath.replace("/", "_").replace("\\", "_") + "_save.json";
                com.badlogic.gdx.files.FileHandle saveFile = Gdx.files.local("assets/scores/" + safeFileName);

                if (saveFile.exists()) {
                    try {
                        com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
                        com.nodevoltex.game.data.ScoreHistory history = json.fromJson(com.nodevoltex.game.data.ScoreHistory.class, saveFile);
                        if (history != null && history.plays != null) loadedScores.addAll(history.plays);
                    } catch (Exception e) {}
                }

                Gdx.app.postRunnable(() -> {
                    if (selectedSong == song && selectedDiffName.equals(diffName)) {
                        statsPanel.injectScoresAsync(loadedScores, animateScores);
                    }
                });
            });
            jsonThread.setPriority(Thread.MIN_PRIORITY);
            jsonThread.start();
        } else {
            statsPanel.injectScoresAsync(null, animateScores);
        }
    }

    // --- UPDATED: Background Threading for Seamless Audio Transitions! ---
    // --- UPDATED: Safe, Parallel Audio Loading ---
    // --- UPDATED: Yielding Audio Thread ---
    private void playAudio(String audioPath, float offsetSeconds) {
        if (audioPath == null) return;
        currentPreviewPath = audioPath;

        Thread audioThread = new Thread(() -> {
            // 1. YIELD TO UI: Let the closing animation visually start!
            try { Thread.sleep(30); } catch (Exception e) {}

            com.badlogic.gdx.files.FileHandle file = Gdx.files.internal(audioPath);
            if (!file.exists()) return;

            try {
                final com.badlogic.gdx.audio.Music newMusic = Gdx.audio.newMusic(file);

                Gdx.app.postRunnable(() -> {
                    if (currentPreviewPath.equals(audioPath)) {
                        if (mainMenuMusic != null) { mainMenuMusic.stop(); mainMenuMusic.dispose(); mainMenuMusic = null; }
                        if (previewMusic != null) { previewMusic.stop(); previewMusic.dispose(); }

                        previewMusic = newMusic;
                        previewMusic.setLooping(true);
                        previewMusic.setVolume(0.3f);
                        previewMusic.play();

                        if (offsetSeconds > 0) previewMusic.setPosition(offsetSeconds);
                    } else {
                        newMusic.dispose();
                    }
                });
            } catch (Exception e) {}
        });

        // 2. PRIORITY DROP: Tell the OS the Main Render Thread is more important!
        audioThread.setPriority(Thread.MIN_PRIORITY);
        audioThread.start();
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
        refreshSongList(false); // Sorting, no cascade needed

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
            handleSongSelection(allSongs.get(randomIndex), null);
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

                // --- THE FIX: Let the centralized manager handle EVERYTHING automatically! ---
                handleSongSelection(song, expectedDiff);
                return;
            }
        }

        System.out.println("WARNING: Could not find any song matching folder: " + targetFolder);
        selectRandomSong();
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        // === THE FIX: Auto-Unlocking Camera ===
        if (cameraTarget != null) {
            float safeDelta = Math.min(delta, 0.03f);

            songListTable.validate();
            tempPos1.set(0, cameraTarget.getHeight() / 2f);
            cameraTarget.localToAscendantCoordinates(songListTable, tempPos1);

            float distanceToTop = songListTable.getHeight() - tempPos1.y;
            float targetScroll = distanceToTop - (scrollPane.getHeight() / 2f);

            float currentScroll = scrollPane.getScrollY();
            float distance = targetScroll - currentScroll;

            // 1. If we are within 2 pixels, snap into place and KILL the camera lock!
            if (Math.abs(distance) < 2f) {
                scrollPane.setScrollY(targetScroll);
                cameraTarget = null;
            } else {
                // 2. Otherwise, continue smoothing gliding
                float newScroll = currentScroll + (distance * (safeDelta * 12f));
                scrollPane.setScrollY(newScroll);
            }
        }

        // --- Continuous Slant Engine ---
        float tanAngle = (float) Math.tan(Math.toRadians(5f));
        if (songListTable != null) {
            applySlant(songListTable, tanAngle);
        }
    }

    // --- FIXED: Zero-Allocation Absolute Slant Engine ---
    private void applySlant(com.badlogic.gdx.scenes.scene2d.Group group, float tanAngle) {
        float baseLeftX = 40f;

        for (com.badlogic.gdx.scenes.scene2d.Actor child : group.getChildren()) {

            if ("slantHeader".equals(child.getName()) || "slantDiff".equals(child.getName())) {

                // 1. Get exact distance down the screen using tempPos1
                tempPos1.set(0, child.getHeight());
                child.localToAscendantCoordinates(SongListPanel.this, tempPos1);
                float distanceDown = SongListPanel.this.getHeight() - tempPos1.y;

                // 2. Diagonal math
                float absoluteX = baseLeftX + (distanceDown * tanAngle);

                // Indents & Slide animations
                if ("slantDiff".equals(child.getName())) absoluteX += 30f;
                if (child.getUserObject() instanceof Float) {
                    absoluteX += (Float) child.getUserObject();
                }

                // 3. Convert Screen X to Local X using tempPos2
                tempPos2.set(absoluteX, 0);
                SongListPanel.this.localToStageCoordinates(tempPos2);
                child.getParent().stageToLocalCoordinates(tempPos2);

                // Lock it in!
                child.setX(tempPos2.x);
            }

            // Recursion is perfectly safe with these temp vectors!
            if (child instanceof com.badlogic.gdx.scenes.scene2d.Group) {
                applySlant((com.badlogic.gdx.scenes.scene2d.Group) child, tanAngle);
            }
        }
    }

    // --- Manual Scroll Driver ---
    public void scroll(float amountY) {
        if (scrollPane != null) {
            // --- THE FIX: Break the camera lock instantly if the user uses the mouse wheel! ---
            cameraTarget = null;

            float newScroll = scrollPane.getScrollY() + (amountY * 75f);
            scrollPane.setScrollY(newScroll);
        }
    }

    // --- Centralized Level Color Logic ---
    private Color getColorForLevel(int level) {
        if (level >= 1 && level <= 6) return Color.valueOf("#c1ff72");
        if (level >= 7 && level <= 12) return Color.valueOf("#599f00");
        if (level >= 13 && level <= 14) return Color.valueOf("#ff751f");
        if (level >= 15 && level <= 16) return Color.valueOf("#da142b");
        if (level == 17) return Color.valueOf("#003794");
        if (level == 18) return Color.valueOf("#120484");
        if (level == 19) return Color.valueOf("#2c0640");
        if (level >= 20) return Color.valueOf("#000000");

        return Color.DARK_GRAY; // Fallback for 0 or missing levels
    }

    // --- Calculate map stats instantly on selection ---
    // --- Calculates stats directly from RAM, no hard drive reading ---
    private int[] calculateMapStatsFromTree(JsonValue root) {
        int tapCount = 0;
        int holdCount = 0;
        int totalLaserTicks = 0;

        try {
            JsonValue hitObjects = root.get("hitObjects");
            if (hitObjects != null) {
                for (JsonValue ho : hitObjects) {
                    String type = ho.getString("type", "TAP");
                    if (type.equals("TAP")) tapCount++;
                    else if (type.equals("HOLD")) holdCount++;
                }
            }

            JsonValue lasers = root.get("lasers");
            if (lasers != null) {
                JsonValue left = lasers.get("left");
                if (left != null) {
                    for (JsonValue seq : left) {
                        JsonValue nodes = seq.get("nodes");
                        if (nodes != null) totalLaserTicks += nodes.size;
                    }
                }
                JsonValue right = lasers.get("right");
                if (right != null) {
                    for (JsonValue seq : right) {
                        JsonValue nodes = seq.get("nodes");
                        if (nodes != null) totalLaserTicks += nodes.size;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to calculate map stats from tree.");
        }

        return new int[]{tapCount, holdCount, totalLaserTicks};
    }
}
