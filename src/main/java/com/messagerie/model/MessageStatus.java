package com.messagerie.model;

// Statut d'un message
public enum MessageStatus {
    ENVOYE,   // message envoyé mais pas encore livré
    RECU,     // message livré au destinataire
    LU        // message lu par le destinataire
}
