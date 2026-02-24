package com.messagerie.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

// Représente un message en base de données
@Entity
@Table(name = "messages")
public class Message implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'expéditeur du message
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Le destinataire du message
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // Contenu du message (max 1000 caractères - RG7)
    @Column(length = 1000, nullable = false)
    private String contenu;

    // Date d'envoi
    private LocalDateTime dateEnvoi;

    // Statut : ENVOYE, RECU ou LU
    @Enumerated(EnumType.STRING)
    private MessageStatus statut;

    // Constructeur vide obligatoire pour Hibernate
    public Message() {}

    // Constructeur pour créer un message
    public Message(User sender, User receiver, String contenu) {
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.dateEnvoi = LocalDateTime.now();
        this.statut = MessageStatus.ENVOYE;
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public MessageStatus getStatut() {
        return statut;
    }

    public void setStatut(MessageStatus statut) {
        this.statut = statut;
    }
}
