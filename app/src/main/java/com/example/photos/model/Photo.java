package com.example.photos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Photo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String uriString;
    private final List<Tag> tags;

    public Photo(String uriString) {
        this.uriString = uriString == null ? "" : uriString.trim();
        this.tags = new ArrayList<>();
    }

    public String getUriString() {
        return uriString;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public boolean addTag(Tag tag) {
        if (tag == null || tags.contains(tag)) {
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

    public boolean hasTags() {
        return !tags.isEmpty();
    }

    public int getTagCount() {
        return tags.size();
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
        return Objects.hash(uriString);
    }

    @Override
    public String toString() {
        return uriString;
    }
}