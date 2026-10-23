package com.example.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Verifie le chronometre : la mediane doit ignorer les tours aberrants et le
 * cout SQL doit decrire un seul parcours, pas la somme des tours.
 */
public class ChronometreTest {

    @Test
    public void medianeDuneSerieImpaire() {
        assertEquals(3L, Chronometre.mediane(new long[] { 5L, 1L, 3L }));
    }

    @Test
    public void medianeDuneSeriePaire() {
        assertEquals(3L, Chronometre.mediane(new long[] { 4L, 1L, 2L, 3L }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void medianeRefuseeSurUneSerieVide() {
        Chronometre.mediane(new long[0]);
    }

    @Test
    public void laMedianeNeModifiePasLaSerieDOrigine() {
        long[] serie = { 9L, 1L, 5L };
        Chronometre.mediane(serie);
        assertEquals(9L, serie[0]);
        assertEquals(1L, serie[1]);
        assertEquals(5L, serie[2]);
    }

    @Test
    public void lesEchauffementsTournentSansEntrerDansLaMesure() {
        final AtomicInteger compteur = new AtomicInteger();
        Chronometre chronometre = new Chronometre(2, 3);

        Mesure mesure = chronometre.mesurer("test", new Runnable() {
            @Override
            public void run() {
                compteur.incrementAndGet();
            }
        });

        assertEquals(5, compteur.get());
        assertEquals(3, mesure.getNombreDeTours());
        assertTrue(mesure.getMillisecondes() >= 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ilFautAuMoinsUnTourDeMesure() {
        new Chronometre(0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void leTravailAMesurerEstObligatoire() {
        new Chronometre().mesurer("test", null);
    }
}
