package com.example.enactusapp.Event.PossibleWordEvent;

public class SelectPossibleWordEvent {

    private final int position;

    public SelectPossibleWordEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
