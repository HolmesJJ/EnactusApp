package com.example.enactusapp.Event.PossibleWordEvent;

public class ConfirmPossibleWordEvent {

    private final int position;

    public ConfirmPossibleWordEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
