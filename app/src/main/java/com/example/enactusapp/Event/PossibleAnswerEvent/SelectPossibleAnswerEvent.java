package com.example.enactusapp.Event.PossibleAnswerEvent;

public class SelectPossibleAnswerEvent {

    private final int position;

    public SelectPossibleAnswerEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
