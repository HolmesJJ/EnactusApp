package com.example.enactusapp.Entity;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class BlinkEvent {

    private boolean isLeftEye;

    public BlinkEvent(boolean isLeftEye) {
        this.isLeftEye = isLeftEye;
    }

    public boolean isLeftEye() {
        return isLeftEye;
    }

    public void setLeftEye(boolean leftEye) {
        isLeftEye = leftEye;
    }
}
