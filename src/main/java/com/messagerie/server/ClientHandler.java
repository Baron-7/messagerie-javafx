package com.messagerie.server;

import com.messagerie.dao.UserDAO;
import com.messagerie.model.ConversationInfo;
import com.messagerie.model.FilePayload;
import com.messagerie.model.Message;
import com.messagerie.model.Packet;
import com.messagerie.model.User;
import com.messagerie.service.AuthService;
import com.messagerie.service.MessageService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                // Les données sont [username, password, avatar]
                String[] registerData = (String[]) paquet.getData();
                String avatarReg = registerData.length > 2 ? registerData[2] : "🌿";
                gererRegister(registerData[0], registerData[1], avatarReg);
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

            case Packet.DELETE_MESSAGE:
                Long messageId = (Long) paquet.getData();
                gererSuppressionMessage(messageId);
                break;

            case Packet.SEND_FILE:
                FilePayload fichier = (FilePayload) paquet.getData();
                gererEnvoiFichier(fichier);
                break;

            case Packet.TYPING:
                String[] typingData = (String[]) paquet.getData();
                gererIndicateurFrappe(typingData[0], Boolean.parseBoolean(typingData[1]));
                break;

            case Packet.MARK_READ:
                String senderUsername = (String) paquet.getData();
                gererMarquageLu(senderUsername);
                break;

            case Packet.GET_CONVERSATIONS:
                gererConversations();
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

            reponse.setSuccess(true);
            reponse.setMessage("Connexion réussie !");
            reponse.setData(u);

        } catch (Exception e) {
            reponse.setSuccess(false);
            reponse.setMessage(e.getMessage());
        }

        // On envoie la réponse EN PREMIER pour que le client puisse ouvrir le chat
        // et démarrer le MessageListener avant de recevoir les notifications push
        envoyerAuClient(reponse);

        if (reponse.isSuccess()) {
            // Notifier tous les clients qu'un nouvel utilisateur est connecté
            broadcastListeUtilisateurs();

            // On livre les messages en attente (RG6) — le MessageListener est maintenant prêt
            List<Message> messagesEnAttente = messageService.deliverPending(user);
            for (Message m : messagesEnAttente) {
                envoyerAuClient(new Packet(Packet.NEW_MESSAGE, m));
            }
        }
    }

    // Gère l'inscription d'un utilisateur
    private void gererRegister(String username, String password, String avatar) {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);

        try {
            User u = authService.register(username, password, avatar);
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

    // Gère la liste des utilisateurs connectés (retourne les objets User avec leur avatar)
    private void gererUtilisateursEnLigne() {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);
        reponse.setSuccess(true);
        reponse.setData(collecterUtilisateursEnLigne());
        envoyerAuClient(reponse);
    }

    // Retourne le résumé de toutes les conversations + les utilisateurs en ligne sans historique
    private void gererConversations() {
        Packet reponse = new Packet();
        reponse.setType(Packet.RESPONSE);
        try {
            List<ConversationInfo> conversations = messageService.getConversationSummaries(user);

            // Marquer qui est actuellement en ligne parmi les conversations existantes
            java.util.Set<String> partenairesExistants = new java.util.HashSet<>();
            for (ConversationInfo ci : conversations) {
                ci.setOnline(clientsActifs.containsKey(ci.getPartner().getUsername()));
                partenairesExistants.add(ci.getPartner().getUsername());
            }

            // Ajouter les utilisateurs en ligne qui n'ont pas encore de conversation avec moi
            for (ClientHandler handler : clientsActifs.values()) {
                String nomEnLigne = handler.user.getUsername();
                if (!nomEnLigne.equals(user.getUsername())
                        && !partenairesExistants.contains(nomEnLigne)) {
                    ConversationInfo ci = new ConversationInfo(handler.user, null, null, 0);
                    ci.setOnline(true);
                    conversations.add(ci);
                }
            }

            reponse.setSuccess(true);
            reponse.setData(conversations.toArray(new ConversationInfo[0]));
        } catch (Exception e) {
            reponse.setSuccess(false);
            reponse.setMessage(e.getMessage());
        }
        envoyerAuClient(reponse);
    }

    // Collecte le tableau des utilisateurs actuellement connectés
    private User[] collecterUtilisateursEnLigne() {
        return clientsActifs.values().stream()
                .map(ClientHandler::getUser)
                .filter(u -> u != null)
                .toArray(User[]::new);
    }

    // Envoie la liste mise à jour à tous les clients connectés (sauf expéditeur optionnel)
    private void broadcastListeUtilisateurs() {
        User[] users = collecterUtilisateursEnLigne();
        Packet notif = new Packet(Packet.USERS_UPDATED, users);
        for (ClientHandler handler : clientsActifs.values()) {
            handler.envoyerAuClient(notif);
        }
    }

    // Marque les messages comme LU et notifie l'expéditeur
    private void gererMarquageLu(String senderUsername) {
        try {
            messageService.markAsRead(user, senderUsername);
            // Notifier l'expéditeur pour qu'il passe ses ✓✓ en bleu
            ClientHandler senderHandler = clientsActifs.get(senderUsername);
            if (senderHandler != null) {
                senderHandler.envoyerAuClient(new Packet(Packet.MESSAGE_READ, user.getUsername()));
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du marquage en lu : " + e.getMessage());
        }
    }

    // Relaie l'indicateur de frappe au destinataire
    private void gererIndicateurFrappe(String receiverUsername, boolean typing) {
        ClientHandler receiverHandler = clientsActifs.get(receiverUsername);
        if (receiverHandler != null) {
            receiverHandler.envoyerAuClient(new Packet(Packet.TYPING,
                    new String[]{user.getUsername(), String.valueOf(typing)}));
        }
    }

    // Gère l'envoi d'un fichier
    private void gererEnvoiFichier(FilePayload fichier) {
        try {
            // Sauvegarder le fichier sur le serveur
            Path dossier = Paths.get("uploads");
            Files.createDirectories(dossier);
            Path destination = dossier.resolve(System.currentTimeMillis() + "_" + fichier.getFilename());
            Files.write(destination, fichier.getData());

            // Transmettre au destinataire s'il est connecté
            ClientHandler destinataireHandler = clientsActifs.get(fichier.getReceiverUsername());
            if (destinataireHandler != null) {
                destinataireHandler.envoyerAuClient(new Packet(Packet.NEW_FILE, fichier));
            }

            logger.logMessage(fichier.getSenderUsername(), fichier.getReceiverUsername());

        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }

    // Gère la suppression d'un message
    private void gererSuppressionMessage(Long messageId) {
        try {
            Message message = messageService.deleteMessage(user, messageId);

            // Notifier l'expéditeur (confirmation)
            envoyerAuClient(new Packet(Packet.MESSAGE_DELETED, messageId));

            // Notifier le destinataire s'il est connecté
            String receiverUsername = message.getReceiver().getUsername();
            ClientHandler receiverHandler = clientsActifs.get(receiverUsername);
            if (receiverHandler != null) {
                receiverHandler.envoyerAuClient(new Packet(Packet.MESSAGE_DELETED, messageId));
            }
        } catch (Exception e) {
            System.out.println("Suppression impossible : " + e.getMessage());
        }
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
            // Notifier les autres que cet utilisateur est parti
            broadcastListeUtilisateurs();
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
