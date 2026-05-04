package com.example.photos.model;

import java.util.ArrayList;
import java.util.List;

public class Album {

    private String name;
    private final List<Photo> photos;

    public Album(String name) {
        this.name   = name.trim();
        this.photos = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public boolean addPhoto(Photo photo) {
        for (Photo p : photos) {
            if (p.getUriString().equals(photo.getUriString())) {
                return false;
            }
        }
        photos.add(photo);
        return true;
    }

    public void removePhoto(Photo photo) {
        photos.remove(photo);
    }

    public int getPhotoCount() {
        return photos.size();
    }
}
