package com.example.photos.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoLibrary {

    private static final String FILE_NAME = "photo_library.json";

    private static PhotoLibrary instance;

    private final List<Album> albums;

    private PhotoLibrary() {
        albums = new ArrayList<>();
    }

    public static PhotoLibrary getInstance() {
        if (instance == null) {
            instance = new PhotoLibrary();
        }
        return instance;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public boolean addAlbum(Album album) {
        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(album.getName())) {
                return false;
            }
        }
        albums.add(album);
        return true;
    }

    public boolean deleteAlbum(String name) {
        return albums.removeIf(a -> a.getName().equalsIgnoreCase(name));
    }

    public boolean renameAlbum(String oldName, String newName) {
        String trimmed = newName.trim();
        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(trimmed)) {
                return false;
            }
        }
        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(oldName)) {
                a.setName(trimmed);
                return true;
            }
        }
        return false;
    }

    public Album getAlbum(String name) {
        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(name)) {
                return a;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void save(Context context) {
        try {
            JSONArray jsonAlbums = new JSONArray();
            for (Album album : albums) {
                JSONObject jAlbum = new JSONObject();
                jAlbum.put("name", album.getName());

                JSONArray jPhotos = new JSONArray();
                for (Photo photo : album.getPhotos()) {
                    JSONObject jPhoto = new JSONObject();
                    jPhoto.put("uri", photo.getUriString());

                    JSONArray jTags = new JSONArray();
                    for (Tag tag : photo.getTags()) {
                        JSONObject jTag = new JSONObject();
                        jTag.put("type", tag.getType());
                        jTag.put("value", tag.getValue());
                        jTags.put(jTag);
                    }
                    jPhoto.put("tags", jTags);
                    jPhotos.put(jPhoto);
                }
                jAlbum.put("photos", jPhotos);
                jsonAlbums.put(jAlbum);
            }

            File file = new File(context.getFilesDir(), FILE_NAME);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonAlbums.toString());
            }
        } catch (JSONException | IOException e) {
            // Silently fail; data will be empty next launch but nothing crashes
        }
    }

    public void load(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            try (FileReader reader = new FileReader(file)) {
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
            }

            albums.clear();

            JSONArray jsonAlbums = new JSONArray(sb.toString());
            for (int i = 0; i < jsonAlbums.length(); i++) {
                JSONObject jAlbum = jsonAlbums.getJSONObject(i);
                Album album = new Album(jAlbum.getString("name"));

                JSONArray jPhotos = jAlbum.getJSONArray("photos");
                for (int j = 0; j < jPhotos.length(); j++) {
                    JSONObject jPhoto = jPhotos.getJSONObject(j);
                    Photo photo = new Photo(jPhoto.getString("uri"));

                    JSONArray jTags = jPhoto.getJSONArray("tags");
                    for (int k = 0; k < jTags.length(); k++) {
                        JSONObject jTag = jTags.getJSONObject(k);
                        try {
                            Tag tag = new Tag(jTag.getString("type"), jTag.getString("value"));
                            photo.addTag(tag);
                        } catch (IllegalArgumentException ignored) {
                            // Skip tags with invalid types from older data
                        }
                    }
                    album.addPhoto(photo);
                }
                albums.add(album);
            }
        } catch (JSONException | IOException e) {
            // Leave albums empty if file is corrupt
        }
    }
}
