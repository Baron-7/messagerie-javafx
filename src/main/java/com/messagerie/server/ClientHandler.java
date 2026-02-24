package com.messagerie.server;

import com.messagerie.dao.UserDAO;
import com.messagerie.model.Message;
import com.messagerie.model.Packet;
import com.messagerie.model.User;
import com.messagerie.service.AuthService;
import com.messagerie.service.MessageService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

// Gère la connexion avec un seul client dans un thread séparé (RG11)
public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user; // l'utilisateur connecté à ce handler

    // Référence partagée vers tous les clients connectés
    private Map<String, ClientHandler> clientsActifs;

    private AuthService authService = new AuthService();
    private MessageService messageService = new MessageService();
    private UserDAO userDAO = new UserDAO();
    private ServerLogger logger;

    public ClientHandler(Socket socket, Map<String, ClientHandler> clientsActifs, ServerLogger logger) {
        this.socket = socket;
        this.clientsActifs = clientsActifs;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            // On initialise les flux pour envoyer/recevoir des objets
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            // On lit les paquets envoyés par le client en boucle
            Packet paquet;
            while ((paquet = (Packet) in.readObject()) != null) {
                traiterPaquet(paquet);
            }

        } catch (Exception e) {
            // Si la connexion est perdue (RG10)
            System.out.println("Client déconnecté de façon inattendue.");
        } finally {
            // On déconnecte proprement dans tous les cas
            deconnecter();
        }
    }

    // Regarde le type du paquet et appelle la bonne méthode
    private void traiterPaquet(Packet paquet) {
        switch (paquet.getType()) {

            case Packet.LOGIN:
                // Les données sont [username, password]
                String[] loginData = (String[]) paquet.getData();
                gererLogin(loginData[0], loginData[1]);
                break;

            case Packet.REGISTER:
                // Les données sont [username, password]
                String[] registerData = (String[]) paquet.getData();
                gererRegister(registerData[0], registerData[1]);
                break;

            case Packet.LOGOUT:
                deconnecter();
                break;

            case Packet.SEND_MESSAGE:
                // Les données sont [username_destinataire, contenu_message]
                String[] msgData = (String[]) paquet.getData();
                gererEnvoiMessage(msgData[0], msgData[1]);
                break;

            case Packet.GET_HISTORY:
                // Les données sont le username du correspondant
                String correspondant = (String) paquet.getData();
                gererHistorique(correspondant);
                break;

            case Packet.GET_ONLINE_USERS:
                gererUtilisateursEnLigne();
                break;

            default:
                System.out.println("Type de paquet inconnu : " + paquet.getType());
        }
    }

    // Gère la connexion d'un utilisateur
    private void gererLogin(String username, String password) {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);

        try {
            User u = authService.login(username, password);
            this.user = u;

            // On ajoute ce client à la liste des clients actifs
            clientsActifs.put(username, this);
            logger.logConnexion(username);

            // On livre les messages en attente (RG6)
            List<Message> messagesEnAttente = messageService.deliverPending(u);
            for (Message m : messagesEnAttente) {
                envoyerAuClient(new Packet(Packet.NEW_MESSAGE, m));
            }

            reponse.setSuccess(true);
            reponse.setMessage("Connexion réussie !");
            reponse.setData(u);

        } catch (Exception e) {
            reponse.setSuccess(false);
            reponse.setMessage(e.getMessage());
        }

        envoyerAuClient(reponse);
    }

    // Gère l'inscription d'un utilisateur
    private void gererRegister(String username, String password) {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);

        try {
            User u = authService.register(username, password);
            reponse.setSuccess(true);
            reponse.setMessage("Inscription réussie ! Vous pouvez vous connecter.");
            reponse.setData(u);

        } catch (Exception e) {
            reponse.setSuccess(false);
            reponse.setMessage(e.getMessage());
        }

        envoyerAuClient(reponse);
    }

    // Gère l'envoi d'un message vers un autre utilisateur
    private void gererEnvoiMessage(String usernameDestinataire, String contenu) {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);

        try {
            Message message = messageService.sendMessage(user, usernameDestinataire, contenu);
            logger.logMessage(user.getUsername(), usernameDestinataire);

            // Si le destinataire est connecté, on lui envoie le message directement
            ClientHandler destinataireHandler = clientsActifs.get(usernameDestinataire);
            if (destinataireHandler != null) {
                destinataireHandler.envoyerAuClient(new Packet(Packet.NEW_MESSAGE, message));
            }
            // Sinon le message est déjà sauvegardé en base, il sera livré à la prochaine connexion (RG6)

            reponse.setSuccess(true);
            reponse.setMessage("Message envoyé.");
            reponse.setData(message);

        } catch (Exception e) {
            reponse.setSuccess(false);
            reponse.setMessage(e.getMessage());
        }

        envoyerAuClient(reponse);
    }

    // Gère la récupération de l'historique d'une conversation
    private void gererHistorique(String usernameCorrespondant) {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);

        try {
            User correspondant = userDAO.findByUsername(usernameCorrespondant);
            if (correspondant == null) {
                throw new Exception("Utilisateur introuvable.");
            }
            List<Message> historique = messageService.getHistory(user, correspondant);
            reponse.setSuccess(true);
            reponse.setData(historique);

        } catch (Exception e) {
            reponse.setSuccess(false);
            reponse.setMessage(e.getMessage());
        }

        envoyerAuClient(reponse);
    }

    // Gère la liste des utilisateurs connectés
    private void gererUtilisateursEnLigne() {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);
        reponse.setSuccess(true);
        // On retourne la liste des noms des clients connectés
        reponse.setData(clientsActifs.keySet().toArray(new String[0]));
        envoyerAuClient(reponse);
    }

    // Envoie un paquet au client
    public void envoyerAuClient(Packet paquet) {
        try {
            out.writeObject(paquet);
            out.flush();
        } catch (Exception e) {
            System.out.println("Impossible d'envoyer au client : " + e.getMessage());
        }
    }

    // Déconnecte ce client proprement
    public void deconnecter() {
        if (user != null) {
            authService.logout(user);
            clientsActifs.remove(user.getUsername());
            logger.logDeconnexion(user.getUsername());
            user = null;
        }
        try {
            socket.close();
        } catch (Exception e) {
            // on ignore
        }
    }

    // Retourne l'utilisateur de ce handler
    public User getUser() {
        return user;
    }
}
