package com.nodevoltex.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class TopSearchBar extends Table {

    public TopSearchBar(Skin skin, SongListPanel songListPanel) {
        this.background(skin.newDrawable("white", new Color(1f, 0.2f, 0.6f, 0.9f)));
        this.left().pad(10);

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        fieldStyle.fontColor = Color.WHITE;

        TextField searchField = new TextField("search: ...", fieldStyle);
        this.add(searchField).width(250).padRight(20);

        this.add(new Label("sorted by:", skin)).padRight(10);

        // --- NEW: Interactive Sort Buttons ---
        TextButton titleBtn = new TextButton("title", skin);
        TextButton artistBtn = new TextButton("artist", skin);
        TextButton mapperBtn = new TextButton("mapper", skin);

        titleBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { songListPanel.sortSongs("title"); }
        });
        artistBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { songListPanel.sortSongs("artist"); }
        });
        mapperBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { songListPanel.sortSongs("mapper"); }
        });

        this.add(titleBtn).padRight(5);
        this.add(artistBtn).padRight(5);
        this.add(mapperBtn);
    }
}
