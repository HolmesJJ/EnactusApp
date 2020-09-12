package com.example.enactusapp.Event;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class StartChatEvent {

    private boolean isStartChat;

    public StartChatEvent(boolean isStartChat) {
        this.isStartChat = isStartChat;
    }

    public boolean isStartChat() {
        return isStartChat;
    }

    public void setStartChat(boolean startChat) {
        isStartChat = startChat;
    }
}
