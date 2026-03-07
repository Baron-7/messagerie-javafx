package com.messagerie.model;

import java.io.Serializable;

// Paquet qui voyage entre le client et le serveur via les sockets
// POINT DE SYNCHRONISATION : les deux personnes utilisent cette classe
public class Packet implements Serializable {

    // Les types de paquets possibles
    public static final String LOGIN           = "LOGIN";
    public static final String REGISTER        = "REGISTER";
    public static final String LOGOUT          = "LOGOUT";
    public static final String SEND_MESSAGE    = "SEND_MESSAGE";
    public static final String GET_HISTORY     = "GET_HISTORY";
    public static final String GET_ONLINE_USERS = "GET_ONLINE_USERS";
    public static final String RESPONSE        = "RESPONSE";       // réponse du serveur
    public static final String NEW_MESSAGE     = "NEW_MESSAGE";    // nouveau message reçu
    public static final String DELETE_MESSAGE  = "DELETE_MESSAGE"; // demande de suppression
    public static final String MESSAGE_DELETED = "MESSAGE_DELETED"; // confirmation de suppression
    public static final String SEND_FILE       = "SEND_FILE";      // envoi d'un fichier
    public static final String NEW_FILE        = "NEW_FILE";       // fichier reçu
    public static final String TYPING         = "TYPING";         // indicateur de frappe
    public static final String MARK_READ      = "MARK_READ";      // marquer messages comme lus
    public static final String MESSAGE_READ   = "MESSAGE_READ";   // confirmation : messages lus

    private String type;      // type du paquet
    private Object data;      // données transportées
    private boolean success;  // si c'est une réponse : vrai = OK, faux = erreur
    private String message;   // message texte (ex: "Connexion réussie" ou "Erreur...")

    // Constructeur vide
    public Packet() {}

    // Constructeur rapide avec type et données
    public Packet(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    // --- Getters et Setters ---

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
