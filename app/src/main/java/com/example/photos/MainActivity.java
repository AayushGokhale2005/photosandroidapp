package com.example.photos;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.PhotoLibrary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private AlbumListAdapter adapter;
    private GridLayoutManager glm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark/light mode before super so theme is correct
        SettingsManager sm = SettingsManager.get(this);
        AppCompatDelegate.setDefaultNightMode(
                sm.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES
                               : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ThemeHelper.applyAccent(this);

        PhotoLibrary.getInstance().load(this);
        PhotoLibrary.getInstance().seedStockAlbum(this);

        int cols = sm.getAlbumCols();
        RecyclerView recycler = findViewById(R.id.recyclerAlbums);
        glm = new GridLayoutManager(this, cols);
        recycler.setLayoutManager(glm);
        recycler.addItemDecoration(new GridSpacingDecoration(cols));
        recycler.setHasFixedSize(true);

        adapter = new AlbumListAdapter(
                PhotoLibrary.getInstance().getAlbums(),
                this,
                this::onAlbumClick,
                this::onAlbumLongClick
        );
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNewAlbum);
        fab.setOnClickListener(v -> showCreateAlbumDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onAlbumClick(Album album) {
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra(AlbumActivity.EXTRA_ALBUM_NAME, album.getName());
        startActivity(intent);
    }

    private void onAlbumLongClick(Album album) {
        new AlertDialog.Builder(this)
                .setTitle(album.getName())
                .setItems(new String[]{"Rename", "Delete"}, (dialog, which) -> {
                    if (which == 0) showRenameAlbumDialog(album);
                    else           showDeleteAlbumDialog(album);
                })
                .show();
    }

    private void showCreateAlbumDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Album name");

        new AlertDialog.Builder(this)
                .setTitle("New Album")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!PhotoLibrary.getInstance().addAlbum(new Album(name))) {
                        Toast.makeText(this, "Album already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PhotoLibrary.getInstance().save(this);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameAlbumDialog(Album album) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(album.getName());
        input.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("Rename Album")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!PhotoLibrary.getInstance().renameAlbum(album.getName(), newName)) {
                        Toast.makeText(this, "Album already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PhotoLibrary.getInstance().save(this);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAlbumDialog(Album album) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Album")
                .setMessage("Delete \"" + album.getName() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    PhotoLibrary.getInstance().deleteAlbum(album.getName());
                    PhotoLibrary.getInstance().save(this);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh grid columns (may have changed in settings)
        int cols = SettingsManager.get(this).getAlbumCols();
        glm.setSpanCount(cols);
        adapter.notifyDataSetChanged();
        ThemeHelper.applyAccent(this);
    }
}
