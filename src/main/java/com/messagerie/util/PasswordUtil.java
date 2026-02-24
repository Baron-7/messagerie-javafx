package com.messagerie.util;

import org.mindrot.jbcrypt.BCrypt;

// Classe pour gérer le hachage des mots de passe (RG9)
// On ne stocke jamais un mot de passe en clair !
public class PasswordUtil {

    // Transforme un mot de passe en version hachée
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Vérifie si un mot de passe correspond à un hash stocké
    public static boolean verify(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}
