package com.nodevoltex.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class TopSearchBar extends Table {

    private final ShapeRenderer shapeRenderer;
    private final Skin skin;
    private boolean isSearchFocused = false;
    private String activeSort = "title"; // Tracks which tab is selected

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

        final TextField searchField = new TextField("search: ...", fieldStyle);
        searchField.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!isSearchFocused) {
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

        searchRow.add(searchField).width(350).padLeft(30);
        this.add(searchRow).expandX().fillX().height(40).row();

        // --- ROW 2: SORT BUTTONS (Custom Underline Tabs) ---
        Table sortTable = new Table();
        sortTable.left();

        Label sortLbl = new Label("sorted by:", skin);
        sortLbl.setColor(Color.valueOf("#222222"));
        sortLbl.setFontScale(0.85f); // Matches the tabs

        Table titleTab = createSortTab("title", songListPanel);
        Table artistTab = createSortTab("artist", songListPanel);
        Table mapperTab = createSortTab("mapper", songListPanel);

        sortTable.add(sortLbl).padLeft(25).padRight(15);
        sortTable.add(titleTab).padRight(15);
        sortTable.add(artistTab).padRight(15);
        sortTable.add(mapperTab);

        this.add(sortTable).expandX().fillX().height(35);
    }

    // --- Custom Animated Tab Builder ---
    private Table createSortTab(final String text, final SongListPanel songListPanel) {
        final Table tab = new Table();
        tab.setTouchable(Touchable.enabled);

        final Label label = new Label(text, skin);
        label.setFontScale(0.85f); // Matched size

        // The underline image (Black to match the text on the pink background)
        final Image underline = new Image(skin.newDrawable("white", Color.BLACK));

        tab.add(label).padBottom(2).row();
        tab.add(underline).growX().height(2); // 2 pixels thick, stretches to text width

        final ClickListener listener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activeSort = text;
                songListPanel.sortSongs(text);
            }
        };
        tab.addListener(listener);

        // Permanent logic loop to handle visual states seamlessly
        tab.addAction(new com.badlogic.gdx.scenes.scene2d.Action() {
            @Override
            public boolean act(float delta) {
                if (activeSort.equals(text)) {
                    underline.getColor().a = 1.0f;          // Fully visible
                    label.setColor(Color.BLACK);            // Active color
                } else if (listener.isOver()) {
                    underline.getColor().a = 0.4f;          // Low opacity hover
                    label.setColor(Color.valueOf("#222222")); // Slightly faded text
                } else {
                    underline.getColor().a = 0.0f;          // Invisible
                    label.setColor(Color.valueOf("#444444")); // Inactive color
                }
                return false; // Loop forever
            }
        });

        return tab;
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

        float slantTotal = h * tanAngle;
        shapeRenderer.setColor(lightPink);
        shapeRenderer.triangle(x, y + h, x + slantTotal, y, x + w, y);
        shapeRenderer.triangle(x, y + h, x + w, y, x + w, y + h);

        float shiftRight = 10f, slantInner = topHeight * tanAngle;
        float innerTopX = x + shiftRight, innerBotX = innerTopX + slantInner;

        shapeRenderer.setColor(darkPink);
        shapeRenderer.triangle(innerTopX, y + h, innerBotX, splitY, x + w, splitY);
        shapeRenderer.triangle(innerTopX, y + h, x + w, splitY, x + w, y + h);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();

        super.draw(batch, parentAlpha);
    }
}
