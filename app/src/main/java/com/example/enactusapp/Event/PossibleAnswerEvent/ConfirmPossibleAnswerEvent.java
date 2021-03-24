package com.example.enactusapp.Event.PossibleAnswerEvent;

public class ConfirmPossibleAnswerEvent {

    private int position;

    public ConfirmPossibleAnswerEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
