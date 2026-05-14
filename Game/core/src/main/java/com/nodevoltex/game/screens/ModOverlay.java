package com.nodevoltex.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.nodevoltex.game.managers.SettingsManager;

public class ModOverlay {
    private final Group root;
    private final Table mainContainer;
    private final Table clickShield; // <--- Extracted so we can animate it
    private boolean isOpen = false;

    public ModOverlay(Skin skin, Stage stage) {
        root = new Group();
        root.setSize(stage.getWidth(), stage.getHeight());
        root.setVisible(false);
        root.setTouchable(Touchable.disabled);

        // 1. The Screen-Dimming Shield
        clickShield = new Table();
        clickShield.setFillParent(true);
        // --- THE FIX: Added a 75% dark background to dim the screen! ---
        clickShield.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.75f)));
        clickShield.setTouchable(Touchable.enabled);
        clickShield.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { hide(); }
        });
        root.addActor(clickShield);

        // 2. The Sliding Container
        mainContainer = new Table();
        mainContainer.setWidth(stage.getWidth());

        // --- TOP HALF: The Mod Panel ---
        Table modPanel = new Table();
        modPanel.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.15f, 0.95f)));

        Table headerBox = new Table();
        headerBox.setBackground(skin.newDrawable("white", new Color(0.18f, 0.18f, 0.25f, 1f)));
        headerBox.pad(10);
        Label header = new Label("Automation", skin);
        header.setColor(Color.LIGHT_GRAY);
        headerBox.add(header).left().padLeft(15);

        modPanel.add(headerBox).expandX().fillX().row();

        Table btnRow = new Table();
        btnRow.pad(20);
        TextButton autoBtn = new TextButton("Autoplay", skin);
        TextButton noLaserBtn = new TextButton("No Laser", skin);

        Runnable updateBtnColors = () -> {
            // THE FIX: Color the text label directly so it glows bright, regardless of the button texture!
            if (SettingsManager.getModAutoPlay()) {
                autoBtn.getLabel().setColor(Color.CYAN);
            } else {
                autoBtn.getLabel().setColor(Color.DARK_GRAY);
            }

            if (SettingsManager.getModNoLaser()) {
                noLaserBtn.getLabel().setColor(Color.CYAN);
            } else {
                noLaserBtn.getLabel().setColor(Color.DARK_GRAY);
            }
        };
        updateBtnColors.run();

        autoBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean newState = !SettingsManager.getModAutoPlay();
                SettingsManager.setModAutoPlay(newState);
                if (newState) SettingsManager.setModNoLaser(false);
                updateBtnColors.run();
            }
        });

        noLaserBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean newState = !SettingsManager.getModNoLaser();
                SettingsManager.setModNoLaser(newState);
                if (newState) SettingsManager.setModAutoPlay(false);
                updateBtnColors.run();
            }
        });

        btnRow.add(autoBtn).width(160).height(50).padRight(20);
        btnRow.add(noLaserBtn).width(160).height(50);
        modPanel.add(btnRow).left().expandX().row();

        // --- BOTTOM HALF: The Exit Strip ---
        Table exitArea = new Table();
        exitArea.setBackground(skin.newDrawable("white", new Color(0.05f, 0.05f, 0.08f, 0.98f)));
        Label exitLbl = new Label("Exit", skin);
        exitLbl.setColor(Color.SCARLET);
        exitArea.add(exitLbl).center();

        exitArea.setTouchable(Touchable.enabled);
        exitArea.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { hide(); }
        });

        mainContainer.add(modPanel).expandX().fillX().row();
        mainContainer.add(exitArea).expandX().fillX().height(80);

        mainContainer.pack();
        mainContainer.setWidth(stage.getWidth());
        mainContainer.setPosition(0, -mainContainer.getHeight());

        root.addActor(mainContainer);
        stage.addActor(root);
    }

    public void show() {
        if (isOpen) return;
        isOpen = true;
        root.setVisible(true);
        root.setTouchable(Touchable.enabled);

        // --- THE FIX: Fade in the dark background shield ---
        clickShield.getColor().a = 0f;
        clickShield.addAction(Actions.fadeIn(0.4f, Interpolation.pow2Out));

        mainContainer.clearActions();
        mainContainer.addAction(Actions.moveTo(0, 0, 0.5f, Interpolation.pow3Out));
    }

    public void hide() {
        if (!isOpen) return;
        isOpen = false;
        root.setTouchable(Touchable.disabled);

        // --- THE FIX: Fade out the dark background shield ---
        clickShield.addAction(Actions.fadeOut(0.4f, Interpolation.pow2In));

        mainContainer.clearActions();
        mainContainer.addAction(Actions.sequence(
            Actions.moveTo(0, -mainContainer.getHeight(), 0.5f, Interpolation.pow3In),
            Actions.run(() -> root.setVisible(false))
        ));
    }

    public boolean isOpen() { return isOpen; }

    public void resize(int width, int height) {
        root.setSize(width, height);
        mainContainer.setWidth(width);
        mainContainer.pack();
        mainContainer.setWidth(width);
        if (!isOpen) mainContainer.setY(-mainContainer.getHeight());
    }
}
