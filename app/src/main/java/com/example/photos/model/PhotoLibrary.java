package com.example.photos.model;

import java.util.ArrayList;
import java.util.List;

public class PhotoLibrary {

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
}
