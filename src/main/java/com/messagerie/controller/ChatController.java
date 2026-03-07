package com.messagerie.controller;

import com.messagerie.client.Client;
import com.messagerie.client.MessageListener;
import com.messagerie.model.FilePayload;
import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.Packet;
import com.messagerie.model.User;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ChatController {

    @FXML private BorderPane panneauPrincipal;
    @FXML private VBox panneauGauche;
    @FXML private HBox enteteGauche;
    @FXML private VBox zoneConversation;
    @FXML private HBox enteteConversation;
    @FXML private HBox zoneSaisie;
    @FXML private ListView<User> listeUtilisateurs;
    @FXML private Label labelInterlocuteur;
    @FXML private Label labelFrappe;
    @FXML private Button boutonTheme;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox zoneMessages;
    @FXML private TextField champSaisie;

    private Client client;
    private String interlocuteurActuel;
    private boolean modeSombre = false;

    // Debounce : arrête l'indicateur 2 s après la dernière frappe
    private final PauseTransition pauseFrappe = new PauseTransition(Duration.seconds(2));

    // Associe chaque ID de message à son label de statut (✓ / ✓✓) pour la mise à jour en bleu
    private final Map<Long, Label> mapStatutLabels = new HashMap<>();

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

        // Indicateur de frappe : envoie "typing=true" dès qu'on tape,
        // puis "typing=false" après 2 s d'inactivité
        pauseFrappe.setOnFinished(e -> envoyerIndicateurFrappe(false));
        champSaisie.textProperty().addListener((obs, oldVal, newVal) -> {
            if (interlocuteurActuel == null || client == null) return;
            if (!newVal.isEmpty()) {
                envoyerIndicateurFrappe(true);
                pauseFrappe.playFromStart();
            } else {
                pauseFrappe.stop();
                envoyerIndicateurFrappe(false);
            }
        });
    }

    // --- Indicateur de frappe ---

    // Envoie au serveur l'état de frappe (true = en train d'écrire)
    private void envoyerIndicateurFrappe(boolean typing) {
        if (interlocuteurActuel == null || client == null) return;
        try {
            client.envoyerPaquet(new Packet(Packet.TYPING,
                    new String[]{interlocuteurActuel, String.valueOf(typing)}));
        } catch (Exception e) {
            // ignore — non critique
        }
    }

    // Affiche ou masque le label "X est en train d'écrire..." (appelé par MessageListener)
    public void afficherIndicateurFrappe(String username, boolean typing) {
        if (username.equals(interlocuteurActuel)) {
            if (typing) labelFrappe.setText(username + " est en train d'écrire...");
            labelFrappe.setVisible(typing);
        }
    }

    // --- Thème clair / sombre ---

    @FXML
    public void toggleTheme() {
        modeSombre = !modeSombre;
        appliquerTheme();
    }

    private void appliquerTheme() {
        if (modeSombre) {
            panneauPrincipal.setStyle("-fx-background-color: #0a0f0d;");
            panneauGauche.setStyle("-fx-background-color: #141f18; -fx-border-color: #1e3a2a; -fx-border-width: 0 2 0 0;");
            enteteGauche.setStyle("-fx-background-color: #0d2218; -fx-padding: 16 14; -fx-alignment: center-left; -fx-spacing: 8;");
            zoneConversation.setStyle("-fx-background-color: #111a14;");
            enteteConversation.setStyle("-fx-background-color: #0d2218; -fx-padding: 12 18; -fx-alignment: center-left; -fx-spacing: 10;");
            zoneSaisie.setStyle("-fx-padding: 10 14; -fx-background-color: #0d2218; -fx-alignment: center-left; -fx-border-color: #1e3a2a; -fx-border-width: 1 0 0 0;");
            champSaisie.setStyle("-fx-background-radius: 22; -fx-padding: 10 16; -fx-font-size: 13;" +
                    "-fx-background-color: #1a3320; -fx-border-color: #2d6a4f; -fx-border-radius: 22;" +
                    "-fx-border-width: 1.5; -fx-text-fill: #d8f3dc; -fx-prompt-text-fill: #52b788;");
            labelFrappe.setStyle("-fx-font-size: 11; -fx-text-fill: #52b788; -fx-padding: 0 14 2 14; -fx-font-style: italic;");
            boutonTheme.setText("☀");
        } else {
            panneauPrincipal.setStyle("-fx-background-color: #1b4332;");
            panneauGauche.setStyle("-fx-background-color: #f0f7eb; -fx-border-color: #a8d5a2; -fx-border-width: 0 2 0 0;");
            enteteGauche.setStyle("-fx-background-color: #1b4332; -fx-padding: 16 14; -fx-alignment: center-left; -fx-spacing: 8;");
            zoneConversation.setStyle("-fx-background-color: #e9f5e1;");
            enteteConversation.setStyle("-fx-background-color: #2d6a4f; -fx-padding: 12 18; -fx-alignment: center-left; -fx-spacing: 10;");
            zoneSaisie.setStyle("-fx-padding: 10 14; -fx-background-color: #c8e6c0; -fx-alignment: center-left; -fx-border-color: #a8d5a2; -fx-border-width: 1 0 0 0;");
            champSaisie.setStyle("-fx-background-radius: 22; -fx-padding: 10 16; -fx-font-size: 13;" +
                    "-fx-background-color: #f0f7eb; -fx-border-color: #74c69d; -fx-border-radius: 22;" +
                    "-fx-border-width: 1.5; -fx-text-fill: #1b4332; -fx-prompt-text-fill: #74c69d;");
            labelFrappe.setStyle("-fx-font-size: 11; -fx-text-fill: #52b788; -fx-padding: 0 14 2 14; -fx-font-style: italic;");
            boutonTheme.setText("🌙");
        }
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
            // Mémoriser pour mise à jour en bleu si le message est lu plus tard
            if (message.getId() != null) {
                mapStatutLabels.put(message.getId(), statutLabel);
            }
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

        // Stocker l'ID du message pour pouvoir le supprimer plus tard
        conteneur.setUserData(message.getId());

        // Menu contextuel "Supprimer" uniquement sur mes messages qui ont un ID
        if (estMoi && message.getId() != null) {
            ContextMenu menu = new ContextMenu();
            MenuItem itemSupprimer = new MenuItem("Supprimer");
            itemSupprimer.setOnAction(e -> {
                try {
                    client.envoyerPaquet(new Packet(Packet.DELETE_MESSAGE, message.getId()));
                } catch (Exception ex) {
                    System.out.println("Erreur lors de la suppression : " + ex.getMessage());
                }
            });
            menu.getItems().add(itemSupprimer);
            bulle.setOnContextMenuRequested(e -> menu.show(bulle, e.getScreenX(), e.getScreenY()));
        }

        return conteneur;
    }

    // ✓ pour ENVOYE, ✓✓ pour RECU ou LU
    private String getSymboleStatut(MessageStatus statut) {
        if (statut == MessageStatus.RECU || statut == MessageStatus.LU) return "✓✓";
        return "✓";
    }

    // Bleu pour LU (✓✓ lus), vert clair pour ENVOYE/RECU
    private String getStyleStatut(MessageStatus statut) {
        if (statut == MessageStatus.LU) {
            return "-fx-font-size: 11; -fx-text-fill: #1a7cde; -fx-font-weight: bold;";
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

    // Passe tous les ✓✓ en bleu quand le destinataire a lu les messages (appelé par MessageListener)
    public void marquerMessagesLus(String readerUsername) {
        if (readerUsername.equals(interlocuteurActuel)) {
            String styleLu = "-fx-font-size: 11; -fx-text-fill: #1a7cde; -fx-font-weight: bold;";
            for (Label label : mapStatutLabels.values()) {
                label.setText("✓✓");
                label.setStyle(styleLu);
            }
        }
    }

    // Retire la bulle correspondant au message supprimé (appelé par MessageListener)
    public void supprimerBulle(Long messageId) {
        Platform.runLater(() ->
            zoneMessages.getChildren().removeIf(node -> messageId.equals(node.getUserData()))
        );
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

    // Ouvre un sélecteur de fichier et envoie le fichier choisi
    @FXML
    public void choisirFichier() {
        User destinataireUser = listeUtilisateurs.getSelectionModel().getSelectedItem();
        if (destinataireUser == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à envoyer");
        File fichier = fileChooser.showOpenDialog(champSaisie.getScene().getWindow());
        if (fichier == null) return;

        // Limite à 10 Mo
        if (fichier.length() > 10 * 1024 * 1024) {
            Label erreur = new Label("[Fichier trop volumineux : max 10 Mo]");
            erreur.setStyle("-fx-text-fill: #6b2737;");
            HBox ligne = new HBox(erreur);
            ligne.setAlignment(Pos.CENTER);
            zoneMessages.getChildren().add(ligne);
            return;
        }

        try {
            byte[] data = Files.readAllBytes(fichier.toPath());
            String receiver = destinataireUser.getUsername();
            FilePayload payload = new FilePayload(fichier.getName(), data,
                    client.getCurrentUser().getUsername(), receiver);

            client.envoyerPaquet(new Packet(Packet.SEND_FILE, payload));

            // Affichage immédiat côté expéditeur
            zoneMessages.getChildren().add(creerBulleFichier(payload, true));
            scrollVersLeBas();

        } catch (IOException e) {
            Label erreur = new Label("[Erreur de lecture du fichier]");
            erreur.setStyle("-fx-text-fill: #6b2737;");
            HBox ligne = new HBox(erreur);
            ligne.setAlignment(Pos.CENTER);
            zoneMessages.getChildren().add(ligne);
        } catch (Exception e) {
            Label erreur = new Label("[Erreur d'envoi]");
            erreur.setStyle("-fx-text-fill: #6b2737;");
            HBox ligne = new HBox(erreur);
            ligne.setAlignment(Pos.CENTER);
            zoneMessages.getChildren().add(ligne);
        }
    }

    // Crée une bulle pour un fichier (envoyé ou reçu)
    private HBox creerBulleFichier(FilePayload fichier, boolean estMoi) {
        VBox bulle = new VBox(6);
        bulle.setMaxWidth(280);

        if (!estMoi) {
            Label nomLabel = new Label(fichier.getSenderUsername());
            nomLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #2d6a4f;");
            bulle.getChildren().add(nomLabel);
        }

        // Icône + nom du fichier
        String taille = String.format("%.1f Ko", fichier.getData().length / 1024.0);
        Label infoLabel = new Label("  " + fichier.getFilename() + "\n" + taille);
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #1b4332;");
        bulle.getChildren().add(infoLabel);

        // Bouton Télécharger
        Button btnTelecharger = new Button("Télécharger");
        btnTelecharger.setStyle(
            "-fx-background-color: #40916c; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11;"
        );
        btnTelecharger.setOnAction(e -> telechargerFichier(fichier));
        bulle.getChildren().add(btnTelecharger);

        bulle.setStyle(
            "-fx-background-color: " + (estMoi ? "#b7e4c7" : "#f0f7eb") + ";" +
            "-fx-background-radius: 12; -fx-padding: 9 13;" +
            "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.15), 4, 0, 0, 2);"
        );

        HBox conteneur = new HBox();
        HBox.setMargin(bulle, new Insets(2, 8, 2, 8));
        conteneur.setAlignment(estMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        conteneur.getChildren().add(bulle);
        return conteneur;
    }

    // Ouvre un FileChooser "Enregistrer sous" et écrit le fichier sur disque
    private void telechargerFichier(FilePayload fichier) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier");
        fileChooser.setInitialFileName(fichier.getFilename());
        File destination = fileChooser.showSaveDialog(champSaisie.getScene().getWindow());
        if (destination == null) return;
        try {
            Files.write(destination.toPath(), fichier.getData());
        } catch (IOException e) {
            System.out.println("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    // Affiche un fichier reçu (appelé par MessageListener)
    public void afficherFichierRecu(FilePayload fichier) {
        Platform.runLater(() -> {
            if (interlocuteurActuel != null &&
                    fichier.getSenderUsername().equals(interlocuteurActuel)) {
                zoneMessages.getChildren().add(creerBulleFichier(fichier, false));
                scrollVersLeBas();
            }
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
            // Stopper l'indicateur de frappe
            pauseFrappe.stop();
            envoyerIndicateurFrappe(false);
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

    // Met à jour la liste des contacts en ligne (appelé par MessageListener via USERS_UPDATED)
    public void mettreAJourListeUtilisateurs(User[] utilisateurs) {
        String moi = client.getCurrentUser().getUsername();
        listeUtilisateurs.getItems().clear();
        for (User u : utilisateurs) {
            if (!u.getUsername().equals(moi)) {
                listeUtilisateurs.getItems().add(u);
            }
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
                    // Signaler au serveur que les messages de cet interlocuteur sont lus
                    client.envoyerPaquet(new Packet(Packet.MARK_READ, username));
                    Platform.runLater(() -> {
                        zoneMessages.getChildren().clear();
                        mapStatutLabels.clear();
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
