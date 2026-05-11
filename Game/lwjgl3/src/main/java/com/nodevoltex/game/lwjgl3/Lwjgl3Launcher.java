package com.nodevoltex.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.nodevoltex.game.NodeVoltex;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new NodeVoltex(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Node Voltex");
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        // --- NEW: Windowed Fullscreen Settings ---

        // 1. Maximize the window automatically on startup.
        // This natively respects the Windows taskbar bounds.
        configuration.setMaximized(true);
        //configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode(Lwjgl3ApplicationConfiguration.getPrimaryMonitor()));

        // 2. (Optional) Remove the top title bar and window borders.
        // If you want the game to look completely borderless while still
        // sitting above the taskbar, keep this set to false.
        // If you want to keep the standard Windows "X" close button at the top right,
        // you can delete this line.
        //configuration.setDecorated(false);

        // Frame rate settings
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        configuration.setWindowedMode(1280, 720); // Fallback size if it gets un-maximized
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}
