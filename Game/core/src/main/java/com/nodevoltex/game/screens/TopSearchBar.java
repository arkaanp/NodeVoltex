package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;

public class TopSearchBar extends Table {

    private final ShapeRenderer shapeRenderer;
    private final Skin skin;
    private boolean isSearchFocused = false;
    // Read from the global memory so the correct underline appears
    private String activeSort = SongListPanel.GLOBAL_LAST_SORT_MODE;

    private TextField searchField;

    public TopSearchBar(Skin skin, final SongListPanel songListPanel) {
        this.shapeRenderer = new ShapeRenderer();
        this.skin = skin;
        this.background((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
        this.pad(0);

        // --- ROW 1: SEARCH FIELD ---
        Table searchRow = new Table();
        searchRow.left();

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.background = null;

        searchField = new TextField("search: ...", fieldStyle);

        searchField.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isSearchFocused && searchField.getText().trim().equals("search: ...")) {
                    searchField.setText("");
                    isSearchFocused = true;
                }
            }
        });

        searchField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                songListPanel.filterSongs(searchField.getText().trim());
            }
        });

        // --- Automatic Placeholder Restoration ---
        searchField.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    if (searchField.getText().trim().isEmpty()) {
                        isSearchFocused = false;
                        searchField.setText("search: ...");
                    }
                }
            }
        });

        searchRow.add(searchField).width(350).padLeft(30);
        this.add(searchRow).expandX().fillX().height(40).row();

        // --- ROW 2: SORT BUTTONS ---
        Table sortTable = new Table();
        sortTable.left();

        Label sortLbl = new Label("sorted by:", skin);
        sortLbl.setColor(Color.valueOf("#222222"));
        sortLbl.setFontScale(0.85f);

        Table titleTab = createSortTab("title", songListPanel);
        Table artistTab = createSortTab("artist", songListPanel);
        Table mapperTab = createSortTab("mapper", songListPanel);

        // --- New Level Tab ---
        Table levelTab = createSortTab("level", songListPanel);

        sortTable.add(sortLbl).padLeft(25).padRight(15);
        sortTable.add(titleTab).padRight(15);
        sortTable.add(artistTab).padRight(15);
        sortTable.add(mapperTab).padRight(15);

        // --- Add Level Tab to the layout ---
        sortTable.add(levelTab);

        this.add(sortTable).expandX().fillX().height(35);
    }

    // --- Smart Unfocus Scanner ---
    @Override
    public void act(float delta) {
        super.act(delta);

        // Scan for screen taps/clicks
        if (Gdx.input.justTouched() && getStage() != null) {
            com.badlogic.gdx.math.Vector2 pos = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
            getStage().screenToStageCoordinates(pos);
            this.stageToLocalCoordinates(pos);

            float topHeight = 40f;
            float innerY = getHeight() - topHeight;

            // If the click lands anywhere OUTSIDE the top 40 pixels (the inner trapezoid bounds)
            if (pos.y < innerY || pos.x < 0 || pos.y > getHeight() || pos.x > getWidth()) {
                // If the search bar was actively typing, kill the focus!
                if (getStage().getKeyboardFocus() == searchField) {
                    getStage().setKeyboardFocus(null);
                }
            }
        }
    }

    private Table createSortTab(final String text, final SongListPanel songListPanel) {
        final Table tab = new Table();
        tab.setTouchable(Touchable.enabled);

        final Label label = new Label(text, skin);
        label.setFontScale(0.85f);

        final Image underline = new Image(skin.newDrawable("white", Color.BLACK));

        tab.add(label).padBottom(2).row();
        tab.add(underline).growX().height(2);

        final ClickListener listener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activeSort = text;
                songListPanel.sortSongs(text);
            }
        };
        tab.addListener(listener);

        tab.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (activeSort.equals(text)) {
                    underline.getColor().a = 1.0f;
                    label.setColor(Color.BLACK);
                } else if (listener.isOver()) {
                    underline.getColor().a = 0.4f;
                    label.setColor(Color.valueOf("#222222"));
                } else {
                    underline.getColor().a = 0.0f;
                    label.setColor(Color.valueOf("#444444"));
                }
                return false;
            }
        });

        return tab;
    }

    // --- Mathematically Perfect Rounded Trapezoid Helper ---
    private void drawRoundedTrapezoid(ShapeRenderer sr, float x, float y, float w, float h, float tanAngle, Color color, float radius) {
        sr.setColor(color);
        float slantTotal = h * tanAngle;

        // Calculate the exact center point so the arc seamlessly matches the 5-degree slant
        float arcCenterX = x + slantTotal + radius - (radius * tanAngle);
        float arcCenterY = y + radius;

        // 1. Draw the smooth bottom-left corner
        sr.arc(arcCenterX, arcCenterY, radius, 180f, 95f, 25);

        // 2. Draw the flat bottom connecting the arc to the right edge
        sr.rect(arcCenterX, y, (x + w) - arcCenterX, radius);

        // 3. Draw the main geometric body using two triangles
        float slantAtRadiusY = slantTotal - (radius * tanAngle);
        sr.triangle(x, y + h, x + slantAtRadiusY, arcCenterY, x + w, y + h);
        sr.triangle(x + slantAtRadiusY, arcCenterY, x + w, arcCenterY, x + w, y + h);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float tanAngle = (float) Math.tan(Math.toRadians(5f));
        float topHeight = 40f, splitY = y + h - topHeight;

        Color lightPink = new Color(1f, 0.4f, 0.75f, 0.95f * parentAlpha);
        Color darkPink = new Color(1f, 0.15f, 0.65f, 0.95f * parentAlpha);

        // --- Draw both background bands using the rounded geometry ---
        // 1. MAIN BACKGROUND (Light Pink with 12px radius)
        drawRoundedTrapezoid(shapeRenderer, x, y, w, h, tanAngle, lightPink, 12f);

        // 2. INNER SEARCH BAND (Dark Pink with 8px radius)
        float shiftRight = 10f;
        drawRoundedTrapezoid(shapeRenderer, x + shiftRight, splitY, w - shiftRight, topHeight, tanAngle, darkPink, 8f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        super.draw(batch, parentAlpha);
    }
}
