package com.example.enactusapp.Event.PossibleAnswerEvent;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class SpeakPossibleAnswerEvent {

    private String answer;

    public SpeakPossibleAnswerEvent(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
