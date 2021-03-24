package com.example.enactusapp.Event.PossibleAnswerEvent;

import com.example.enactusapp.Entity.User;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;

import java.util.List;

public class MessageToPossibleAnswersEvent {

    private User user;
    private String message;
    private List<FirebaseTextMessage> chatHistory;

    public MessageToPossibleAnswersEvent(User user, String message, List<FirebaseTextMessage> chatHistory) {
        this.user = user;
        this.message = message;
        this.chatHistory = chatHistory;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FirebaseTextMessage> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<FirebaseTextMessage> chatHistory) {
        this.chatHistory = chatHistory;
    }
}