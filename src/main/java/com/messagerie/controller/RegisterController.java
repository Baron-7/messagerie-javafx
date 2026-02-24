package com.messagerie.controller;

import com.messagerie.client.Client;
import com.messagerie.model.Packet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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

    // Quand on clique sur "S'inscrire"
    @FXML
    public void sInscrire() {
        String username = champUsername.getText().trim();
        String password = champPassword.getText();
        String confirmPassword = champConfirmPassword.getText();

        // Vérifications de base
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            labelMessage.setText("Veuillez remplir tous les champs.");
            return;
        }

        // Les deux mots de passe doivent être identiques
        if (!password.equals(confirmPassword)) {
            labelMessage.setText("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            // On se connecte au serveur et on envoie la demande d'inscription
            Client client = new Client();
            client.connecter();

            Packet paquet = new Packet(Packet.REGISTER, new String[]{username, password});
            client.envoyerPaquet(paquet);

            // On attend la réponse
            Packet reponse = client.recevoirPaquet();
            client.fermer(); // on ferme la connexion après l'inscription

            if (reponse.isSuccess()) {
                // Inscription réussie : on affiche un message vert et on revient à la connexion
                labelMessage.setStyle("-fx-text-fill: green; -fx-font-size: 13; -fx-font-weight: bold;");
                labelMessage.setText("Inscription réussie ! Vous pouvez maintenant vous connecter.");
            } else {
                labelMessage.setStyle("-fx-text-fill: red; -fx-font-size: 13; -fx-font-weight: bold;");
                labelMessage.setText(reponse.getMessage());
            }

        } catch (Exception e) {
            labelMessage.setStyle("-fx-text-fill: red;");
            labelMessage.setText("Impossible de se connecter au serveur. Est-il démarré ?");
        }
    }

    // Quand on clique sur "Retour" : on revient à l'écran de connexion
    @FXML
    public void retourConnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) champUsername.getScene().getWindow();
            stage.setTitle("Messagerie - Connexion");
            stage.setScene(scene);
        } catch (Exception e) {
            labelMessage.setText("Erreur de navigation.");
        }
    }
}
