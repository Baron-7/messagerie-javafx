package com.messagerie.controller;

import com.messagerie.client.Client;
import com.messagerie.model.Packet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Contrôleur de l'écran d'inscription
public class RegisterController {

    @FXML private TextField champUsername;
    @FXML private PasswordField champPassword;
    @FXML private PasswordField champConfirmPassword;
    @FXML private Label labelMessage;
    @FXML private Label labelAvatarChoisi;

    // Avatar sélectionné par l'utilisateur (🌿 par défaut)
    private String avatarChoisi = "🌿";

    // Adresse du serveur transmise depuis LoginController
    private String serveur = "localhost";

    public void setServeur(String serveur) {
        this.serveur = (serveur == null || serveur.isEmpty()) ? "localhost" : serveur;
    }

    // Appelé quand on clique sur un bouton emoji de la grille
    @FXML
    public void choisirAvatar(ActionEvent event) {
        Button btn = (Button) event.getSource();
        avatarChoisi = btn.getText();
        labelAvatarChoisi.setText(avatarChoisi);
    }

    // Quand on clique sur "S'inscrire"
    @FXML
    public void sInscrire() {
        String username = champUsername.getText().trim();
        String password = champPassword.getText();
        String confirmPassword = champConfirmPassword.getText();

        // Vérifications de base
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        // Les deux mots de passe doivent être identiques
        if (!password.equals(confirmPassword)) {
            afficherErreur("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            // On se connecte au serveur (avec l'adresse transmise depuis login)
            Client client = new Client();
            client.setHost(serveur);
            client.connecter();

            Packet paquet = new Packet(Packet.REGISTER, new String[]{username, password, avatarChoisi});
            client.envoyerPaquet(paquet);

            // On attend la réponse
            Packet reponse = client.recevoirPaquet();
            client.fermer();

            if (reponse.isSuccess()) {
                labelMessage.setStyle(
                    "-fx-text-fill: #2d6a4f; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 0 28;"
                );
                labelMessage.setText("✔ Inscription réussie ! Vous pouvez maintenant vous connecter.");
            } else {
                afficherErreur(reponse.getMessage());
            }

        } catch (Exception e) {
            afficherErreur("Impossible de se connecter au serveur. Est-il démarré ?");
        }
    }

    // Quand on clique sur "Retour" : on revient à l'écran de connexion
    @FXML
    public void retourConnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 420, 400);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // On réaffiche l'adresse du serveur dans le champ
            LoginController lc = loader.getController();
            lc.setServeurInitial(serveur);

            Stage stage = (Stage) champUsername.getScene().getWindow();
            stage.setTitle("Messagerie - Connexion");
            stage.setScene(scene);
        } catch (Exception e) {
            afficherErreur("Erreur de navigation.");
        }
    }

    private void afficherErreur(String message) {
        labelMessage.setStyle(
            "-fx-text-fill: #6b2737; -fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 0 28;"
        );
        labelMessage.setText(message);
    }
}
