package com.example.enactusapp.Event.PossibleWordEvent;

public class SelectPossibleWordEvent {

    private int position;

    public SelectPossibleWordEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
