package com.example.enactusapp.Entity;

public class Selection {

    private final int id;
    private final String name;

    public Selection(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
