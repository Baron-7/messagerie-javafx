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

                // Si c'est un nouveau message, on le donne au ChatController
                if (Packet.NEW_MESSAGE.equals(paquet.getType())) {
                    Message message = (Message) paquet.getData();
                    // IMPORTANT : on utilise Platform.runLater() car on est dans un autre thread
                    // TODO (Personne B) : appeler chatController.afficherMessage(message)
                    javafx.application.Platform.runLater(() -> {
                        chatController.afficherMessage(message);
                    });
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
