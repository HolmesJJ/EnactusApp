package com.example.enactusapp.Event.PossibleAnswerEvent;

import java.util.List;

public class PossibleAnswersEvent {

    private final List<String> possibleAnswers;

    public PossibleAnswersEvent(List<String> possibleAnswers) {
        this.possibleAnswers = possibleAnswers;
    }

    public List<String> getPossibleAnswers() {
        return possibleAnswers;
    }
}
