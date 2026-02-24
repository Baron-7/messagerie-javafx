package com.messagerie.dao;

import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.User;
import com.messagerie.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

// TODO (Personne B) : cette classe gère les opérations sur les messages en base de données
public class MessageDAO {

    // Sauvegarde un message en base
    public void save(Message message) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.save(message);
        tx.commit();
        session.close();
    }

    // Retourne tous les messages entre deux utilisateurs, triés par date (RG8)
    public List<Message> findConversation(User u1, User u2) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Message> messages = session.createQuery(
                        "FROM Message WHERE (sender = :u1 AND receiver = :u2) " +
                        "OR (sender = :u2 AND receiver = :u1) ORDER BY dateEnvoi",
                        Message.class)
                .setParameter("u1", u1)
                .setParameter("u2", u2)
                .list();
        session.close();
        return messages;
    }

    // Retourne les messages en attente pour un utilisateur hors ligne (RG6)
    public List<Message> findPendingMessages(User receiver) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Message> messages = session.createQuery(
                        "FROM Message WHERE receiver = :receiver AND statut = :statut",
                        Message.class)
                .setParameter("receiver", receiver)
                .setParameter("statut", MessageStatus.ENVOYE)
                .list();
        session.close();
        return messages;
    }

    // Change le statut d'un message (ENVOYE -> RECU ou LU)
    public void updateStatus(Long id, MessageStatus statut) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        Message message = session.get(Message.class, id);
        if (message != null) {
            message.setStatut(statut);
            session.update(message);
        }
        tx.commit();
        session.close();
    }
}
