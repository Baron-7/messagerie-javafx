package com.messagerie;

import com.messagerie.dao.MessageDAO;
import com.messagerie.dao.UserDAO;
import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.User;
import com.messagerie.model.UserStatus;
import com.messagerie.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageServiceTest {

    private MessageService messageService;
    private User alice;
    private User bob;

    // Variables contrôlables depuis les faux DAOs
    private User receiverRetourne;
    private List<Message> messagesEnAttente = new ArrayList<>();
    private List<Message> conversation = new ArrayList<>();
    private List<Message> messagesSauvegardes = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        alice = new User("alice", "hash_alice");
        alice.setStatus(UserStatus.ONLINE);

        bob = new User("bob", "hash_bob");
        bob.setStatus(UserStatus.OFFLINE);

        // Faux UserDAO : retourne ce que receiverRetourne contient
        UserDAO fakeUserDAO = new UserDAO() {
            @Override public User findByUsername(String username) { return receiverRetourne; }
            @Override public void save(User user) {}
            @Override public void update(User user) {}
            @Override public List<User> findAll() { return Collections.emptyList(); }
            @Override public List<User> findOnlineUsers() { return Collections.emptyList(); }
        };

        // Faux MessageDAO : stocke les messages en mémoire, pas en base
        MessageDAO fakeMessageDAO = new MessageDAO() {
            @Override public void save(Message m) { messagesSauvegardes.add(m); }
            @Override public List<Message> findPendingMessages(User u) { return messagesEnAttente; }
            @Override public void updateStatus(Long id, MessageStatus s) {}
            @Override public List<Message> findConversation(User u1, User u2) { return conversation; }
        };

        messageService = new MessageService(fakeMessageDAO, fakeUserDAO);

        // Réinitialise les listes entre chaque test
        messagesEnAttente = new ArrayList<>();
        messagesSauvegardes = new ArrayList<>();
        conversation = new ArrayList<>();
        receiverRetourne = null;
    }

    // ===================== RG5 =====================

    // RG5 : envoi réussi si expéditeur ONLINE et destinataire existe
    @Test
    public void testEnvoiMessage_Reussi() throws Exception {
        receiverRetourne = bob;

        Message message = messageService.sendMessage(alice, "bob", "Bonjour !");

        assertNotNull(message);
        assertEquals("Bonjour !", message.getContenu());
        assertEquals(alice, message.getSender());
        assertEquals(bob, message.getReceiver());
        assertEquals(MessageStatus.ENVOYE, message.getStatut());
        assertEquals(1, messagesSauvegardes.size());
    }

    // RG5 : échec si l'expéditeur est OFFLINE
    @Test
    public void testEnvoiMessage_ExpediteurOffline() {
        alice.setStatus(UserStatus.OFFLINE);

        assertThrows(Exception.class, () ->
            messageService.sendMessage(alice, "bob", "Bonjour !")
        );

        assertEquals(0, messagesSauvegardes.size());
    }

    // RG5 : échec si le destinataire n'existe pas en base
    @Test
    public void testEnvoiMessage_DestinataireInexistant() {
        receiverRetourne = null; // le DAO ne trouve personne

        assertThrows(Exception.class, () ->
            messageService.sendMessage(alice, "inconnu", "Bonjour !")
        );

        assertEquals(0, messagesSauvegardes.size());
    }

    // ===================== RG7 =====================

    // RG7 : échec si le contenu est vide
    @Test
    public void testEnvoiMessage_ContenuVide() {
        receiverRetourne = bob;

        assertThrows(Exception.class, () ->
            messageService.sendMessage(alice, "bob", "")
        );

        assertEquals(0, messagesSauvegardes.size());
    }

    // RG7 : échec si le contenu est null
    @Test
    public void testEnvoiMessage_ContenuNull() {
        receiverRetourne = bob;

        assertThrows(Exception.class, () ->
            messageService.sendMessage(alice, "bob", null)
        );

        assertEquals(0, messagesSauvegardes.size());
    }

    // RG7 : échec si le contenu dépasse 1000 caractères
    @Test
    public void testEnvoiMessage_ContenuTropLong() {
        receiverRetourne = bob;

        assertThrows(Exception.class, () ->
            messageService.sendMessage(alice, "bob", "a".repeat(1001))
        );

        assertEquals(0, messagesSauvegardes.size());
    }

    // RG7 : succès si le contenu fait exactement 1000 caractères (limite autorisée)
    @Test
    public void testEnvoiMessage_ContenuExactement1000Caracteres() throws Exception {
        receiverRetourne = bob;

        Message message = messageService.sendMessage(alice, "bob", "a".repeat(1000));

        assertNotNull(message);
        assertEquals(1000, message.getContenu().length());
        assertEquals(1, messagesSauvegardes.size());
    }

    // ===================== RG6 =====================

    // RG6 : les messages en attente sont livrés et passent au statut RECU
    @Test
    public void testLivraison_MessagesEnAttente() {
        messagesEnAttente = new ArrayList<>(Arrays.asList(
            new Message(alice, bob, "Message 1"),
            new Message(alice, bob, "Message 2")
        ));

        List<Message> livres = messageService.deliverPending(bob);

        assertEquals(2, livres.size());
        assertEquals(MessageStatus.RECU, livres.get(0).getStatut());
        assertEquals(MessageStatus.RECU, livres.get(1).getStatut());
    }

    // RG6 : si aucun message en attente, retourne une liste vide
    @Test
    public void testLivraison_AucunMessageEnAttente() {
        messagesEnAttente = new ArrayList<>();

        List<Message> livres = messageService.deliverPending(bob);

        assertTrue(livres.isEmpty());
    }

    // ===================== RG8 =====================

    // RG8 : l'historique est retourné dans l'ordre chronologique
    @Test
    public void testHistorique_OrdreChronologique() {
        Message m1 = new Message(alice, bob, "Premier");
        m1.setDateEnvoi(LocalDateTime.now().minusMinutes(10));

        Message m2 = new Message(bob, alice, "Deuxième");
        m2.setDateEnvoi(LocalDateTime.now().minusMinutes(5));

        Message m3 = new Message(alice, bob, "Troisième");
        m3.setDateEnvoi(LocalDateTime.now());

        conversation = Arrays.asList(m1, m2, m3);

        List<Message> historique = messageService.getHistory(alice, bob);

        assertEquals(3, historique.size());
        assertTrue(historique.get(0).getDateEnvoi().isBefore(historique.get(1).getDateEnvoi()));
        assertTrue(historique.get(1).getDateEnvoi().isBefore(historique.get(2).getDateEnvoi()));
    }

    // RG8 : l'historique d'une conversation vide retourne une liste vide
    @Test
    public void testHistorique_ConversationVide() {
        conversation = new ArrayList<>();

        List<Message> historique = messageService.getHistory(alice, bob);

        assertTrue(historique.isEmpty());
    }
}
