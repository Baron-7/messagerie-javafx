package com.messagerie.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Le serveur principal : accepte les connexions des clients
public class Server {

    // Port sur lequel le serveur écoute
    private static final int PORT = 5000;

    // Liste des clients connectés : username -> ClientHandler
    private Map<String, ClientHandler> clientsActifs = new ConcurrentHashMap<>();

    // Pool de threads pour gérer plusieurs clients en même temps (RG11)
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private ServerLogger logger = new ServerLogger();

    // Démarre le serveur
    public void demarrer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            logger.logInfo("Serveur démarré sur le port " + PORT);
            logger.logInfo("En attente de connexions...");

            // Boucle infinie : on attend les nouvelles connexions
            while (true) {
                Socket socket = serverSocket.accept();
                logger.logInfo("Nouvelle connexion entrante.");

                // Pour chaque client, on crée un handler et on le lance dans un thread (RG11)
                ClientHandler handler = new ClientHandler(socket, clientsActifs, logger);
                threadPool.execute(handler);
            }

        } catch (Exception e) {
            System.out.println("Erreur serveur : " + e.getMessage());
        }
    }

    // Point d'entrée du serveur (à lancer séparément du client)
    public static void main(String[] args) {
        Server server = new Server();
        server.demarrer();
    }
}
