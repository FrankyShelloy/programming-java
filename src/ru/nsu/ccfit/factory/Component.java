package ru.nsu.ccfit.factory;

public class Component {
    private final long id;

    public Component(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}

