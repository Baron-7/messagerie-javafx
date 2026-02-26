package com.messagerie;

import com.messagerie.model.User;
import com.messagerie.service.AuthService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Tests pour le service d'authentification
// Note : ces tests nécessitent une vraie base de données configurée
public class AuthServiceTest {

    private AuthService authService = new AuthService();

    // Test 1 : l'inscription avec un username valide doit fonctionner
    @Test
    public void testInscriptionReussie() throws Exception {
        // On crée un username unique à chaque fois grâce au timestamp
        String username = "testuser_" + System.currentTimeMillis();
        User user = authService.register(username, "motdepasse123", "🌿");
        assertNotNull(user);
        assertEquals(username, user.getUsername());
    }

    // Test 2 : on ne peut pas s'inscrire avec un username déjà pris
    @Test
    public void testInscriptionUsernameDejaPris() throws Exception {
        String username = "doublon_" + System.currentTimeMillis();
        authService.register(username, "motdepasse123", "🌱");

        // La deuxième inscription avec le même username doit échouer
        assertThrows(Exception.class, () -> {
            authService.register(username, "autremotdepasse", "🍀");
        });
    }

    // Test 3 : se connecter avec un username qui n'existe pas doit échouer
    @Test
    public void testConnexionUsernameInexistant() {
        assertThrows(Exception.class, () -> {
            authService.login("username_qui_nexiste_pas", "motdepasse");
        });
    }

    // Test 4 : se connecter avec un mauvais mot de passe doit échouer
    @Test
    public void testConnexionMauvaisMotDePasse() throws Exception {
        String username = "test_mdp_" + System.currentTimeMillis();
        authService.register(username, "bonMotDePasse", "🦋");

        assertThrows(Exception.class, () -> {
            authService.login(username, "mauvaisMotDePasse");
        });
    }
}
