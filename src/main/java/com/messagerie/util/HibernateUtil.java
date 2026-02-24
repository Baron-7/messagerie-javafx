package com.messagerie.util;

import com.messagerie.model.Message;
import com.messagerie.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

// Classe utilitaire pour la connexion Hibernate
// On crée la SessionFactory une seule fois et on la réutilise partout
public class HibernateUtil {

    private static SessionFactory sessionFactory;

    // Retourne la SessionFactory (la crée si elle n'existe pas encore)
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            // On lit le fichier hibernate.cfg.xml et on ajoute nos classes
            Configuration config = new Configuration().configure();
            config.addAnnotatedClass(User.class);
            config.addAnnotatedClass(Message.class);
            sessionFactory = config.buildSessionFactory();
        }
        return sessionFactory;
    }

    // Ferme la connexion quand on quitte l'application
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
