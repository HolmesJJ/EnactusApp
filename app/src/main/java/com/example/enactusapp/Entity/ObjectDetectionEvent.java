package com.example.enactusapp.Entity;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class ObjectDetectionEvent {

    private boolean isShowed;

    public ObjectDetectionEvent(boolean isShowed) {
        this.isShowed = isShowed;
    }

    public boolean isShowed() {
        return isShowed;
    }

    public void setShowed(boolean showed) {
        isShowed = showed;
    }
}
