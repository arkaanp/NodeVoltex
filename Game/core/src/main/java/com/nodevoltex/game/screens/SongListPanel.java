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

    // --- NEW: Camera Tracking Variables ---
    private Actor cameraTarget = null;
    private float cameraLerpTime = 0f;

    // --- NEW: Zero-Allocation Vectors for 60fps math! ---
    private final com.badlogic.gdx.math.Vector2 tempPos1 = new com.badlogic.gdx.math.Vector2();
    private final com.badlogic.gdx.math.Vector2 tempPos2 = new com.badlogic.gdx.math.Vector2();

    // --- Data Container Class ---
    private static class SongData {
        String title, artist, mapper, audioPath;
        int novLv = 0, advLv = 0, exhLv = 0, mxmLv = 0;
        String novPath, advPath, exhPath, mxmPath;
        float previewOffsetSeconds = 0f;
        String jacketPath;

        boolean hasDiffs() { return novLv > 0 || advLv > 0 || exhLv > 0 || mxmLv > 0; }
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
                        data.jacketPath = folder.path() + "/" + general.getString("jacketFilename", "jak.png");

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

        refreshSongList(false); // Initial load, no animation needed // Draw the UI based on loaded data
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
            cameraLerpTime = 0f;
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
                handleSongSelection(song);
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

        updateStatsPanel(song, selectedDiffName, defaultLv, defaultPath, true);

        // TRUE! User clicked a brand new song, trigger the cascade!
        refreshSongList(true);
    }

    private void updateStatsPanel(SongData song, String diffName, int level, String mapPath, boolean animateScores) {
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

        // --- Grab the exact counts ---
        int[] stats = calculateMapStats(mapPath);
        int displayNotes = stats[0];
        int displayHolds = stats[1];
        int totalLaserTicks = stats[2];

        statsPanel.updateSong(song.title, song.artist, diffName + " " + level, getColorForLevel(level),
            song.mapper, song.jacketPath, displayNotes, displayHolds, totalLaserTicks, loadedScores, animateScores);
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

                // TRUE: Coming from another screen, animate the cascade!
                updateStatsPanel(song, expectedDiff, matchedLevel, safePath, true);
                refreshSongList(true); // TRUE! Coming from another screen, animate it!
                return;
            }
        }

        System.out.println("WARNING: Could not find any song matching folder: " + targetFolder);
        selectRandomSong();
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        // === SMOOTH CAMERA TRACKER ===
        if (cameraTarget != null && cameraLerpTime < 1.0f) {
            float safeDelta = Math.min(delta, 0.03f);
            cameraLerpTime += safeDelta / 0.6f;
            float progress = com.badlogic.gdx.math.Interpolation.pow3Out.apply(Math.min(cameraLerpTime, 1f));

            songListTable.validate();

            // USE PRE-ALLOCATED VECTOR
            tempPos1.set(0, cameraTarget.getHeight() / 2f);
            cameraTarget.localToAscendantCoordinates(songListTable, tempPos1);

            float distanceToTop = songListTable.getHeight() - tempPos1.y;
            float targetScroll = distanceToTop - (scrollPane.getHeight() / 2f);

            float currentScroll = scrollPane.getScrollY();
            scrollPane.setScrollY(currentScroll + (targetScroll - currentScroll) * progress);
        }

        // --- THE SCROLL FIX: Continuously update the slant every frame! ---
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
            // 75f is slightly faster for the song list since it's much longer
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
    private int[] calculateMapStats(String mapPath) {
        int tapCount = 0;
        int holdCount = 0;
        int totalLaserTicks = 0;

        if (mapPath != null) {
            try {
                com.badlogic.gdx.files.FileHandle file = Gdx.files.internal(mapPath);
                if (file.exists()) {
                    com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(file);

                    // 1. Count Taps & Holds
                    com.badlogic.gdx.utils.JsonValue hitObjects = root.get("hitObjects");
                    if (hitObjects != null) {
                        for (com.badlogic.gdx.utils.JsonValue ho : hitObjects) {
                            String type = ho.getString("type", "TAP"); // Default to TAP
                            if (type.equals("TAP")) tapCount++;
                            else if (type.equals("HOLD")) holdCount++;
                        }
                    }

                    // 2. Count Lasers (Currently counts Nodes as a baseline)
                    com.badlogic.gdx.utils.JsonValue lasers = root.get("lasers");
                    if (lasers != null) {
                        com.badlogic.gdx.utils.JsonValue left = lasers.get("left");
                        if (left != null) {
                            for (com.badlogic.gdx.utils.JsonValue seq : left) {
                                com.badlogic.gdx.utils.JsonValue nodes = seq.get("nodes");
                                if (nodes != null) totalLaserTicks += nodes.size;
                            }
                        }
                        com.badlogic.gdx.utils.JsonValue right = lasers.get("right");
                        if (right != null) {
                            for (com.badlogic.gdx.utils.JsonValue seq : right) {
                                com.badlogic.gdx.utils.JsonValue nodes = seq.get("nodes");
                                if (nodes != null) totalLaserTicks += nodes.size;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to calculate map stats: " + e.getMessage());
            }
        }
        return new int[]{tapCount, holdCount, totalLaserTicks};
    }
}
