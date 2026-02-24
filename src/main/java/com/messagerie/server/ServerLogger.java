package com.messagerie.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Classe pour enregistrer les événements du serveur dans la console (RG12)
public class ServerLogger {

    // Format de la date et heure
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Retourne l'heure actuelle formatée
    private String maintenant() {
        return LocalDateTime.now().format(FORMAT);
    }

    // Enregistre une connexion
    public void logConnexion(String username) {
        System.out.println("[" + maintenant() + "] CONNEXION  : " + username + " est connecté.");
    }

    // Enregistre une déconnexion
    public void logDeconnexion(String username) {
        System.out.println("[" + maintenant() + "] DECONNEXION: " + username + " s'est déconnecté.");
    }

    // Enregistre l'envoi d'un message
    public void logMessage(String expediteur, String destinataire) {
        System.out.println("[" + maintenant() + "] MESSAGE    : " + expediteur + " -> " + destinataire);
    }

    // Enregistre une information générale
    public void logInfo(String info) {
        System.out.println("[" + maintenant() + "] INFO       : " + info);
    }
}
