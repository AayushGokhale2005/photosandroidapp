package com.example.photos.model;

import java.util.Objects;

public class Tag {

    private final String type;
    private final String value;

    public Tag(String type, String value) {
        String t = type  == null ? "" : type.trim().toLowerCase();
        String v = value == null ? "" : value.trim().toLowerCase();
        if (t.isEmpty()) throw new IllegalArgumentException("Tag type cannot be empty");
        if (v.isEmpty()) throw new IllegalArgumentException("Tag value cannot be empty");
        this.type  = t;
        this.value = v;
    }

    public String getType()  { return type; }
    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag other = (Tag) o;
        return type.equals(other.type) && value.equals(other.value);
    }

    @Override
    public int hashCode() { return Objects.hash(type, value); }

    @Override
    public String toString() { return type + ": " + value; }
}
