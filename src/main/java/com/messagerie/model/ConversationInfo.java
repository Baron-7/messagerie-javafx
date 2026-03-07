package com.messagerie.model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Résumé d'une conversation : partenaire + dernier message + statut en ligne
// Transporté dans le paquet GET_CONVERSATIONS
public class ConversationInfo implements Serializable {

    private User partner;
    private String lastMessage;
    private LocalDateTime lastDate;
    private int unreadCount;
    private boolean online;

    public ConversationInfo(User partner, String lastMessage, LocalDateTime lastDate, int unreadCount) {
        this.partner = partner;
        this.lastMessage = lastMessage;
        this.lastDate = lastDate;
        this.unreadCount = unreadCount;
    }

    public User getPartner()             { return partner; }
    public String getLastMessage()       { return lastMessage; }
    public LocalDateTime getLastDate()   { return lastDate; }
    public int getUnreadCount()          { return unreadCount; }
    public boolean isOnline()            { return online; }
    public void setOnline(boolean online){ this.online = online; }
    public void setUnreadCount(int n)    { this.unreadCount = n; }
}
