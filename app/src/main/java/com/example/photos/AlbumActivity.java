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

        RecyclerView recycler = findViewById(R.id.recyclerPhotos);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));

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
