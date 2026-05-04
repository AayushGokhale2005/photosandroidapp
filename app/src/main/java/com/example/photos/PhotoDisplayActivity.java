package com.example.photos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;
import com.example.photos.model.Tag;

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
        tagAdapter.setLongClickListener(this::showDeleteTagDialog);
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
        if (id == R.id.action_add_tag) {
            showAddTagDialog();
            return true;
        }
        if (id == R.id.action_move) {
            showMoveToAlbumDialog();
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

    private void showAddTagDialog() {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tag, null);
        Spinner spinner = dialogView.findViewById(R.id.spinnerTagType);
        EditText editValue = dialogView.findViewById(R.id.editTagValue);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"person", "location"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Add Tag")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String type  = (String) spinner.getSelectedItem();
                    String value = editValue.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Tag value cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Tag tag = new Tag(type, value);
                        boolean added = photo.addTag(tag);
                        if (!added) {
                            Toast.makeText(this, "Tag already exists on this photo", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        PhotoLibrary.getInstance().save(this);
                        tagAdapter.notifyDataSetChanged();
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, "Invalid tag type", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteTagDialog(int position) {
        Photo photo = getCurrentPhoto();
        if (photo == null || position >= photo.getTags().size()) return;
        Tag tag = photo.getTags().get(position);

        new AlertDialog.Builder(this)
                .setTitle("Delete Tag")
                .setMessage("Remove tag \"" + tag + "\"?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    photo.removeTag(tag);
                    PhotoLibrary.getInstance().save(this);
                    tagAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    protected void showMoveToAlbumDialog() {
        java.util.List<Album> allAlbums = PhotoLibrary.getInstance().getAlbums();
        java.util.List<String> otherNames = new java.util.ArrayList<>();
        for (Album a : allAlbums) {
            if (!a.getName().equalsIgnoreCase(albumName)) {
                otherNames.add(a.getName());
            }
        }
        if (otherNames.isEmpty()) {
            Toast.makeText(this, "No other albums available", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = otherNames.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Move to Album")
                .setItems(names, (dialog, which) -> {
                    movePhotoToAlbum(names[which]);
                })
                .show();
    }

    private void movePhotoToAlbum(String targetAlbumName) {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;
        Album source = PhotoLibrary.getInstance().getAlbum(albumName);
        Album target = PhotoLibrary.getInstance().getAlbum(targetAlbumName);
        if (source == null || target == null) return;
        target.addPhoto(photo);
        source.removePhoto(photo);
        PhotoLibrary.getInstance().save(this);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
