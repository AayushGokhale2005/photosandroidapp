package com.example.photos.model;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    public int getAlbumCount() {
        return albums.size();
    }

    public boolean isEmpty() {
        return albums.isEmpty();
    }

    public void clear() {
        albums.clear();
    }

    public boolean addAlbum(Album album) {
        if (album == null || album.getName().trim().isEmpty()) {
            return false;
        }

        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(album.getName())) {
                return false;
            }
        }

        albums.add(album);
        return true;
    }

    public boolean deleteAlbum(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        return albums.removeIf(a -> a.getName().equalsIgnoreCase(name.trim()));
    }

    public boolean renameAlbum(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }

        String trimmed = newName.trim();

        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(trimmed)) {
                return false;
            }
        }

        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(oldName.trim())) {
                a.setName(trimmed);
                return true;
            }
        }

        return false;
    }

    public Album getAlbum(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(name.trim())) {
                return a;
            }
        }

        return null;
    }

    public void seedStockAlbum(Context context) {
        if (context == null || getAlbum("Stock") != null) return;

        String[] assetNames;
        try {
            assetNames = context.getAssets().list("stock");
        } catch (IOException e) {
            return;
        }

        if (assetNames == null || assetNames.length == 0) return;

        File stockDir = new File(context.getFilesDir(), "stock");
        stockDir.mkdirs();

        Album stockAlbum = new Album("Stock");

        for (String name : assetNames) {
            File dest = new File(stockDir, name);

            if (!dest.exists()) {
                try (InputStream in = context.getAssets().open("stock/" + name);
                     OutputStream out = new FileOutputStream(dest)) {

                    byte[] buf = new byte[8192];
                    int len;

                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }

                } catch (IOException e) {
                    continue;
                }
            }

            stockAlbum.addPhoto(new Photo(Uri.fromFile(dest).toString()));
        }

        if (stockAlbum.getPhotoCount() > 0) {
            albums.add(0, stockAlbum);
            save(context);
        }
    }

    public void save(Context context) {
        if (context == null) {
            return;
        }

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
        if (context == null) {
            return;
        }

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

                JSONArray jPhotos = jAlbum.optJSONArray("photos");

                if (jPhotos != null) {
                    for (int j = 0; j < jPhotos.length(); j++) {
                        JSONObject jPhoto = jPhotos.getJSONObject(j);
                        Photo photo = new Photo(jPhoto.getString("uri"));

                        JSONArray jTags = jPhoto.optJSONArray("tags");

                        if (jTags != null) {
                            for (int k = 0; k < jTags.length(); k++) {
                                JSONObject jTag = jTags.getJSONObject(k);

                                try {
                                    Tag tag = new Tag(
                                            jTag.getString("type"),
                                            jTag.getString("value")
                                    );
                                    photo.addTag(tag);
                                } catch (IllegalArgumentException ignored) {
                                    // Skip tags with invalid types from older data
                                }
                            }
                        }

                        album.addPhoto(photo);
                    }
                }

                albums.add(album);
            }

        } catch (JSONException | IOException e) {
            // Leave albums empty if file is corrupt
        }
    }
}