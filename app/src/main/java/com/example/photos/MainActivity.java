package com.example.photos;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photos.model.Album;
import com.example.photos.model.PhotoLibrary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private AlbumListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PhotoLibrary.getInstance().load(this);
        PhotoLibrary.getInstance().seedStockAlbum(this);

        RecyclerView recycler = findViewById(R.id.recyclerAlbums);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        recycler.setLayoutManager(glm);
        recycler.addItemDecoration(new GridSpacingDecoration(2));

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
        if (item.getItemId() == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
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
        adapter.notifyDataSetChanged();
    }
}
