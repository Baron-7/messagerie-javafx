package com.messagerie.util;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * Joue le son de notification MP3 personnalisé.
 * Utilise JavaFX MediaPlayer pour la lecture du fichier audio.
 */
public class NotificationSound {

    private static final String SOUND_RESOURCE = "/sounds/notification.mp3";

    /** Joue la notification dans le thread JavaFX (non bloquant). */
    public static void play() {
        try {
            URL resource = NotificationSound.class.getResource(SOUND_RESOURCE);
            if (resource == null) return;
            String uri = resource.toExternalForm();
            Media media = new Media(uri);
            Platform.runLater(() -> {
                try {
                    MediaPlayer player = new MediaPlayer(media);
                    player.setOnEndOfMedia(player::dispose);
                    player.play();
                } catch (Exception e) {
                    // Son non disponible : on ignore silencieusement
                }
            });
        } catch (Exception e) {
            // Son non disponible : on ignore silencieusement
        }
    }
}
