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

    private Array<SongData> allSongs = new Array<>();
    private SongData selectedSong = null;
    private String selectedDiffName = "";
    private Actor currentlyExpandedActor = null;

    private boolean isTransitioning = false;
    private Actor cameraTarget = null;
    private float cameraLerpTime = 0f;
    private String searchQuery = "";

    private final com.badlogic.gdx.math.Vector2 tempPos1 = new com.badlogic.gdx.math.Vector2();
    private final com.badlogic.gdx.math.Vector2 tempPos2 = new com.badlogic.gdx.math.Vector2();

    private static com.badlogic.gdx.audio.Music mainMenuMusic;
    private static Music previewMusic;
    private static String currentPreviewPath = "";

    private static Array<SongData> GLOBAL_SONG_CACHE = null;
    private static String GLOBAL_LAST_PLAYED_PATH = null;
    private static String GLOBAL_LAST_PLAYED_DIFFICULTY = null;
    private long lastSelectionTime = 0;

    // --- Dual 9-Patch Memory for Borders & Fills ---
    private static com.badlogic.gdx.graphics.g2d.NinePatch normalPatch;
    private static com.badlogic.gdx.graphics.g2d.NinePatch outlinePatch;
    private static com.badlogic.gdx.graphics.Texture roundedTexture;


    private static class SongData {
        String title, artist, mapper, audioPath;
        int novLv = 0, advLv = 0, exhLv = 0, mxmLv = 0;
        String novPath, advPath, exhPath, mxmPath;
        float previewOffsetSeconds = 0f;
        String jacketPath;

        int novNotes = 0, novHolds = 0, novLasers = 0;
        int advNotes = 0, advHolds = 0, advLasers = 0;
        int exhNotes = 0, exhHolds = 0, exhLasers = 0;
        int mxmNotes = 0, mxmHolds = 0, mxmLasers = 0;

        boolean hasDiffs() { return novLv > 0 || advLv > 0 || exhLv > 0 || mxmLv > 0; }
    }

    public static class MapStatsCache {
        public int notes = 0;
        public int holds = 0;
        public int lasers = 0;
    }

    public SongListPanel(NodeVoltex game, Skin skin, StatsPanel statsPanel, com.badlogic.gdx.audio.Music mainMenuMusicIn) {
        this.game = game;
        this.skin = skin;
        this.statsPanel = statsPanel;

        if (mainMenuMusicIn != null) mainMenuMusic = mainMenuMusicIn;

        songListTable = new Table() {
            @Override
            public void layout() {
                super.layout();
                float tanAngle = (float) Math.tan(Math.toRadians(5f));
                applySlant(this, tanAngle);
            }
        };
        songListTable.top();

        scrollPane = new ScrollPane(songListTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

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

    // --- Advanced Bordered 9-Patch Generator ---
    private static void initPatches() {
        if (normalPatch != null) return;
        int radius = 10;
        int size = radius * 2 + 1;

        // 1. Solid Normal Patch
        com.badlogic.gdx.graphics.Pixmap normalPix = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        normalPix.setColor(Color.WHITE);
        normalPix.fillCircle(radius, radius, radius);
        com.badlogic.gdx.graphics.Texture normalTex = new com.badlogic.gdx.graphics.Texture(normalPix);
        normalTex.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        normalPatch = new com.badlogic.gdx.graphics.g2d.NinePatch(normalTex, radius, radius, radius, radius);
        normalPix.dispose();

        // 2. Solid Patch with a Colored Border
        com.badlogic.gdx.graphics.Pixmap outlinePix = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        // Changed from BLACK to GRAY. When tinted, this becomes a darker shade of the fill color
        outlinePix.setColor(Color.GRAY);
        outlinePix.fillCircle(radius, radius, radius);
        outlinePix.setColor(Color.WHITE);
        outlinePix.fillCircle(radius, radius, radius - 2);

        com.badlogic.gdx.graphics.Texture outlineTex = new com.badlogic.gdx.graphics.Texture(outlinePix);
        outlineTex.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        outlinePatch = new com.badlogic.gdx.graphics.g2d.NinePatch(outlineTex, radius, radius, radius, radius);
        outlinePix.dispose();
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createRoundedBackground(Color color) {
        if (normalPatch == null) {
            int radius = 10;
            int size = radius * 2 + 1;

            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(radius, radius, radius);

            roundedTexture = new com.badlogic.gdx.graphics.Texture(pixmap);
            roundedTexture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
            normalPatch = new com.badlogic.gdx.graphics.g2d.NinePatch(roundedTexture, radius, radius, radius, radius);
            pixmap.dispose();
        }
        return new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(normalPatch).tint(color);
    }

    private void loadSongsFromDirectory() {
        if (GLOBAL_SONG_CACHE != null && GLOBAL_SONG_CACHE.size > 0) {
            allSongs.addAll(GLOBAL_SONG_CACHE);
            refreshSongList(false);

            if (selectedSong == null) {
                if (GLOBAL_LAST_PLAYED_PATH != null) selectSongByPath(GLOBAL_LAST_PLAYED_PATH);
                else selectRandomSong();
            }
            return;
        }

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
                GLOBAL_SONG_CACHE = new Array<>();
                GLOBAL_SONG_CACHE.addAll(allSongs);

                if (allSongs.size > 0 && selectedSong == null) {
                    if (GLOBAL_LAST_PLAYED_PATH != null) selectSongByPath(GLOBAL_LAST_PLAYED_PATH);
                    else selectRandomSong();
                } else {
                    refreshSongList(false);
                }
            });

        }).start();
    }

    private void refreshSongList(boolean animateCascade) {
        songListTable.clearChildren();
        currentlyExpandedActor = null;

        float fixedWidth = this.getWidth() - 100f;
        if (fixedWidth <= 0) fixedWidth = 800f;

        for (SongData song : allSongs) {
            if (!searchQuery.isEmpty()) {
                if (!song.title.toLowerCase().contains(searchQuery) &&
                    !song.artist.toLowerCase().contains(searchQuery) &&
                    !song.mapper.toLowerCase().contains(searchQuery)) {
                    continue;
                }
            }
            Table item;
            if (song == selectedSong) {
                item = buildExpandedItem(song, fixedWidth, animateCascade);
                currentlyExpandedActor = item;
            } else {
                item = buildCollapsedItem(song);
            }
            songListTable.add(item).width(fixedWidth).left().padBottom(5).row();
        }

        songListTable.validate();

        if (currentlyExpandedActor != null && animateCascade) {
            cameraTarget = (Actor) currentlyExpandedActor.getUserObject();
        } else {
            cameraTarget = null;
        }
    }

    private Table buildCollapsedItem(SongData song) {
        final Table item = new Table();
        item.setName("slantHeader");

        // --- Soft hover, no outline ---
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = createRoundedBackground(new Color(0.15f, 0.15f, 0.2f, 0.7f));
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = createRoundedBackground(new Color(0.12f, 0.12f, 0.17f, 0.8f)); // Soft hover

        item.setBackground(normalBg);

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
        final ClickListener listener = new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                handleSongSelection(song, null);
            }
        };
        item.addListener(listener);

        item.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (listener.isOver() && !isTransitioning) item.setBackground(hoverBg);
                else item.setBackground(normalBg);
                return false;
            }
        });

        return item;
    }

    private Table buildExpandedItem(SongData song, float fixedWidth, boolean animate) {
        Table wrapper = new Table();

        Table headerBox = new Table();
        headerBox.setName("slantHeader");

        // --- Selected Header (Slightly darker/more opaque, no outline) ---
        headerBox.background(createRoundedBackground(new Color(0.15f, 0.15f, 0.2f, 0.9f)));

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

        wrapper.add(headerBox).width(fixedWidth).left().row();

        int cascadeIndex = 0;

        if (song.novLv > 0) wrapper.add(createAnimatedDiffRow(song, "NOV", song.novLv, getColorForLevel(song.novLv), song.novPath, cascadeIndex++, fixedWidth, animate)).left().row();
        if (song.advLv > 0) wrapper.add(createAnimatedDiffRow(song, "ADV", song.advLv, getColorForLevel(song.advLv), song.advPath, cascadeIndex++, fixedWidth, animate)).left().row();
        if (song.exhLv > 0) wrapper.add(createAnimatedDiffRow(song, "EXH", song.exhLv, getColorForLevel(song.exhLv), song.exhPath, cascadeIndex++, fixedWidth, animate)).left().row();
        if (song.mxmLv > 0) wrapper.add(createAnimatedDiffRow(song, "MXM", song.mxmLv, getColorForLevel(song.mxmLv), song.mxmPath, cascadeIndex++, fixedWidth, animate)).left().row();

        wrapper.setUserObject(headerBox);
        return wrapper;
    }

    private com.badlogic.gdx.scenes.scene2d.ui.Container<Table> createAnimatedDiffRow(SongData song, String diffName, int level, Color color, String mapPath, int delayIndex, float fixedWidth, boolean animate) {
        final Table diffBox = new Table(); // This is the physical box

        // --- Diff Box States (No Outlines) ---
        Color dimCol = new Color(color.r * 0.6f, color.g * 0.6f, color.b * 0.6f, 0.4f);
        Color hoverCol = new Color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 0.5f); // Soft hover
        Color activeCol = new Color(color.r, color.g, color.b, 0.85f); // Bright active color

        final com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = createRoundedBackground(dimCol);
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = createRoundedBackground(hoverCol);
        final com.badlogic.gdx.scenes.scene2d.utils.Drawable activeBg = createRoundedBackground(activeCol);

        Label diffLabel = new Label(diffName + " " + level, skin);
        if (level >= 1 && level <= 12) diffLabel.setColor(Color.BLACK);
        else diffLabel.setColor(Color.WHITE);

        diffBox.add(diffLabel).expandX().left().pad(8).padLeft(20);

        diffBox.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        final ClickListener listener = new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (selectedDiffName.equals(diffName)) {
                    stopAudio();
                    if (mapPath != null) {
                        if (game.getScreen() instanceof SongSelectScreen) {
                            ((SongSelectScreen) game.getScreen()).animateOutToPlayScreen(mapPath);
                        } else {
                            game.setScreen(new PlayScreen(game, mapPath));
                        }
                    }
                } else {
                    selectedDiffName = diffName;
                    updateStatsPanel(song, diffName, level, mapPath, true);
                    refreshSongList(false);
                }
            }
        };
        diffBox.addListener(listener);

        diffBox.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (selectedDiffName.equals(diffName)) diffBox.setBackground(activeBg);
                else if (listener.isOver() && !isTransitioning) diffBox.setBackground(hoverBg);
                else diffBox.setBackground(normalBg);
                return false;
            }
        });

        // --- Spacing Table ---
        // We wrap the colored diffBox inside a transparent margin table.
        // The padTop(5f) creates a physical 5 pixel gap ABOVE every single diff box
        Table marginTable = new Table();
        marginTable.add(diffBox).expandX().fillX().height(35f).padTop(5f);

        // The exact animated height is now 40f (35f box + 5f gap)
        float exactHeight = 40f;

        com.badlogic.gdx.scenes.scene2d.ui.Container<Table> clipWrapper = new com.badlogic.gdx.scenes.scene2d.ui.Container<>(marginTable);
        clipWrapper.setName("slantDiff");
        clipWrapper.align(com.badlogic.gdx.utils.Align.top);

        clipWrapper.prefWidth(fixedWidth - 30f);
        clipWrapper.minHeight(0f);

        if (animate) {
            clipWrapper.setUserObject(800f);
            clipWrapper.prefHeight(0f);
            clipWrapper.setHeight(0f);

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

                    float newH = exactHeight * progress;
                    clipWrapper.prefHeight(newH);
                    clipWrapper.setHeight(newH);
                    clipWrapper.invalidateHierarchy();
                    clipWrapper.setUserObject(800f * (1f - progress));
                    return time >= duration;
                }
            });
        } else {
            clipWrapper.setUserObject(0f);
            clipWrapper.prefHeight(exactHeight);
            clipWrapper.setHeight(exactHeight);
        }

        return clipWrapper;
    }

    private void handleSongSelection(SongData song, String forceDiff) {
        if (isTransitioning || selectedSong == song) return;

        if (System.currentTimeMillis() - lastSelectionTime < 100) return;
        lastSelectionTime = System.currentTimeMillis();

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

        isTransitioning = true;

        Runnable loadAndOpenTask = new Runnable() {
            @Override
            public void run() {
                playAudio(song.audioPath, song.previewOffsetSeconds);
                updateStatsPanel(song, finalDiff, finalLv, finalPath, true);

                selectedSong = song;
                selectedDiffName = finalDiff;
                GLOBAL_LAST_PLAYED_PATH = finalPath != null ? finalPath : song.novPath;

                refreshSongList(true);
                isTransitioning = false;
            }
        };

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
                        float startHeight = clipWrapper.getHeight();

                        final float rowDelay = (diffRows.size - 1 - i) * 0.05f;
                        maxDelay = Math.max(maxDelay, rowDelay);

                        clipWrapper.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
                            float time = 0;
                            float currentDelay = rowDelay;
                            @Override
                            public boolean act(float delta) {
                                float safeDelta = Math.min(delta, 0.03f);

                                if (currentDelay > 0) { currentDelay -= safeDelta; return false; }

                                time += safeDelta;
                                float progress = com.badlogic.gdx.math.Interpolation.pow3In.apply(Math.min(time / duration, 1f));

                                float newH = startHeight * (1f - progress);
                                clipWrapper.prefHeight(newH);
                                clipWrapper.setHeight(newH);
                                clipWrapper.invalidateHierarchy();
                                clipWrapper.setUserObject(800f * progress);

                                return time >= duration;
                            }
                        });
                    }
                }

                float totalWaitTime = maxDelay + duration;
                this.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(totalWaitTime),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.run(loadAndOpenTask)
                ));
            } else {
                loadAndOpenTask.run();
            }
        } else {
            loadAndOpenTask.run();
        }
    }

    private void finalizeSongSelection(SongData song, String forceDiff) {
        selectedSong = song;

        int defaultLv = 0;
        String defaultPath = null;

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
        refreshSongList(true);

        isTransitioning = false;
    }

    private void updateStatsPanel(SongData song, String diffName, int level, String mapPath, boolean animateScores) {
        int displayNotes = 0, displayHolds = 0, totalLaserTicks = 0;
        if (diffName.equals("NOV")) { displayNotes = song.novNotes; displayHolds = song.novHolds; totalLaserTicks = song.novLasers; }
        else if (diffName.equals("ADV")) { displayNotes = song.advNotes; displayHolds = song.advHolds; totalLaserTicks = song.advLasers; }
        else if (diffName.equals("EXH")) { displayNotes = song.exhNotes; displayHolds = song.exhHolds; totalLaserTicks = song.exhLasers; }
        else if (diffName.equals("MXM")) { displayNotes = song.mxmNotes; displayHolds = song.mxmHolds; totalLaserTicks = song.mxmLasers; }

        statsPanel.updateSong(song.title, song.artist, diffName + " " + level, getColorForLevel(level),
            song.mapper, song.jacketPath, displayNotes, displayHolds, totalLaserTicks);

        statsPanel.currentMapPath = mapPath;

        if (mapPath != null) {
            Thread jsonThread = new Thread(() -> {
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

    private void playAudio(String audioPath, float offsetSeconds) {
        if (audioPath == null) return;

        if (audioPath.equals(currentPreviewPath) && previewMusic != null && previewMusic.isPlaying()) {
            return;
        }

        currentPreviewPath = audioPath;

        Thread audioThread = new Thread(() -> {
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
                        previewMusic.setVolume(com.nodevoltex.game.managers.SettingsManager.getMasterVolume() * com.nodevoltex.game.managers.SettingsManager.getMusicVolume());
                        previewMusic.play();

                        if (offsetSeconds > 0) previewMusic.setPosition(offsetSeconds);
                    } else {
                        newMusic.dispose();
                    }
                });
            } catch (Exception e) {}
        });

        audioThread.setPriority(Thread.MIN_PRIORITY);
        audioThread.start();
    }

    public void stopAudio() {
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
        currentPreviewPath = "";
    }

    public void sortSongs(String criteria) {
        if (criteria.equals("title")) {
            allSongs.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
        } else if (criteria.equals("artist")) {
            allSongs.sort((a, b) -> a.artist.compareToIgnoreCase(b.artist));
        } else if (criteria.equals("mapper")) {
            allSongs.sort((a, b) -> a.mapper.compareToIgnoreCase(b.mapper));
        }
        refreshSongList(false);

        Gdx.app.postRunnable(() -> scrollPane.setScrollY(0));
    }

    public void requestScrollFocus(com.badlogic.gdx.scenes.scene2d.Stage stage) {
        stage.setScrollFocus(this.scrollPane);
    }

    public void selectRandomSong() {
        if (allSongs.size > 0) {
            int randomIndex = com.badlogic.gdx.math.MathUtils.random(allSongs.size - 1);
            handleSongSelection(allSongs.get(randomIndex), null);
        }
    }

    public static void setLastPlayedDifficulty(String difficulty) {
        GLOBAL_LAST_PLAYED_DIFFICULTY = difficulty;
    }

    public static String getLastPlayedDifficulty() {
        return GLOBAL_LAST_PLAYED_DIFFICULTY;
    }

    public void selectSongByPath(String targetPath) {
        if (targetPath == null || targetPath.trim().isEmpty()) {
            selectRandomSong();
            return;
        }

        String target = targetPath.replace("\\", "/").toLowerCase();
        String expectedDiff = "NOV";
        if (target.endsWith("mxm.json") || target.contains("/mxm.json")) expectedDiff = "MXM";
        else if (target.endsWith("exh.json") || target.contains("/exh.json")) expectedDiff = "EXH";
        else if (target.endsWith("adv.json") || target.contains("/adv.json")) expectedDiff = "ADV";

        if (GLOBAL_LAST_PLAYED_DIFFICULTY != null && !GLOBAL_LAST_PLAYED_DIFFICULTY.isEmpty()) {
            expectedDiff = GLOBAL_LAST_PLAYED_DIFFICULTY;
            GLOBAL_LAST_PLAYED_DIFFICULTY = null;
        }

        String[] targetParts = target.split("/");
        String targetFolder = targetParts.length >= 2 ? targetParts[targetParts.length - 2] : target;

        for (SongData song : allSongs) {
            String nov = song.novPath != null ? song.novPath.replace("\\", "/").toLowerCase() : "";
            String adv = song.advPath != null ? song.advPath.replace("\\", "/").toLowerCase() : "";
            String exh = song.exhPath != null ? song.exhPath.replace("\\", "/").toLowerCase() : "";
            String mxm = song.mxmPath != null ? song.mxmPath.replace("\\", "/").toLowerCase() : "";

            if ((!nov.isEmpty() && nov.contains("/" + targetFolder + "/")) ||
                (!adv.isEmpty() && adv.contains("/" + targetFolder + "/")) ||
                (!exh.isEmpty() && exh.contains("/" + targetFolder + "/")) ||
                (!mxm.isEmpty() && mxm.contains("/" + targetFolder + "/"))) {

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

        if (cameraTarget != null) {
            float safeDelta = Math.min(delta, 0.03f);

            songListTable.validate();
            tempPos1.set(0, cameraTarget.getHeight() / 2f);
            cameraTarget.localToAscendantCoordinates(songListTable, tempPos1);

            float distanceToTop = songListTable.getHeight() - tempPos1.y;
            float targetScroll = distanceToTop - (scrollPane.getHeight() / 2f);

            float currentScroll = scrollPane.getScrollY();
            float distance = targetScroll - currentScroll;

            if (Math.abs(distance) < 2f) {
                scrollPane.setScrollY(targetScroll);
                cameraTarget = null;
            } else {
                float newScroll = currentScroll + (distance * (safeDelta * 12f));
                scrollPane.setScrollY(newScroll);
            }
        }

        float tanAngle = (float) Math.tan(Math.toRadians(5f));
        if (songListTable != null) {
            applySlant(songListTable, tanAngle);
        }
    }

    private void applySlant(com.badlogic.gdx.scenes.scene2d.Group group, float tanAngle) {
        float baseLeftX = 40f;

        for (com.badlogic.gdx.scenes.scene2d.Actor child : group.getChildren()) {

            if ("slantHeader".equals(child.getName()) || "slantDiff".equals(child.getName())) {

                tempPos1.set(0, child.getHeight());
                child.localToAscendantCoordinates(SongListPanel.this, tempPos1);
                float distanceDown = SongListPanel.this.getHeight() - tempPos1.y;

                float absoluteX = baseLeftX + (distanceDown * tanAngle);

                if ("slantDiff".equals(child.getName())) absoluteX += 30f;
                if (child.getUserObject() instanceof Float) {
                    absoluteX += (Float) child.getUserObject();
                }

                tempPos2.set(absoluteX, 0);
                SongListPanel.this.localToStageCoordinates(tempPos2);
                child.getParent().stageToLocalCoordinates(tempPos2);

                child.setX(tempPos2.x);
            }

            if (child instanceof com.badlogic.gdx.scenes.scene2d.Group) {
                applySlant((com.badlogic.gdx.scenes.scene2d.Group) child, tanAngle);
            }
        }
    }

    public void scroll(float amountY) {
        if (scrollPane != null) {
            cameraTarget = null;
            float newScroll = scrollPane.getScrollY() + (amountY * 75f);
            scrollPane.setScrollY(newScroll);
        }
    }

    private Color getColorForLevel(int level) {
        if (level >= 1 && level <= 6) return Color.valueOf("#c1ff72");
        if (level >= 7 && level <= 12) return Color.valueOf("#599f00");
        if (level >= 13 && level <= 14) return Color.valueOf("#ff751f");
        if (level >= 15 && level <= 16) return Color.valueOf("#da142b");
        if (level == 17) return Color.valueOf("#003794");
        if (level == 18) return Color.valueOf("#120484");
        if (level == 19) return Color.valueOf("#2c0640");
        if (level >= 20) return Color.valueOf("#000000");

        return Color.DARK_GRAY;
    }

    private int[] calculateMapStatsFromTree(com.badlogic.gdx.utils.JsonValue root) {
        int tapCount = 0;
        int holdCount = 0;
        int totalLaserTicks = 0;

        try {
            com.badlogic.gdx.utils.JsonValue hitObjects = root.get("hitObjects");
            if (hitObjects != null) {
                for (com.badlogic.gdx.utils.JsonValue ho : hitObjects) {
                    String type = ho.getString("type", "TAP");
                    if (type.equals("TAP")) tapCount++;
                    else if (type.equals("HOLD")) holdCount++;
                }
            }

            com.badlogic.gdx.utils.JsonValue lasers = root.get("lasers");
            if (lasers != null) {
                totalLaserTicks += bakeJsonLaserArray(lasers.get("left"));
                totalLaserTicks += bakeJsonLaserArray(lasers.get("right"));
            }
        } catch (Exception e) {
            System.out.println("Failed to calculate map stats from tree.");
        }

        return new int[]{tapCount, holdCount, totalLaserTicks};
    }

    private int bakeJsonLaserArray(com.badlogic.gdx.utils.JsonValue sequences) {
        if (sequences == null) return 0;
        int ticks = 0;

        for (com.badlogic.gdx.utils.JsonValue seq : sequences) {
            com.badlogic.gdx.utils.JsonValue nodes = seq.get("nodes");
            if (nodes == null || nodes.size == 0) continue;

            com.badlogic.gdx.utils.FloatArray tickTimes = new com.badlogic.gdx.utils.FloatArray();
            tickTimes.add(nodes.get(0).getFloat("offset"));

            for (int i = 1; i < nodes.size; i++) {
                float prevOffset = nodes.get(i - 1).getFloat("offset");
                float currOffset = nodes.get(i).getFloat("offset");
                float duration = currOffset - prevOffset;

                if (duration <= 100.0f) {
                    if (!tickTimes.contains(currOffset)) tickTimes.add(currOffset);
                } else {
                    float tickTime = prevOffset + 100.0f;
                    while (tickTime < currOffset) {
                        if (!tickTimes.contains(tickTime)) tickTimes.add(tickTime);
                        tickTime += 100.0f;
                    }
                }
            }
            float lastOffset = nodes.get(nodes.size - 1).getFloat("offset");
            if (!tickTimes.contains(lastOffset)) tickTimes.add(lastOffset);
            ticks += tickTimes.size;
        }
        return ticks;
    }

    public void filterSongs(String query) {
        this.searchQuery = query.toLowerCase();
        refreshSongList(false);
    }
}
