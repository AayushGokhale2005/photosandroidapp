package com.example.photos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AlbumActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_NAME = "album_name";

    private String albumName;
    private PhotoGridAdapter adapter;

    private final ActivityResultLauncher<String[]> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                // Take persistable read permission so URI survives reboots
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                addPhotoToAlbum(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(albumName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ThemeHelper.applyAccent(this);

        // Tint the whole screen: resolve the theme's surface color then
        // overlay just 10% of the accent so it matches the home screen tone.
        int accent = SettingsManager.get(this).getAccentColor();
        int bgColor = blendAccentOnSurface(accent, 0.10f);
        findViewById(R.id.albumRoot).setBackgroundColor(bgColor);

        int cols = SettingsManager.get(this).getPhotoCols();
        RecyclerView recycler = findViewById(R.id.recyclerPhotos);
        recycler.setLayoutManager(new GridLayoutManager(this, cols));
        recycler.addItemDecoration(new GridSpacingDecoration(2));
        recycler.setHasFixedSize(true);

        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        adapter = new PhotoGridAdapter(
                album != null ? album.getPhotos() : new java.util.ArrayList<>(),
                this,
                this::openPhotoDisplay
        );
        adapter.setLongClickListener(this::showPhotoOptions);
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddPhoto);
        fab.setOnClickListener(v -> pickPhotoLauncher.launch(new String[]{"image/*"}));
    }

    private void addPhotoToAlbum(Uri uri) {
        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        if (album == null) return;
        Photo photo = new Photo(uri.toString());
        boolean added = album.addPhoto(photo);
        if (!added) {
            android.widget.Toast.makeText(this, "Photo already in album", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        PhotoLibrary.getInstance().save(this);
        adapter.notifyDataSetChanged();
    }

    private void openPhotoDisplay(int position) {
        Intent intent = new Intent(this, PhotoDisplayActivity.class);
        intent.putExtra(PhotoDisplayActivity.EXTRA_ALBUM_NAME, albumName);
        intent.putExtra(PhotoDisplayActivity.EXTRA_PHOTO_INDEX, position);
        startActivity(intent);
    }

    private void showPhotoOptions(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Photo options")
                .setItems(new String[]{"View", "Remove"}, (dialog, which) -> {
                    if (which == 0) {
                        openPhotoDisplay(position);
                    } else {
                        confirmRemovePhoto(position);
                    }
                })
                .show();
    }

    private void confirmRemovePhoto(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Photo")
                .setMessage("Remove this photo from the album?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    Album album = PhotoLibrary.getInstance().getAlbum(albumName);
                    if (album == null) return;
                    album.getPhotos().remove(position);
                    PhotoLibrary.getInstance().save(this);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Resolves the theme's colorSurface (which auto-switches for dark/light mode)
     * and blends {@code mix} fraction of the accent color on top.
     * mix=0.10 → 10% accent, 90% surface — subtle tint that matches the home screen.
     */
    private int blendAccentOnSurface(int accent, float mix) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        int surface = tv.data;
        int r = (int)(android.graphics.Color.red(surface)   * (1f - mix) + android.graphics.Color.red(accent)   * mix);
        int g = (int)(android.graphics.Color.green(surface) * (1f - mix) + android.graphics.Color.green(accent) * mix);
        int b = (int)(android.graphics.Color.blue(surface)  * (1f - mix) + android.graphics.Color.blue(accent)  * mix);
        return android.graphics.Color.rgb(r, g, b);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }
}
