package com.messagerie.controller;

import com.messagerie.client.Client;
import com.messagerie.client.MessageListener;
import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.Packet;
import com.messagerie.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class ChatController {

    @FXML private ListView<User> listeUtilisateurs;
    @FXML private Label labelInterlocuteur;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox zoneMessages;
    @FXML private TextField champSaisie;

    private Client client;
    private String interlocuteurActuel;

    private static final DateTimeFormatter HEURE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Initialisation automatique par JavaFX : configure la cellule de la liste
    @FXML
    public void initialize() {
        listeUtilisateurs.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(user.getAvatar() + "  " + user.getUsername());
                    setStyle(
                        "-fx-font-size: 13; -fx-text-fill: #1b4332; -fx-padding: 8 12;" +
                        "-fx-background-color: transparent;"
                    );
                }
            }
        });
    }

    // Appelé par LoginController après la connexion pour passer le client
    public void setClient(Client client) {
        this.client = client;

        // Le listener démarre EN PREMIER : c'est le seul à lire le socket
        Thread listenerThread = new Thread(new MessageListener(client, this));
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Charger les utilisateurs dans un thread séparé (utilise la file)
        new Thread(this::chargerUtilisateursEnLigne).start();
    }

    // Crée une bulle de message style nature
    private HBox creerBulleMessage(Message message, boolean estMoi) {
        VBox bulle = new VBox(2);
        bulle.setMaxWidth(320);

        // Avatar + nom de l'expéditeur (seulement pour les messages reçus)
        if (!estMoi) {
            String avatar = message.getSender().getAvatar();
            Label nomLabel = new Label(avatar + " " + message.getSender().getUsername());
            nomLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #2d6a4f;");
            bulle.getChildren().add(nomLabel);
        }

        // Contenu du message
        Label contenuLabel = new Label(message.getContenu());
        contenuLabel.setWrapText(true);
        contenuLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #1b4332;");
        bulle.getChildren().add(contenuLabel);

        // Pied de bulle : heure + statut
        HBox pied = new HBox(4);
        pied.setAlignment(Pos.CENTER_RIGHT);

        // Heure d'envoi
        if (message.getDateEnvoi() != null) {
            Label heureLabel = new Label(message.getDateEnvoi().format(HEURE_FORMAT));
            heureLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #52b788;");
            pied.getChildren().add(heureLabel);
        }

        // Traits de statut uniquement sur mes messages
        if (estMoi) {
            Label statutLabel = new Label(getSymboleStatut(message.getStatut()));
            statutLabel.setStyle(getStyleStatut(message.getStatut()));
            pied.getChildren().add(statutLabel);
        }

        bulle.getChildren().add(pied);

        // Style de la bulle : vert naturel (envoyé) ou sage clair (reçu)
        if (estMoi) {
            bulle.setStyle(
                "-fx-background-color: #b7e4c7;" +
                "-fx-background-radius: 14 2 14 14;" +
                "-fx-padding: 9 13;" +
                "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.15), 4, 0, 0, 2);"
            );
        } else {
            bulle.setStyle(
                "-fx-background-color: #f0f7eb;" +
                "-fx-background-radius: 2 14 14 14;" +
                "-fx-padding: 9 13;" +
                "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.12), 4, 0, 0, 2);"
            );
        }

        // Conteneur externe pour aligner la bulle à droite ou à gauche
        HBox conteneur = new HBox();
        HBox.setMargin(bulle, new Insets(2, 8, 2, 8));
        conteneur.setAlignment(estMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        conteneur.getChildren().add(bulle);
        return conteneur;
    }

    // ✓ pour ENVOYE, ✓✓ pour RECU ou LU
    private String getSymboleStatut(MessageStatus statut) {
        if (statut == MessageStatus.RECU || statut == MessageStatus.LU) return "✓✓";
        return "✓";
    }

    // Vert forêt pour LU, vert grisé pour ENVOYE et RECU
    private String getStyleStatut(MessageStatus statut) {
        if (statut == MessageStatus.LU) {
            return "-fx-font-size: 11; -fx-text-fill: #2d6a4f; -fx-font-weight: bold;";
        }
        return "-fx-font-size: 11; -fx-text-fill: #74c69d;";
    }

    private void scrollVersLeBas() {
        scrollPane.setVvalue(1.0);
    }

    // Affiche un message reçu dans la conversation active
    // Appelé par MessageListener (depuis un autre thread)
    public void afficherMessage(Message message) {
        Platform.runLater(() -> {
            if (interlocuteurActuel != null &&
                    message.getSender().getUsername().equals(interlocuteurActuel)) {
                zoneMessages.getChildren().add(creerBulleMessage(message, false));
                scrollVersLeBas();
            }
        });
    }

    // Affiche un message d'erreur si la connexion est perdue (RG10)
    public void afficherErreurConnexion() {
        Platform.runLater(() -> {
            Label erreur = new Label("🍂 Connexion au serveur perdue");
            erreur.setStyle("-fx-text-fill: #6b2737; -fx-font-size: 12; -fx-padding: 6 12;" +
                    "-fx-background-color: #fde8ec; -fx-background-radius: 8;");
            HBox ligne = new HBox(erreur);
            ligne.setAlignment(Pos.CENTER);
            zoneMessages.getChildren().add(ligne);
        });
    }

    // Quand on clique sur "Envoyer"
    @FXML
    public void envoyerMessage() {
        String contenu = champSaisie.getText().trim();
        User destinataireUser = listeUtilisateurs.getSelectionModel().getSelectedItem();

        if (contenu.isEmpty() || destinataireUser == null) return;

        try {
            String destinataire = destinataireUser.getUsername();
            Packet paquet = new Packet(Packet.SEND_MESSAGE, new String[]{destinataire, contenu});
            client.envoyerPaquet(paquet);

            // Affichage immédiat de ma bulle avec statut ENVOYE (✓)
            Message msgTemp = new Message(client.getCurrentUser(), null, contenu);
            zoneMessages.getChildren().add(creerBulleMessage(msgTemp, true));
            champSaisie.clear();
            scrollVersLeBas();
        } catch (Exception e) {
            Label erreur = new Label("[Erreur d'envoi]");
            erreur.setStyle("-fx-text-fill: #6b2737;");
            HBox ligne = new HBox(erreur);
            ligne.setAlignment(Pos.CENTER);
            zoneMessages.getChildren().add(ligne);
        }
    }

    // Quand on sélectionne un utilisateur dans la liste
    @FXML
    public void selectionnerUtilisateur() {
        User utilisateurChoisi = listeUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurChoisi != null) {
            interlocuteurActuel = utilisateurChoisi.getUsername();
            labelInterlocuteur.setText(utilisateurChoisi.getAvatar() + "  " + utilisateurChoisi.getUsername());
            chargerHistorique(utilisateurChoisi.getUsername());
        }
    }

    // Bouton Rafraîchir : recharge la liste des utilisateurs en ligne
    @FXML
    public void rafraichirUtilisateurs() {
        new Thread(this::chargerUtilisateursEnLigne).start();
    }

    // Bouton Déconnexion : envoie LOGOUT au serveur et retourne à l'écran de connexion
    @FXML
    public void seDeconnecter() {
        try {
            client.envoyerPaquet(new Packet(Packet.LOGOUT, null));
            client.fermer();
        } catch (Exception e) {
            // On ferme quand même même si l'envoi échoue
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 420, 340);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) champSaisie.getScene().getWindow();
            stage.setTitle("Messagerie - Connexion");
            stage.setScene(scene);
        } catch (Exception e) {
            System.out.println("Erreur lors de la déconnexion : " + e.getMessage());
        }
    }

    // Charge la liste des utilisateurs en ligne avec leurs avatars
    private void chargerUtilisateursEnLigne() {
        try {
            client.envoyerPaquet(new Packet(Packet.GET_ONLINE_USERS, null));
            Packet reponse = client.attendreReponse();
            if (reponse.isSuccess()) {
                User[] utilisateurs = (User[]) reponse.getData();
                Platform.runLater(() -> {
                    listeUtilisateurs.getItems().clear();
                    for (User u : utilisateurs) {
                        if (!u.getUsername().equals(client.getCurrentUser().getUsername())) {
                            listeUtilisateurs.getItems().add(u);
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Impossible de charger les utilisateurs en ligne.");
        }
    }

    // Charge l'historique de conversation avec un utilisateur
    private void chargerHistorique(String username) {
        new Thread(() -> {
            try {
                client.envoyerPaquet(new Packet(Packet.GET_HISTORY, username));
                Packet reponse = client.attendreReponse();
                if (reponse.isSuccess()) {
                    java.util.List<Message> historique = (java.util.List<Message>) reponse.getData();
                    Platform.runLater(() -> {
                        zoneMessages.getChildren().clear();
                        String moi = client.getCurrentUser().getUsername();
                        for (Message m : historique) {
                            boolean estMoi = m.getSender().getUsername().equals(moi);
                            zoneMessages.getChildren().add(creerBulleMessage(m, estMoi));
                        }
                        scrollVersLeBas();
                    });
                }
            } catch (Exception e) {
                System.out.println("Impossible de charger l'historique.");
            }
        }).start();
    }
}
