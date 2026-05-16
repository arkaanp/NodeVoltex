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

    // --- Playfield Customization ---
    public static float getPlayfieldHitPosY() { return getPrefs().getFloat("playfieldHitPosY", 100f); }
    public static float getPlayfieldWidth() { return getPrefs().getFloat("playfieldWidth", 300f); } // Default is 75f * 4

    // --- Mod Settings ---
    public static boolean getModAutoPlay() { return getPrefs().getBoolean("modAutoPlay", false); }
    public static void setModAutoPlay(boolean val) { getPrefs().putBoolean("modAutoPlay", val); getPrefs().flush(); }

    public static boolean getModNoLaser() { return getPrefs().getBoolean("modNoLaser", false); }
    public static void setModNoLaser(boolean val) { getPrefs().putBoolean("modNoLaser", val); getPrefs().flush(); }

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
            // --- Alternate Defaults Applied ---
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

    public static void savePlayfield(float hitPosY, float width) {
        getPrefs().putFloat("playfieldHitPosY", hitPosY);
        getPrefs().putFloat("playfieldWidth", width);
        getPrefs().flush();
    }

    public static void saveKey(String buttonCode, boolean isPrimary, String keyName) {
        getPrefs().putString((isPrimary ? "pri_" : "alt_") + buttonCode, keyName);
        getPrefs().flush();
    }

    // --- UI Customization ---
    public static float getBackgroundBrightness() { return getPrefs().getFloat("bgBrightness", 0.6f); }
    public static float getJudgmentComboTopOffset() { return getPrefs().getFloat("judgOffset", 200f); }

    // --- UR Bar Setting (Defaults to TRUE) ---
    public static boolean isShowURBar() { return getPrefs().getBoolean("showURBar", true); }
    public static void setShowURBar(boolean val) { getPrefs().putBoolean("showURBar", val); getPrefs().flush(); }

    public static void saveUI(float brightness, float offset) {
        getPrefs().putFloat("bgBrightness", brightness);
        getPrefs().putFloat("judgOffset", offset);
        getPrefs().flush();
    }

    // --- Retry Settings ---
    public static float getRetryHoldTime() { return getPrefs().getFloat("retryHoldTime", 1.0f); } // Default 1 second
    public static void setRetryHoldTime(float val) { getPrefs().putFloat("retryHoldTime", val); getPrefs().flush(); }

    public static String getRetryKeyString() { return getPrefs().getString("retryKey", "`"); }
    public static void setRetryKeyString(String keyName) { getPrefs().putString("retryKey", keyName); getPrefs().flush(); }

    public static int getRetryKey() {
        String keyName = getRetryKeyString();
        if (keyName.isEmpty() || keyName.equals("_")) return Input.Keys.UNKNOWN;
        if (keyName.equals("`")) return Input.Keys.GRAVE;
        return Input.Keys.valueOf(keyName);
    }

    // --- Display Settings (0 = Windowed, 1 = Fullscreen, 2 = Borderless) ---
    public static int getDisplayMode() { return getPrefs().getInteger("displayMode", 0); } // Default to Borderless
    public static void setDisplayMode(int val) { getPrefs().putInteger("displayMode", val); getPrefs().flush(); }
}
