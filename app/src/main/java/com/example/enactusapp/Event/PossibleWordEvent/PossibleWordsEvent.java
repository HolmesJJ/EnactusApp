package com.example.enactusapp.Event.PossibleWordEvent;

import java.util.List;

public class PossibleWordsEvent {

    private List<String> possibleWords;

    public PossibleWordsEvent(List<String> possibleWords) {
        this.possibleWords = possibleWords;
    }

    public List<String> getPossibleWords() {
        return possibleWords;
    }

    public void setPossibleWords(List<String> possibleWords) {
        this.possibleWords = possibleWords;
    }
}
