package com.messagerie.client;

import com.messagerie.controller.ChatController;
import com.messagerie.model.FilePayload;
import com.messagerie.model.Message;
import com.messagerie.model.Packet;
import com.messagerie.model.User;

// TODO (Personne B) : écoute les messages venant du serveur dans un thread séparé
// Quand un message arrive, il notifie le ChatController pour l'afficher
public class MessageListener implements Runnable {

    private Client client;
    private ChatController chatController;

    public MessageListener(Client client, ChatController chatController) {
        this.client = client;
        this.chatController = chatController;
    }

    @Override
    public void run() {
        // On écoute en boucle les paquets venant du serveur
        try {
            Packet paquet;
            while ((paquet = client.recevoirPaquet()) != null) {

                if (Packet.NEW_MESSAGE.equals(paquet.getType())) {
                    // Nouveau message entrant : on l'affiche dans le chat
                    Message message = (Message) paquet.getData();
                    javafx.application.Platform.runLater(() -> {
                        chatController.afficherMessage(message);
                    });
                } else if (Packet.MESSAGE_DELETED.equals(paquet.getType())) {
                    // Message supprimé : on retire la bulle de l'interface
                    Long messageId = (Long) paquet.getData();
                    javafx.application.Platform.runLater(() -> {
                        chatController.supprimerBulle(messageId);
                    });
                } else if (Packet.NEW_FILE.equals(paquet.getType())) {
                    // Fichier reçu : on l'affiche dans le chat
                    FilePayload fichier = (FilePayload) paquet.getData();
                    javafx.application.Platform.runLater(() -> {
                        chatController.afficherFichierRecu(fichier);
                    });
                } else if (Packet.USERS_UPDATED.equals(paquet.getType())) {
                    // Liste des connectés mise à jour : rafraîchir la sidebar
                    final User[] users = (User[]) paquet.getData();
                    javafx.application.Platform.runLater(() ->
                        chatController.mettreAJourListeUtilisateurs(users));
                } else if (Packet.MESSAGE_READ.equals(paquet.getType())) {
                    // Le destinataire a lu nos messages : passer les ✓✓ en bleu
                    final String readerUsername = (String) paquet.getData();
                    javafx.application.Platform.runLater(() ->
                        chatController.marquerMessagesLus(readerUsername));
                } else if (Packet.TYPING.equals(paquet.getType())) {
                    // Indicateur de frappe
                    String[] typingData = (String[]) paquet.getData();
                    final String senderUsername = typingData[0];
                    final boolean typing = Boolean.parseBoolean(typingData[1]);
                    javafx.application.Platform.runLater(() ->
                        chatController.afficherIndicateurFrappe(senderUsername, typing));
                } else {
                    // Réponse à une requête (GET_ONLINE_USERS, GET_HISTORY...)
                    // On la dépose dans la file pour que ChatController la lise
                    client.mettreEnFile(paquet);
                }
            }
        } catch (Exception e) {
            // Si la connexion est perdue (RG10)
            System.out.println("Connexion au serveur perdue.");
            javafx.application.Platform.runLater(() -> {
                chatController.afficherErreurConnexion();
            });
        }
    }
}
