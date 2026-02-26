package com.messagerie.client;

import com.messagerie.controller.ChatController;
import com.messagerie.model.Message;
import com.messagerie.model.Packet;

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
