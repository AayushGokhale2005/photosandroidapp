package com.example.photos;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager sm;
    private View colorSwatch;
    private MaterialButtonToggleGroup toggleAlbumCols, togglePhotoCols;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sm = SettingsManager.get(this);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        colorSwatch = findViewById(R.id.colorSwatch);
        toggleAlbumCols = findViewById(R.id.toggleAlbumCols);
        togglePhotoCols = findViewById(R.id.togglePhotoCols);

        // Apply accent to toolbar, FAB, headers, toggle buttons
        applyAccentToPage();

        SwitchMaterial switchDark = findViewById(R.id.switchDarkMode);
        switchDark.setChecked(sm.isDarkMode());
        switchDark.setOnCheckedChangeListener((btn, checked) -> {
            sm.setDarkMode(checked);
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });

        updateSwatch(sm.getAccentColor());
        findViewById(R.id.rowAccentColor).setOnClickListener(v -> showColorPicker());

        selectAlbumBtn(sm.getAlbumCols());
        selectPhotoBtn(sm.getPhotoCols());

        toggleAlbumCols.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnAlbum2) sm.setAlbumCols(2);
            else if (checkedId == R.id.btnAlbum3) sm.setAlbumCols(3);
            else sm.setAlbumCols(4);
        });

        togglePhotoCols.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnPhoto2) sm.setPhotoCols(2);
            else if (checkedId == R.id.btnPhoto3) sm.setPhotoCols(3);
            else sm.setPhotoCols(4);
        });
    }

    /** Applies the current accent color to every tintable widget on this screen. */
    private void applyAccentToPage() {
        int accent = sm.getAccentColor();
        ColorStateList accentCsl = ColorStateList.valueOf(accent);

        // Toolbar + status bar via ThemeHelper
        ThemeHelper.applyAccent(this);

        // Section header labels
        TextView tvAppearance = findViewById(R.id.tvHeaderAppearance);
        TextView tvGrid       = findViewById(R.id.tvHeaderGrid);
        if (tvAppearance != null) tvAppearance.setTextColor(accent);
        if (tvGrid != null)       tvGrid.setTextColor(accent);

        // Toggle button group strokes and checked state
        applyToggleTint(toggleAlbumCols, accent);
        applyToggleTint(togglePhotoCols, accent);
    }

    private void applyToggleTint(MaterialButtonToggleGroup group, int accent) {
        if (group == null) return;
        ColorStateList csl = ColorStateList.valueOf(accent);
        for (int i = 0; i < group.getChildCount(); i++) {
            com.google.android.material.button.MaterialButton btn =
                    (com.google.android.material.button.MaterialButton) group.getChildAt(i);
            btn.setStrokeColor(csl);
            btn.setRippleColor(csl);
            // Color the text: accent when checked, default otherwise
            int[][] states = {{android.R.attr.state_checked}, {}};
            int[] colors   = {0xFFFFFFFF, accent};
            btn.setTextColor(new ColorStateList(states, colors));
            // Background: filled accent when checked, transparent otherwise
            int[][] bgStates = {{android.R.attr.state_checked}, {}};
            int[] bgColors   = {accent, 0x00000000};
            btn.setBackgroundTintList(new ColorStateList(bgStates, bgColors));
        }
    }

    private void selectAlbumBtn(int cols) {
        int id = cols == 2 ? R.id.btnAlbum2 : cols == 3 ? R.id.btnAlbum3 : R.id.btnAlbum4;
        toggleAlbumCols.check(id);
    }

    private void selectPhotoBtn(int cols) {
        int id = cols == 2 ? R.id.btnPhoto2 : cols == 3 ? R.id.btnPhoto3 : R.id.btnPhoto4;
        togglePhotoCols.check(id);
    }

    private void updateSwatch(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke((int)(2 * getResources().getDisplayMetrics().density), 0x33000000);
        colorSwatch.setBackground(gd);
    }

    private void showColorPicker() {
        Dialog dialog = new Dialog(this, R.style.Dialog_Transparent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_color_picker);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ColorWheelView wheel = dialog.findViewById(R.id.colorWheel);
        View preview = dialog.findViewById(R.id.colorPreview);
        wheel.setColor(sm.getAccentColor());
        updateSwatchView(preview, sm.getAccentColor());

        wheel.setOnColorChangedListener(c -> updateSwatchView(preview, c));

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnApply).setOnClickListener(v -> {
            int chosen = wheel.getColor();
            sm.setAccentColor(chosen);
            updateSwatch(chosen);
            // Re-tint everything on this page immediately
            applyAccentToPage();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSwatchView(View v, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke((int)(2 * getResources().getDisplayMetrics().density), 0x33000000);
        v.setBackground(gd);
    }
}
