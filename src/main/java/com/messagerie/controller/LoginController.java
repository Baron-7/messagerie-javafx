package com.messagerie.controller;

import com.messagerie.client.Client;
import com.messagerie.model.Packet;
import com.messagerie.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Contrôleur de l'écran de connexion
public class LoginController {

    @FXML private TextField champUsername;
    @FXML private PasswordField champPassword;
    @FXML private TextField champServeur;
    @FXML private Label labelErreur;

    // Permet de pré-remplir l'adresse serveur (appelé depuis RegisterController)
    public void setServeurInitial(String adresse) {
        if (champServeur != null && adresse != null && !adresse.isEmpty()) {
            champServeur.setText(adresse);
        }
    }

    // Quand on clique sur "Se connecter"
    @FXML
    public void seConnecter() {
        String username = champUsername.getText().trim();
        String password = champPassword.getText();
        String serveur  = champServeur.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            labelErreur.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            // On crée le client avec l'adresse du serveur saisie
            Client client = new Client();
            client.setHost(serveur.isEmpty() ? "localhost" : serveur);
            client.connecter();

            Packet paquet = new Packet(Packet.LOGIN, new String[]{username, password});
            client.envoyerPaquet(paquet);

            Packet reponse = client.recevoirPaquet();

            if (reponse.isSuccess()) {
                User user = (User) reponse.getData();
                client.setCurrentUser(user);
                ouvrirChat(client);
            } else {
                labelErreur.setText(reponse.getMessage());
            }

        } catch (Exception e) {
            labelErreur.setText("Impossible de joindre le serveur « " +
                    (serveur.isEmpty() ? "localhost" : serveur) + " ». Est-il démarré ?");
        }
    }

    // Quand on clique sur "S'inscrire" : on va à l'écran d'inscription
    // On transmet aussi l'adresse du serveur saisie
    @FXML
    public void allerInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(loader.load(), 440, 560);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // On passe l'adresse serveur au RegisterController
            RegisterController rc = loader.getController();
            rc.setServeur(champServeur.getText().trim());

            Stage stage = (Stage) champUsername.getScene().getWindow();
            stage.setTitle("Messagerie - Inscription");
            stage.setScene(scene);
        } catch (Exception e) {
            labelErreur.setText("Erreur lors du changement d'écran.");
        }
    }

    // Ouvre l'écran de chat et passe le client au contrôleur
    private void ouvrirChat(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load(), 860, 580);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            ChatController chatController = loader.getController();
            chatController.setClient(client);

            Stage stage = (Stage) champUsername.getScene().getWindow();
            stage.setTitle("Messagerie 🌿 — " + client.getCurrentUser().getAvatar()
                    + " " + client.getCurrentUser().getUsername());
            stage.setScene(scene);

        } catch (Exception e) {
            labelErreur.setText("Erreur lors de l'ouverture du chat.");
        }
    }
}
