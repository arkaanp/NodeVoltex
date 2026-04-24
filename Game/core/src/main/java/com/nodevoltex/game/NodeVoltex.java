package com.nodevoltex.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.nodevoltex.game.screens.PlayScreen;

public class NodeVoltex extends Game {
    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Boot directly into the core engine simulation
        this.setScreen(new PlayScreen(this));
    }

    @Override
    public void render() {
        // Mmandatory to render the active screen
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (screen != null) screen.dispose();
    }
}
