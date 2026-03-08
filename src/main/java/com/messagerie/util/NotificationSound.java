package com.messagerie.util;

import javax.sound.sampled.*;

/**
 * Joue un son de notification personnalisé (double bip montant).
 * Aucun fichier audio externe requis — le son est généré en mémoire.
 *
 * Personnalisation rapide :
 *   - FREQ1 / FREQ2 : fréquences en Hz (ex. 880 = La5, 1100 = Do#6)
 *   - DUR1  / DUR2  : durées en millisecondes
 *   - VOLUME        : amplitude 0.0 (silence) → 1.0 (max)
 */
public class NotificationSound {

    private static final float  SAMPLE_RATE = 44100f;
    private static final double FREQ1       = 880.0;   // 1er bip
    private static final double FREQ2       = 1100.0;  // 2e bip (plus haut)
    private static final int    DUR1        = 120;     // ms
    private static final int    DUR2        = 180;     // ms
    private static final int    PAUSE       = 60;      // ms entre les deux bips
    private static final double VOLUME      = 0.6;     // 0.0 – 1.0

    /** Joue la notification dans un thread daemon (non bloquant). */
    public static void play() {
        Thread t = new Thread(() -> {
            try {
                jouerTon(FREQ1, DUR1);
                Thread.sleep(PAUSE);
                jouerTon(FREQ2, DUR2);
            } catch (InterruptedException ignored) {
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void jouerTon(double frequence, int durationMs) {
        try {
            int nbSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[nbSamples];

            for (int i = 0; i < nbSamples; i++) {
                // Onde sinusoïdale avec enveloppe adoucissante (fade-in / fade-out)
                double angle = 2.0 * Math.PI * i * frequence / SAMPLE_RATE;
                double envelope = enveloppe(i, nbSamples);
                buffer[i] = (byte) (VOLUME * envelope * 127 * Math.sin(angle));
            }

            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
            line.close();
        } catch (Exception e) {
            // Son non disponible : on ignore silencieusement
        }
    }

    /** Enveloppe trapézoïdale : fade-in 10 % / sustain / fade-out 20 %. */
    private static double enveloppe(int i, int total) {
        double pos = (double) i / total;
        if (pos < 0.10) return pos / 0.10;
        if (pos > 0.80) return (1.0 - pos) / 0.20;
        return 1.0;
    }
}
