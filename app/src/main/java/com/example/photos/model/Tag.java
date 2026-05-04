package com.example.photos.model;

import java.util.Objects;

public class Tag {

    private final String type;
    private final String value;

    public Tag(String type, String value) {
        if (!type.equals("person") && !type.equals("location")) {
            throw new IllegalArgumentException("Tag type must be 'person' or 'location'");
        }
        this.type  = type.trim().toLowerCase();
        this.value = value.trim().toLowerCase();
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag other = (Tag) o;
        return type.equals(other.type) && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return type + ": " + value;
    }
}
