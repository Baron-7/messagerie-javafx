package com.messagerie.dao;

import com.messagerie.model.ConversationInfo;
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

    // Retourne un message par son identifiant
    public Message findById(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Message message = session.get(Message.class, id);
        session.close();
        return message;
    }

    // Supprime un message par son identifiant
    public void delete(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        Message message = session.get(Message.class, id);
        if (message != null) {
            session.delete(message);
        }
        tx.commit();
        session.close();
    }

    // Marque tous les messages d'un expéditeur vers un destinataire comme LU
    public void markConversationAsRead(User sender, User receiver) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.createQuery(
                "UPDATE Message SET statut = :lu " +
                "WHERE sender = :sender AND receiver = :receiver AND statut != :lu")
                .setParameter("lu", MessageStatus.LU)
                .setParameter("sender", sender)
                .setParameter("receiver", receiver)
                .executeUpdate();
        tx.commit();
        session.close();
    }

    // Retourne uniquement les conversations ayant eu lieu (au moins un message échangé)
    // triées par date du dernier message décroissante
    public List<ConversationInfo> findConversationSummaries(User me) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            // Partenaires qui m'ont envoyé au moins un message
            List<User> received = session.createQuery(
                    "SELECT DISTINCT m.sender FROM Message m WHERE m.receiver = :me", User.class)
                    .setParameter("me", me).list();

            // Partenaires à qui j'ai envoyé au moins un message
            List<User> sent = session.createQuery(
                    "SELECT DISTINCT m.receiver FROM Message m WHERE m.sender = :me", User.class)
                    .setParameter("me", me).list();

            java.util.Set<User> partners = new java.util.HashSet<>(received);
            partners.addAll(sent);

            List<ConversationInfo> result = new java.util.ArrayList<>();
            for (User partner : partners) {
                // Dernier message échangé avec ce partenaire
                Message last = session.createQuery(
                        "FROM Message m WHERE (m.sender = :me AND m.receiver = :p) " +
                        "OR (m.sender = :p AND m.receiver = :me) ORDER BY m.dateEnvoi DESC",
                        Message.class)
                        .setParameter("me", me)
                        .setParameter("p", partner)
                        .setMaxResults(1)
                        .uniqueResult();

                // Nombre de messages non lus reçus de ce partenaire
                Long unread = session.createQuery(
                        "SELECT COUNT(m) FROM Message m " +
                        "WHERE m.sender = :p AND m.receiver = :me AND m.statut != :lu",
                        Long.class)
                        .setParameter("p", partner)
                        .setParameter("me", me)
                        .setParameter("lu", MessageStatus.LU)
                        .uniqueResult();

                if (last != null) {
                    result.add(new ConversationInfo(
                        partner, last.getContenu(), last.getDateEnvoi(), unread.intValue()));
                }
            }

            // Trier par date du dernier message décroissante
            result.sort((a, b) -> b.getLastDate().compareTo(a.getLastDate()));

            return result;
        } finally {
            session.close();
        }
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
