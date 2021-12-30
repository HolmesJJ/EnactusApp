package com.example.enactusapp.Event.PossibleAnswerEvent;

public class GeneratePossibleAnswersEvent {

    private final String message;

    public GeneratePossibleAnswersEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}