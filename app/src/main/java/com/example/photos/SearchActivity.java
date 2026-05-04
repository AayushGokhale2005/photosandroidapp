package com.example.photos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;
import com.example.photos.model.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    // Row 1
    private AutoCompleteTextView autoType1;
    private AutoCompleteTextView autoValue1;
    // Row 2
    private AutoCompleteTextView autoType2;
    private AutoCompleteTextView autoValue2;

    private RadioGroup radioGroupLogic;
    private PhotoGridAdapter resultsAdapter;
    private TextView tvSearchEmpty;
    private final List<Photo> resultPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search");
        }

        ThemeHelper.applyAccent(this);

        autoType1       = findViewById(R.id.spinnerType1);
        autoValue1      = findViewById(R.id.autoCompleteValue1);
        autoType2       = findViewById(R.id.spinnerType2);
        autoValue2      = findViewById(R.id.autoCompleteValue2);
        radioGroupLogic = findViewById(R.id.radioGroupLogic);
        tvSearchEmpty   = findViewById(R.id.tvSearchEmpty);
        Button btnSearch = findViewById(R.id.btnSearch);

        autoType1.setThreshold(1);
        autoType2.setThreshold(1);
        autoValue1.setThreshold(1);
        autoValue2.setThreshold(1);

        autoType1.setOnItemClickListener((p, v, pos, id) ->
                updateValueSuggestions(autoValue1, autoType1.getText().toString().trim().toLowerCase()));
        autoType2.setOnItemClickListener((p, v, pos, id) ->
                updateValueSuggestions(autoValue2, autoType2.getText().toString().trim().toLowerCase()));

        autoType1.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateValueSuggestions(autoValue1, autoType1.getText().toString().trim().toLowerCase());
        });
        autoType2.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateValueSuggestions(autoValue2, autoType2.getText().toString().trim().toLowerCase());
        });

        int cols = SettingsManager.get(this).getPhotoCols();
        RecyclerView recyclerResults = findViewById(R.id.recyclerResults);
        recyclerResults.setLayoutManager(new GridLayoutManager(this, cols));
        recyclerResults.addItemDecoration(new GridSpacingDecoration(2));
        recyclerResults.setHasFixedSize(false);
        resultsAdapter = new PhotoGridAdapter(resultPhotos, this, this::openPhotoFromResult);
        recyclerResults.setAdapter(resultsAdapter);

        btnSearch.setOnClickListener(v -> runSearch());

        refreshAllSuggestions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllSuggestions();
    }

    // -------------------------------------------------------------------------
    // Suggestions
    // -------------------------------------------------------------------------

    private void refreshAllSuggestions() {
        List<String> types = getAllKnownTypes();
        setTypeAdapter(autoType1, types);
        setTypeAdapter(autoType2, types);
        updateValueSuggestions(autoValue1, autoType1.getText().toString().trim().toLowerCase());
        updateValueSuggestions(autoValue2, autoType2.getText().toString().trim().toLowerCase());
    }

    private void setTypeAdapter(AutoCompleteTextView view, List<String> types) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, types);
        view.setAdapter(a);
    }

    private void updateValueSuggestions(AutoCompleteTextView valueView, String type) {
        List<String> values = new ArrayList<>();
        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    boolean typeMatch = type.isEmpty() || tag.getType().equals(type);
                    if (typeMatch && !values.contains(tag.getValue())) {
                        values.add(tag.getValue());
                    }
                }
            }
        }
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, values);
        valueView.setAdapter(a);
    }

    private List<String> getAllKnownTypes() {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        types.add("person");
        types.add("location");
        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    types.add(tag.getType());
                }
            }
        }
        return new ArrayList<>(types);
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    private void runSearch() {
        String type1 = autoType1.getText().toString().trim().toLowerCase();
        String val1  = autoValue1.getText().toString().trim().toLowerCase();
        String type2 = autoType2.getText().toString().trim().toLowerCase();
        String val2  = autoValue2.getText().toString().trim().toLowerCase();
        boolean isAnd = radioGroupLogic.getCheckedRadioButtonId() == R.id.radioAnd;

        resultPhotos.clear();

        boolean row1valid = !type1.isEmpty() && !val1.isEmpty();
        boolean row2valid = !type2.isEmpty() && !val2.isEmpty();

        if (!row1valid) {
            resultsAdapter.notifyDataSetChanged();
            tvSearchEmpty.setText("Fill in at least the first type and value, then tap Search.");
            tvSearchEmpty.setVisibility(View.VISIBLE);
            return;
        }

        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                boolean matches;
                if (!row2valid) {
                    matches = photoMatchesTag(photo, type1, val1);
                } else if (isAnd) {
                    matches = photoMatchesTag(photo, type1, val1)
                            && photoMatchesTag(photo, type2, val2);
                } else {
                    matches = photoMatchesTag(photo, type1, val1)
                            || photoMatchesTag(photo, type2, val2);
                }
                if (matches && !resultPhotos.contains(photo)) {
                    resultPhotos.add(photo);
                }
            }
        }

        resultsAdapter.notifyDataSetChanged();
        if (resultPhotos.isEmpty()) {
            tvSearchEmpty.setText("No photos match — make sure photos are tagged with the same key.");
            tvSearchEmpty.setVisibility(View.VISIBLE);
        } else {
            tvSearchEmpty.setVisibility(View.GONE);
            Toast.makeText(this, resultPhotos.size() + " photo(s) found", Toast.LENGTH_SHORT).show();
        }
    }

    /** Prefix match, case-insensitive — "New" matches "new york", "new mexico" etc. */
    private boolean photoMatchesTag(Photo photo, String type, String prefix) {
        for (Tag tag : photo.getTags()) {
            if (tag.getType().equals(type) && tag.getValue().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void openPhotoFromResult(int position) {
        if (position >= resultPhotos.size()) return;
        String targetUri = resultPhotos.get(position).getUriString();
        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            List<Photo> photos = album.getPhotos();
            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).getUriString().equals(targetUri)) {
                    Intent intent = new Intent(this, PhotoDisplayActivity.class);
                    intent.putExtra(PhotoDisplayActivity.EXTRA_ALBUM_NAME, album.getName());
                    intent.putExtra(PhotoDisplayActivity.EXTRA_PHOTO_INDEX, i);
                    startActivity(intent);
                    return;
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
