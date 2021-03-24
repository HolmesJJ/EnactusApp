package com.example.enactusapp.Event.PossibleWordEvent;

public class ConfirmPossibleWordEvent {

    private int position;

    public ConfirmPossibleWordEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
