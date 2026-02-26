package com.messagerie.service;

import com.messagerie.dao.UserDAO;
import com.messagerie.model.User;
import com.messagerie.model.UserStatus;
import com.messagerie.util.PasswordUtil;

// Service qui gère l'inscription, la connexion et la déconnexion
public class AuthService {

    private UserDAO userDAO = new UserDAO();

    // Inscription : crée un nouveau compte avec avatar emoji
    public User register(String username, String password, String avatar) throws Exception {

        // Le username ne doit pas être vide
        if (username == null || username.trim().isEmpty()) {
            throw new Exception("Le nom d'utilisateur ne peut pas être vide.");
        }

        // Le mot de passe ne doit pas être vide
        if (password == null || password.trim().isEmpty()) {
            throw new Exception("Le mot de passe ne peut pas être vide.");
        }

        // On vérifie que le username n'est pas déjà pris (RG1)
        if (userDAO.findByUsername(username) != null) {
            throw new Exception("Ce nom d'utilisateur est déjà pris.");
        }

        // On hache le mot de passe avant de le stocker (RG9)
        String motDePasseHache = PasswordUtil.hash(password);

        // On crée l'utilisateur avec son avatar et on le sauvegarde
        User user = new User(username, motDePasseHache);
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(avatar);
        }
        userDAO.save(user);

        return user;
    }

    // Connexion : vérifie les identifiants et connecte l'utilisateur
    public User login(String username, String password) throws Exception {

        // On cherche l'utilisateur par son username
        User user = userDAO.findByUsername(username);

        // Si l'utilisateur n'existe pas
        if (user == null) {
            throw new Exception("Nom d'utilisateur introuvable.");
        }

        // On vérifie le mot de passe (RG9)
        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new Exception("Mot de passe incorrect.");
        }

        // Un utilisateur ne peut pas être connecté deux fois (RG3)
        if (user.getStatus() == UserStatus.ONLINE) {
            throw new Exception("Cet utilisateur est déjà connecté depuis un autre appareil.");
        }

        // On passe le statut à ONLINE (RG4)
        user.setStatus(UserStatus.ONLINE);
        userDAO.update(user);

        return user;
    }

    // Déconnexion : passe l'utilisateur à OFFLINE
    public void logout(User user) {
        // On passe le statut à OFFLINE (RG4)
        user.setStatus(UserStatus.OFFLINE);
        userDAO.update(user);
    }
}
