package com.example.enactusapp.Event.PossibleAnswerEvent;

public class ConfirmPossibleAnswerEvent {

    private final int position;

    public ConfirmPossibleAnswerEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
