package com.example.photos;

import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;
import com.example.photos.model.Tag;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_NAME  = "album_name";
    public static final String EXTRA_PHOTO_INDEX = "photo_index";

    protected String albumName;
    protected int photoIndex;

    private ImageView imagePhoto;
    private ChipGroup chipGroupTags;
    private TextView tvNoTags;
    private TextView tvPhotoDate;
    private TextView tvPhotoTitle;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMMM d, yyyy  h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_display);

        albumName  = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        photoIndex = getIntent().getIntExtra(EXTRA_PHOTO_INDEX, 0);

        imagePhoto    = findViewById(R.id.imageViewPhoto);
        chipGroupTags = findViewById(R.id.chipGroupTags);
        tvNoTags      = findViewById(R.id.tvNoTags);
        tvPhotoDate   = findViewById(R.id.tvPhotoDate);
        tvPhotoTitle  = findViewById(R.id.tvPhotoTitle);

        // Top bar navigation
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMoveAlbum).setOnClickListener(v -> showMoveToAlbumDialog());

        // Bottom card actions
        findViewById(R.id.btnPrev).setOnClickListener(v -> navigate(-1));
        findViewById(R.id.btnNext).setOnClickListener(v -> navigate(1));
        findViewById(R.id.btnAddTag).setOnClickListener(v -> showAddTagDialog());

        // Apply accent color to Add Tag button stroke; remove default vertical insets
        int accent = SettingsManager.get(this).getAccentColor();
        com.google.android.material.button.MaterialButton btnAdd = findViewById(R.id.btnAddTag);
        btnAdd.setStrokeColor(ColorStateList.valueOf(accent));
        btnAdd.setTextColor(accent);
        btnAdd.setInsetTop(0);
        btnAdd.setInsetBottom(0);

        loadCurrentPhoto();
    }

    // -------------------------------------------------------------------------
    // Core display
    // -------------------------------------------------------------------------

    protected Photo getCurrentPhoto() {
        Album album = PhotoLibrary.getInstance().getAlbum(albumName);
        if (album == null || photoIndex < 0 || photoIndex >= album.getPhotos().size()) return null;
        return album.getPhotos().get(photoIndex);
    }

    protected void loadCurrentPhoto() {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;

        updateTitle();
        updateDate(photo);
        rebuildChips(photo);

        imagePhoto.setImageBitmap(null);
        String uriStr = photo.getUriString();
        executor.execute(() -> {
            Bitmap bmp = ImageLoader.loadScaled(this, uriStr, 1920);
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
        findViewById(R.id.btnPrev).setEnabled(size > 1);
        findViewById(R.id.btnNext).setEnabled(size > 1);
    }

    protected void updateTitle() {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;
        String segment = Uri.parse(photo.getUriString()).getLastPathSegment();
        if (tvPhotoTitle != null) {
            tvPhotoTitle.setText(segment != null ? segment : "Photo");
        }
    }

    // -------------------------------------------------------------------------
    // Date
    // -------------------------------------------------------------------------

    private void updateDate(Photo photo) {
        executor.execute(() -> {
            String date = resolveDate(photo.getUriString());
            mainHandler.post(() -> tvPhotoDate.setText(date));
        });
    }

    private String resolveDate(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            if ("file".equals(uri.getScheme())) {
                File f = new File(uri.getPath());
                return DATE_FMT.format(new Date(f.lastModified()));
            }
            String[] proj = {MediaStore.Images.Media.DATE_TAKEN,
                             MediaStore.Images.Media.DATE_ADDED};
            try (Cursor c = getContentResolver().query(uri, proj, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    long taken = c.getLong(0);
                    if (taken > 0) return DATE_FMT.format(new Date(taken));
                    long added = c.getLong(1);
                    if (added > 0) return DATE_FMT.format(new Date(added * 1000));
                }
            }
        } catch (Exception ignored) {}
        return "Date unknown";
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

    protected void rebuildChips(Photo photo) {
        chipGroupTags.removeAllViews();
        if (photo == null || photo.getTags().isEmpty()) {
            tvNoTags.setVisibility(View.VISIBLE);
            chipGroupTags.setVisibility(View.GONE);
            return;
        }
        tvNoTags.setVisibility(View.GONE);
        chipGroupTags.setVisibility(View.VISIBLE);

        int accent = SettingsManager.get(this).getAccentColor();
        int accentAlpha = (accent & 0x00FFFFFF) | 0x33000000; // 20% opacity

        for (Tag tag : photo.getTags()) {
            Chip chip = new Chip(this);
            chip.setText(tag.toString());
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(accentAlpha));
            chip.setTextColor(0xFFFFFFFF);
            chip.setCloseIconTint(ColorStateList.valueOf(0xAAFFFFFF));
            chip.setOnCloseIconClickListener(v -> confirmDeleteTag(photo, tag));
            chipGroupTags.addView(chip);
        }
    }

    private void confirmDeleteTag(Photo photo, Tag tag) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Tag")
                .setMessage("Remove \"" + tag + "\"?")
                .setPositiveButton("Remove", (d, w) -> {
                    photo.removeTag(tag);
                    PhotoLibrary.getInstance().save(this);
                    rebuildChips(photo);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddTagDialog() {
        Photo photo = getCurrentPhoto();
        if (photo == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tag, null);
        AutoCompleteTextView autoType = dialogView.findViewById(R.id.autoCompleteTagType);
        EditText editValue            = dialogView.findViewById(R.id.editTagValue);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, getAllKnownTagTypes());
        autoType.setAdapter(typeAdapter);
        autoType.setThreshold(1);

        new AlertDialog.Builder(this)
                .setTitle("Add Tag")
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String type  = autoType.getText().toString().trim().toLowerCase();
                    String value = editValue.getText().toString().trim();
                    if (type.isEmpty()) {
                        Toast.makeText(this, "Type cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Value cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Tag tag = new Tag(type, value);
                        if (!photo.addTag(tag)) {
                            Toast.makeText(this, "Tag already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        PhotoLibrary.getInstance().save(this);
                        rebuildChips(photo);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<String> getAllKnownTagTypes() {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        types.add("person");
        types.add("location");
        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            for (Photo p : album.getPhotos()) {
                for (Tag t : p.getTags()) {
                    types.add(t.getType());
                }
            }
        }
        return new ArrayList<>(types);
    }

    // -------------------------------------------------------------------------
    // Move photo
    // -------------------------------------------------------------------------

    protected void showMoveToAlbumDialog() {
        List<Album> all = PhotoLibrary.getInstance().getAlbums();
        List<String> others = new ArrayList<>();
        for (Album a : all) {
            if (!a.getName().equalsIgnoreCase(albumName)) others.add(a.getName());
        }
        if (others.isEmpty()) {
            Toast.makeText(this, "No other albums", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = others.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Move to Album")
                .setItems(names, (d, which) -> movePhotoToAlbum(names[which]))
                .show();
    }

    private void movePhotoToAlbum(String targetName) {
        Photo photo  = getCurrentPhoto();
        Album source = PhotoLibrary.getInstance().getAlbum(albumName);
        Album target = PhotoLibrary.getInstance().getAlbum(targetName);
        if (photo == null || source == null || target == null) return;
        target.addPhoto(photo);
        source.removePhoto(photo);
        PhotoLibrary.getInstance().save(this);
        finish();
    }
}
