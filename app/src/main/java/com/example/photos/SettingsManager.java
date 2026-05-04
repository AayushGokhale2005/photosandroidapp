package com.example.photos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class SettingsManager {

    private static final String PREFS       = "photos_settings";
    private static final String KEY_DARK    = "dark_mode";
    private static final String KEY_ALBUM_COLS  = "album_cols";
    private static final String KEY_PHOTO_COLS  = "photo_cols";
    private static final String KEY_ACCENT  = "accent_color";

    private static SettingsManager instance;
    private final SharedPreferences prefs;

    private SettingsManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static SettingsManager get(Context ctx) {
        if (instance == null) instance = new SettingsManager(ctx);
        return instance;
    }

    // ---- Dark mode ----
    public boolean isDarkMode()             { return prefs.getBoolean(KEY_DARK, false); }
    public void    setDarkMode(boolean on)  { prefs.edit().putBoolean(KEY_DARK, on).apply(); }

    // ---- Grid columns ----
    public int  getAlbumCols()       { return prefs.getInt(KEY_ALBUM_COLS, 2); }
    public void setAlbumCols(int n)  { prefs.edit().putInt(KEY_ALBUM_COLS, n).apply(); }

    public int  getPhotoCols()       { return prefs.getInt(KEY_PHOTO_COLS, 3); }
    public void setPhotoCols(int n)  { prefs.edit().putInt(KEY_PHOTO_COLS, n).apply(); }

    // ---- Accent color ----
    public int  getAccentColor()        { return prefs.getInt(KEY_ACCENT, 0xFF6200EE); }
    public void setAccentColor(int c)   { prefs.edit().putInt(KEY_ACCENT, c).apply(); }

    /** Darker shade used for status bar / pressed states */
    public int getAccentDark() {
        float[] hsv = new float[3];
        Color.colorToHSV(getAccentColor(), hsv);
        hsv[2] = Math.max(0f, hsv[2] - 0.2f);
        return Color.HSVToColor(hsv);
    }
}
