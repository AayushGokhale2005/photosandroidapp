package com.example.photos;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.Window;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ThemeHelper {

    private ThemeHelper() {}

    /**
     * Call this in every Activity.onCreate() after setContentView() to apply
     * the user's chosen accent color to the toolbar, status bar, and FAB.
     */
    public static void applyAccent(Activity activity) {
        SettingsManager sm = SettingsManager.get(activity);
        int accent = sm.getAccentColor();
        int accentDark = sm.getAccentDark();

        // Status bar
        Window window = activity.getWindow();
        window.setStatusBarColor(accentDark);

        // Toolbar
        Toolbar tb = activity.findViewById(R.id.toolbar);
        if (tb != null) tb.setBackgroundColor(accent);

        // FAB (if present)
        FloatingActionButton fab1 = activity.findViewById(R.id.fabNewAlbum);
        if (fab1 != null) fab1.setBackgroundTintList(ColorStateList.valueOf(accent));
        FloatingActionButton fab2 = activity.findViewById(R.id.fabAddPhoto);
        if (fab2 != null) fab2.setBackgroundTintList(ColorStateList.valueOf(accent));

        // AppBarLayout (if used)
        View abl = activity.findViewById(R.id.appBarLayout);
        if (abl != null) abl.setBackgroundColor(accent);
    }
}
