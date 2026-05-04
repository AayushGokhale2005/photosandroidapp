package com.example.photos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_NAME  = "album_name";
    public static final String EXTRA_PHOTO_INDEX = "photo_index";

    protected String albumName;
    protected int photoIndex;

    private ImageView imagePhoto;
    private TagListAdapter tagAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        }

        imagePhoto = findViewById(R.id.imagePhoto);

        RecyclerView recyclerTags = findViewById(R.id.recyclerTags);
        recyclerTags.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tagAdapter = new TagListAdapter(getCurrentPhoto() != null
                ? getCurrentPhoto().getTags()
                : new java.util.ArrayList<>(), null);
        recyclerTags.setAdapter(tagAdapter);

        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);
        btnPrev.setOnClickListener(v -> navigate(-1));
        btnNext.setOnClickListener(v -> navigate(1));

        loadCurrentPhoto();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected Photo getCurrentPhoto() {
        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        if (album == null || photoIndex < 0 || photoIndex >= album.getPhotos().size()) return null;
        return album.getPhotos().get(photoIndex);
    }

    protected void loadCurrentPhoto() {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;

        updateTitle();
        tagAdapter.setTags(photo.getTags());
        tagAdapter.notifyDataSetChanged();

        imagePhoto.setImageBitmap(null);
        String uriStr = photo.getUriString();
        executor.execute(() -> {
            Bitmap bmp = loadFullBitmap(uriStr);
            mainHandler.post(() -> imagePhoto.setImageBitmap(bmp));
        });

        updateNavButtons();
    }

    private void navigate(int delta) {
        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        if (album == null || album.getPhotos().isEmpty()) return;
        int size = album.getPhotos().size();
        photoIndex = (photoIndex + delta + size) % size;
        loadCurrentPhoto();
    }

    private void updateNavButtons() {
        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        int size = album != null ? album.getPhotos().size() : 0;
        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);
        btnPrev.setEnabled(size > 1);
        btnNext.setEnabled(size > 1);
    }

    protected void updateTitle() {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;
        String segment = Uri.parse(photo.getUriString()).getLastPathSegment();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(segment != null ? segment : "Photo");
        }
    }

    private Bitmap loadFullBitmap(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, bounds);
            }
            int maxDim = 1920;
            int inSample = 1;
            while (bounds.outWidth / inSample > maxDim || bounds.outHeight / inSample > maxDim) {
                inSample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = inSample;
            try (InputStream is2 = getContentResolver().openInputStream(uri)) {
                if (is2 == null) return null;
                return BitmapFactory.decodeStream(is2, null, opts);
            }
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
