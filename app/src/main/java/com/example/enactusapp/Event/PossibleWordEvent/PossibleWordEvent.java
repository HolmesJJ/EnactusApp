package com.example.enactusapp.Event.PossibleWordEvent;

public class PossibleWordEvent {

    private final String possibleWord;

    public PossibleWordEvent(String possibleWord) {
        this.possibleWord = possibleWord;
    }

    public String getPossibleWord() {
        return possibleWord;
    }
}
