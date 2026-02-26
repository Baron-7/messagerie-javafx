package com.messagerie.client;

import com.messagerie.model.Packet;
import com.messagerie.model.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// TODO (Personne B) : classe côté client pour se connecter au serveur
// Cette classe gère la connexion socket vers le serveur
public class Client {

    private static final String HOST = "localhost"; // adresse du serveur
    private static final int PORT = 5000;           // port du serveur

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;

    // File d'attente pour les réponses serveur (GET_ONLINE_USERS, GET_HISTORY...)
    // MessageListener est le seul à lire le socket et dépose les réponses ici
    private final LinkedBlockingQueue<Packet> responseQueue = new LinkedBlockingQueue<>();

    // Se connecte au serveur
    public void connecter() throws Exception {
        socket = new Socket(HOST, PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
    }

    // Envoie un paquet au serveur
    public void envoyerPaquet(Packet paquet) throws Exception {
        out.writeObject(paquet);
        out.flush();
    }

    // Reçoit un paquet du serveur (bloquant : attend jusqu'à réception)
    public Packet recevoirPaquet() throws Exception {
        return (Packet) in.readObject();
    }

    // Dépose une réponse dans la file (appelé par MessageListener)
    public void mettreEnFile(Packet paquet) {
        responseQueue.offer(paquet);
    }

    // Attend la prochaine réponse serveur (utilisé par ChatController)
    public Packet attendreReponse() throws Exception {
        Packet p = responseQueue.poll(5, TimeUnit.SECONDS);
        if (p == null) throw new Exception("Pas de réponse du serveur (timeout).");
        return p;
    }

    // Ferme la connexion
    public void fermer() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
            // on ignore
        }
    }

    // --- Getters et Setters ---

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public ObjectInputStream getIn() {
        return in;
    }
}
