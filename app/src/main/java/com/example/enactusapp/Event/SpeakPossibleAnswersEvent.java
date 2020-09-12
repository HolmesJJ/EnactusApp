package com.example.enactusapp.Event;

import com.example.enactusapp.Entity.User;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class SpeakPossibleAnswersEvent {

    private String answer;

    public SpeakPossibleAnswersEvent(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
