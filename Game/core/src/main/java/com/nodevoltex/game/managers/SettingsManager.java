package com.nodevoltex.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class SettingsManager {
    private static Preferences prefs;

    private static Preferences getPrefs() {
        if (prefs == null) prefs = Gdx.app.getPreferences("NodeVoltexSettings");
        return prefs;
    }

    // --- DEFAULTS APPLIED HERE ---
    public static float getScrollSpeed() { return getPrefs().getFloat("scrollSpeed", 1.2f); }
    public static float getGlobalOffset() { return getPrefs().getFloat("globalOffset", 0f); }

    // Master Volume defaulted to 30%
    public static float getMasterVolume() { return getPrefs().getFloat("masterVolume", 0.3f); }
    public static float getMusicVolume() { return getPrefs().getFloat("musicVolume", 0.8f); }
    public static float getEffectVolume() { return getPrefs().getFloat("effectVolume", 1.0f); }

    // --- STRING GETTER FOR THE UI TO READ ---
    public static String getKeyString(String buttonCode, boolean isPrimary) {
        String defaultKey = "";
        if (isPrimary) {
            switch(buttonCode) {
                case "LL": defaultKey = "2"; break;
                case "LR": defaultKey = "3"; break;
                case "RL": defaultKey = "9"; break;
                case "RR": defaultKey = "0"; break;
                case "BT1": defaultKey = "W"; break;
                case "BT2": defaultKey = "E"; break;
                case "BT3": defaultKey = "I"; break;
                case "BT4": defaultKey = "O"; break;
                case "FXL": defaultKey = "X"; break;
                case "FXR": defaultKey = "M"; break;
            }
        } else {
            // --- THE FIX: Alternate Defaults Applied! ---
            switch(buttonCode) {
                case "LL": defaultKey = "7"; break;
                case "LR": defaultKey = "8"; break;
                case "RL": defaultKey = "4"; break;
                case "RR": defaultKey = "5"; break;
                case "BT1": defaultKey = "Y"; break;
                case "BT2": defaultKey = "U"; break;
                case "BT3": defaultKey = "R"; break;
                case "BT4": defaultKey = "T"; break;
                case "FXL": defaultKey = "N"; break;
                case "FXR": defaultKey = "C"; break;
            }
        }
        return getPrefs().getString((isPrimary ? "pri_" : "alt_") + buttonCode, defaultKey);
    }

    // --- PHYSICAL KEY CONVERTER FOR GAMEPLAY ---
    public static int getKey(String buttonCode, boolean isPrimary) {
        String keyName = getKeyString(buttonCode, isPrimary);
        if (keyName.isEmpty() || keyName.equals("_")) return Input.Keys.UNKNOWN;
        if (keyName.equals("SPC")) return Input.Keys.SPACE;
        return Input.Keys.valueOf(keyName);
    }

    public static void saveVolumes(float master, float music, float effect) {
        getPrefs().putFloat("masterVolume", master);
        getPrefs().putFloat("musicVolume", music);
        getPrefs().putFloat("effectVolume", effect);
        getPrefs().flush();
    }

    public static void saveGameplay(float speed, float offset) {
        getPrefs().putFloat("scrollSpeed", speed);
        getPrefs().putFloat("globalOffset", offset);
        getPrefs().flush();
    }

    public static void saveKey(String buttonCode, boolean isPrimary, String keyName) {
        getPrefs().putString((isPrimary ? "pri_" : "alt_") + buttonCode, keyName);
        getPrefs().flush();
    }
}
