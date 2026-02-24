package com.messagerie.controller;

import com.messagerie.client.Client;
import com.messagerie.client.MessageListener;
import com.messagerie.model.Message;
import com.messagerie.model.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

// TODO (Personne B) : contrôleur de l'écran de chat principal
public class ChatController {

    @FXML private ListView<String> listeUtilisateurs;
    @FXML private TextArea zoneConversation;
    @FXML private TextField champSaisie;

    private Client client;

    // Appelé par LoginController après la connexion pour passer le client
    public void setClient(Client client) {
        this.client = client;

        // On lance le listener dans un thread séparé pour recevoir les messages
        Thread listenerThread = new Thread(new MessageListener(client, this));
        listenerThread.setDaemon(true); // le thread s'arrête si l'appli se ferme
        listenerThread.start();

        // TODO (Personne B) : charger la liste des utilisateurs en ligne
        chargerUtilisateursEnLigne();
    }

    // Affiche un message reçu dans la zone de conversation
    // Appelé par MessageListener (depuis un autre thread, donc on utilise Platform.runLater)
    public void afficherMessage(Message message) {
        Platform.runLater(() -> {
            // TODO (Personne B) : afficher le message dans la bonne conversation
            String ligne = message.getSender().getUsername() + " : " + message.getContenu();
            zoneConversation.appendText(ligne + "\n");
        });
    }

    // Affiche un message d'erreur si la connexion est perdue (RG10)
    public void afficherErreurConnexion() {
        Platform.runLater(() -> {
            zoneConversation.appendText("[Connexion au serveur perdue]\n");
        });
    }

    // Quand on clique sur "Envoyer"
    @FXML
    public void envoyerMessage() {
        // TODO (Personne B) : envoyer le message au serveur
        String contenu = champSaisie.getText().trim();
        String destinataire = listeUtilisateurs.getSelectionModel().getSelectedItem();

        if (contenu.isEmpty() || destinataire == null) {
            return;
        }

        try {
            Packet paquet = new Packet(Packet.SEND_MESSAGE, new String[]{destinataire, contenu});
            client.envoyerPaquet(paquet);
            // On affiche notre propre message dans la zone de conversation
            zoneConversation.appendText("Moi : " + contenu + "\n");
            champSaisie.clear();
        } catch (Exception e) {
            zoneConversation.appendText("[Erreur d'envoi]\n");
        }
    }

    // Quand on sélectionne un utilisateur dans la liste
    @FXML
    public void selectionnerUtilisateur() {
        // TODO (Personne B) : charger l'historique de conversation avec cet utilisateur
        String utilisateurChoisi = listeUtilisateurs.getSelectionModel().getSelectedItem();
        if (utilisateurChoisi != null) {
            chargerHistorique(utilisateurChoisi);
        }
    }

    // Charge la liste des utilisateurs en ligne
    private void chargerUtilisateursEnLigne() {
        try {
            Packet paquet = new Packet(Packet.GET_ONLINE_USERS, null);
            client.envoyerPaquet(paquet);
            Packet reponse = client.recevoirPaquet();
            if (reponse.isSuccess()) {
                String[] utilisateurs = (String[]) reponse.getData();
                listeUtilisateurs.getItems().clear();
                for (String u : utilisateurs) {
                    // On n'affiche pas soi-même dans la liste
                    if (!u.equals(client.getCurrentUser().getUsername())) {
                        listeUtilisateurs.getItems().add(u);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Impossible de charger les utilisateurs en ligne.");
        }
    }

    // Charge l'historique de conversation avec un utilisateur
    private void chargerHistorique(String username) {
        try {
            Packet paquet = new Packet(Packet.GET_HISTORY, username);
            client.envoyerPaquet(paquet);
            Packet reponse = client.recevoirPaquet();
            if (reponse.isSuccess()) {
                java.util.List<Message> historique = (java.util.List<Message>) reponse.getData();
                zoneConversation.clear();
                for (Message m : historique) {
                    String ligne = m.getSender().getUsername() + " : " + m.getContenu();
                    zoneConversation.appendText(ligne + "\n");
                }
            }
        } catch (Exception e) {
            System.out.println("Impossible de charger l'historique.");
        }
    }
}
