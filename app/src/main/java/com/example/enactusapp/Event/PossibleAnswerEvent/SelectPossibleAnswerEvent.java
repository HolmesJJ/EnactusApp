package com.example.enactusapp.Event.PossibleAnswerEvent;

public class SelectPossibleAnswerEvent {

    private int position;

    public SelectPossibleAnswerEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
