package com.example.photos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.PhotoLibrary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AlbumActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_NAME = "album_name";

    private String albumName;
    private PhotoGridAdapter adapter;

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
                position -> openPhotoDisplay(position)
        );
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddPhoto);
        fab.setOnClickListener(v -> { /* wired in Checkpoint 5 */ });
    }

    private void openPhotoDisplay(int position) {
        Intent intent = new Intent(this, PhotoDisplayActivity.class);
        intent.putExtra(PhotoDisplayActivity.EXTRA_ALBUM_NAME, albumName);
        intent.putExtra(PhotoDisplayActivity.EXTRA_PHOTO_INDEX, position);
        startActivity(intent);
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
