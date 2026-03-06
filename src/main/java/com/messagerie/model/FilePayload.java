package com.messagerie.model;

import java.io.Serializable;

// Transporte un fichier entre le client et le serveur via les sockets
public class FilePayload implements Serializable {

    private String filename;          // Nom du fichier original
    private byte[] data;              // Contenu binaire du fichier
    private String senderUsername;    // Expéditeur
    private String receiverUsername;  // Destinataire

    public FilePayload() {}

    public FilePayload(String filename, byte[] data, String senderUsername, String receiverUsername) {
        this.filename = filename;
        this.data = data;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
    }

    // --- Getters et Setters ---

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }
}
