package com.messagerie.service;

import com.messagerie.dao.MessageDAO;
import com.messagerie.dao.UserDAO;
import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.User;
import com.messagerie.model.UserStatus;

import java.util.List;

// TODO (Personne B) : service qui gère l'envoi et la réception des messages
public class MessageService {

    private MessageDAO messageDAO = new MessageDAO();
    private UserDAO userDAO = new UserDAO();

    // Envoie un message d'un utilisateur à un autre
    public Message sendMessage(User sender, String usernameDestinataire, String contenu) throws Exception {

        // L'expéditeur doit être connecté (RG5)
        if (sender.getStatus() != UserStatus.ONLINE) {
            throw new Exception("Vous devez être connecté pour envoyer un message.");
        }

        // Le destinataire doit exister (RG5)
        User receiver = userDAO.findByUsername(usernameDestinataire);
        if (receiver == null) {
            throw new Exception("Destinataire introuvable.");
        }

        // Le contenu ne doit pas être vide (RG7)
        if (contenu == null || contenu.trim().isEmpty()) {
            throw new Exception("Le message ne peut pas être vide.");
        }

        // Le contenu ne doit pas dépasser 1000 caractères (RG7)
        if (contenu.length() > 1000) {
            throw new Exception("Le message ne peut pas dépasser 1000 caractères.");
        }

        // On crée et sauvegarde le message
        Message message = new Message(sender, receiver, contenu);
        messageDAO.save(message);

        return message;
    }

    // Retourne l'historique des messages entre deux utilisateurs (RG8)
    public List<Message> getHistory(User u1, User u2) {
        return messageDAO.findConversation(u1, u2);
    }

    // Livre les messages en attente à un utilisateur qui vient de se connecter (RG6)
    public List<Message> deliverPending(User user) {
        List<Message> messagesEnAttente = messageDAO.findPendingMessages(user);

        // On marque chaque message comme RECU
        for (Message m : messagesEnAttente) {
            messageDAO.updateStatus(m.getId(), MessageStatus.RECU);
            m.setStatut(MessageStatus.RECU);
        }

        return messagesEnAttente;
    }
}
