package com.messagerie.dao;

import com.messagerie.model.User;
import com.messagerie.model.UserStatus;
import com.messagerie.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

// Classe qui gère les opérations sur les utilisateurs en base de données
public class UserDAO {

    // Sauvegarde un nouvel utilisateur
    public void save(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.save(user);
        tx.commit();
        session.close();
    }

    // Cherche un utilisateur par son username
    public User findByUsername(String username) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        User user = session.createQuery("FROM User WHERE username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
        session.close();
        return user;
    }

    // Retourne tous les utilisateurs
    public List<User> findAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<User> users = session.createQuery("FROM User", User.class).list();
        session.close();
        return users;
    }

    // Retourne tous les utilisateurs en ligne
    public List<User> findOnlineUsers() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<User> users = session.createQuery("FROM User WHERE status = :status", User.class)
                .setParameter("status", UserStatus.ONLINE)
                .list();
        session.close();
        return users;
    }

    // Met à jour un utilisateur (ex: changer son statut)
    public void update(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.update(user);
        tx.commit();
        session.close();
    }
}
