package com.example.photos.model;

import java.util.ArrayList;
import java.util.List;

public class Photo {

    private final String uriString;
    private final List<Tag> tags;

    public Photo(String uriString) {
        this.uriString = uriString;
        this.tags      = new ArrayList<>();
    }

    public String getUriString() {
        return uriString;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public boolean addTag(Tag tag) {
        if (tags.contains(tag)) {
            return false;
        }
        tags.add(tag);
        return true;
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    public boolean hasTag(String type, String value) {
        return tags.contains(new Tag(type, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Photo)) return false;
        Photo other = (Photo) o;
        return uriString.equals(other.uriString);
    }

    @Override
    public int hashCode() {
        return uriString.hashCode();
    }
}
