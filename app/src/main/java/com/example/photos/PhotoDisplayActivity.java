package com.example.photos;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;

public class PhotoDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_NAME  = "album_name";
    public static final String EXTRA_PHOTO_INDEX = "photo_index";

    protected String albumName;
    protected int photoIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_display);

        albumName  = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        photoIndex = getIntent().getIntExtra(EXTRA_PHOTO_INDEX, 0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            updateTitle();
        }
    }

    protected void updateTitle() {
        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        if (album == null || photoIndex >= album.getPhotos().size()) return;
        Photo photo = album.getPhotos().get(photoIndex);
        String uriStr = photo.getUriString();
        String segment = Uri.parse(uriStr).getLastPathSegment();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(segment != null ? segment : "Photo");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
