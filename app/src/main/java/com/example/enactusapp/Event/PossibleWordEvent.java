package com.example.enactusapp.Event;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class PossibleWordEvent {

    private String possibleWord;

    public PossibleWordEvent(String possibleWord) {
        this.possibleWord = possibleWord;
    }

    public String getPossibleWord() {
        return possibleWord;
    }

    public void setPossibleWord(String possibleWord) {
        this.possibleWord = possibleWord;
    }
}
