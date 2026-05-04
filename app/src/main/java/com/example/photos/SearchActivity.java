package com.example.photos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.Photo;
import com.example.photos.model.PhotoLibrary;
import com.example.photos.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private Spinner spinnerType1;
    private Spinner spinnerType2;
    private AutoCompleteTextView autoCompleteValue1;
    private AutoCompleteTextView autoCompleteValue2;
    private RadioGroup radioGroupLogic;
    private PhotoGridAdapter resultsAdapter;
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

        spinnerType1       = findViewById(R.id.spinnerType1);
        spinnerType2       = findViewById(R.id.spinnerType2);
        autoCompleteValue1 = findViewById(R.id.autoCompleteValue1);
        autoCompleteValue2 = findViewById(R.id.autoCompleteValue2);
        radioGroupLogic    = findViewById(R.id.radioGroupLogic);
        Button btnSearch   = findViewById(R.id.btnSearch);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"person", "location"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType1.setAdapter(typeAdapter);

        ArrayAdapter<String> typeAdapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"person", "location"});
        typeAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType2.setAdapter(typeAdapter2);

        setupAutoComplete();

        RecyclerView recyclerResults = findViewById(R.id.recyclerResults);
        recyclerResults.setLayoutManager(new GridLayoutManager(this, 2));
        resultsAdapter = new PhotoGridAdapter(resultPhotos, this, this::openPhotoFromResult);
        recyclerResults.setAdapter(resultsAdapter);

        btnSearch.setOnClickListener(v -> runSearch());
    }

    private void setupAutoComplete() {
        spinnerType1.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                updateAutoComplete(autoCompleteValue1, (String) spinnerType1.getSelectedItem());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        spinnerType2.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                updateAutoComplete(autoCompleteValue2, (String) spinnerType2.getSelectedItem());
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        // Initial population
        updateAutoComplete(autoCompleteValue1, "person");
        updateAutoComplete(autoCompleteValue2, "person");
    }

    private void updateAutoComplete(AutoCompleteTextView view, String type) {
        List<String> values = new ArrayList<>();
        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getType().equals(type) && !values.contains(tag.getValue())) {
                        values.add(tag.getValue());
                    }
                }
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, values);
        view.setAdapter(adapter);
    }

    private void runSearch() {
        String val1 = autoCompleteValue1.getText().toString().trim().toLowerCase();
        String val2 = autoCompleteValue2.getText().toString().trim().toLowerCase();
        String type1 = (String) spinnerType1.getSelectedItem();
        String type2 = (String) spinnerType2.getSelectedItem();
        boolean isAnd = radioGroupLogic.getCheckedRadioButtonId() == R.id.radioAnd;

        resultPhotos.clear();

        if (val1.isEmpty()) {
            resultsAdapter.notifyDataSetChanged();
            return;
        }

        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                boolean matches;
                if (val2.isEmpty()) {
                    matches = photoMatchesTag(photo, type1, val1);
                } else if (isAnd) {
                    matches = photoMatchesTag(photo, type1, val1) && photoMatchesTag(photo, type2, val2);
                } else {
                    matches = photoMatchesTag(photo, type1, val1) || photoMatchesTag(photo, type2, val2);
                }
                if (matches && !resultPhotos.contains(photo)) {
                    resultPhotos.add(photo);
                }
            }
        }

        resultsAdapter.notifyDataSetChanged();
    }

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
        Photo targetPhoto = resultPhotos.get(position);
        // Find which album contains this photo and open PhotoDisplayActivity
        for (Album album : PhotoLibrary.getInstance().getAlbums()) {
            List<Photo> photos = album.getPhotos();
            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).getUriString().equals(targetPhoto.getUriString())) {
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh autocomplete in case tags were added since this screen was created
        updateAutoComplete(autoCompleteValue1, (String) spinnerType1.getSelectedItem());
        updateAutoComplete(autoCompleteValue2, (String) spinnerType2.getSelectedItem());
    }
}
