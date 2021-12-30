package com.example.enactusapp.Event.PossibleWordEvent;

import java.util.List;

public class PossibleWordsEvent {

    private final List<String> possibleWords;

    public PossibleWordsEvent(List<String> possibleWords) {
        this.possibleWords = possibleWords;
    }

    public List<String> getPossibleWords() {
        return possibleWords;
    }
}
