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
    @FXML private Label labelErreur;

    // Quand on clique sur "Se connecter"
    @FXML
    public void seConnecter() {
        String username = champUsername.getText().trim();
        String password = champPassword.getText();

        // Vérification basique des champs
        if (username.isEmpty() || password.isEmpty()) {
            labelErreur.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            // On se connecte au serveur
            Client client = new Client();
            client.connecter();

            // On envoie un paquet de connexion
            Packet paquet = new Packet(Packet.LOGIN, new String[]{username, password});
            client.envoyerPaquet(paquet);

            // On attend la réponse du serveur
            Packet reponse = client.recevoirPaquet();

            if (reponse.isSuccess()) {
                // Connexion réussie : on sauvegarde l'utilisateur et on ouvre le chat
                User user = (User) reponse.getData();
                client.setCurrentUser(user);
                ouvrirChat(client);
            } else {
                // Erreur : on affiche le message
                labelErreur.setText(reponse.getMessage());
            }

        } catch (Exception e) {
            labelErreur.setText("Impossible de se connecter au serveur. Est-il démarré ?");
        }
    }

    // Quand on clique sur "S'inscrire" : on va à l'écran d'inscription
    @FXML
    public void allerInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Scene scene = new Scene(loader.load(), 440, 560);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
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
            Scene scene = new Scene(loader.load(), 720, 520);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // On passe le client au ChatController
            ChatController chatController = loader.getController();
            chatController.setClient(client);

            Stage stage = (Stage) champUsername.getScene().getWindow();
            stage.setTitle("Messagerie - " + client.getCurrentUser().getUsername());
            stage.setScene(scene);

        } catch (Exception e) {
            labelErreur.setText("Erreur lors de l'ouverture du chat.");
        }
    }
}
