package com.messagerie.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

// Représente un utilisateur en base de données
@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le username doit être unique (RG1)
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // Statut : ONLINE ou OFFLINE
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // Date de création du compte
    private LocalDateTime dateCreation;

    // Constructeur vide obligatoire pour Hibernate
    public User() {}

    // Constructeur pour créer un utilisateur
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = UserStatus.OFFLINE;
        this.dateCreation = LocalDateTime.now();
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return username;
    }
}
