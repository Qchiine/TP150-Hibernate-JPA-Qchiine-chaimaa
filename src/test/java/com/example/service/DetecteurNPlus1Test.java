package com.example.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

/** Verifie le diagnostic automatique du N+1 sur des mesures fabriquees. */
public class DetecteurNPlus1Test {

    private static Mesure mesure(long requetes) {
        return new Mesure("test", 12L, requetes, 2, new long[] { 12L }, Collections.<String>emptyList());
    }

    @Test
    public void uneSeuleRequeteNestPasUnNPlus1() {
        DetecteurNPlus1.Verdict verdict = DetecteurNPlus1.analyser(10, mesure(1L));

        assertFalse(verdict.isNPlus1());
        assertEquals(0L, verdict.getRequetesEvitables());
        assertEquals("OK (1 requete)", verdict.getConclusion());
    }

    @Test
    public void onzeRequetesPourDixEntitesSignentLeNPlus1() {
        DetecteurNPlus1.Verdict verdict = DetecteurNPlus1.analyser(10, mesure(11L));

        assertTrue(verdict.isNPlus1());
        assertEquals(10L, verdict.getRequetesEvitables());
        assertEquals("N+1 DETECTE (10 requete(s) evitables)", verdict.getConclusion());
    }

    @Test
    public void uneRelectureEntierementEnCacheNeProduitAucuneRequete() {
        DetecteurNPlus1.Verdict verdict = DetecteurNPlus1.analyser(10, mesure(0L));

        assertFalse(verdict.isNPlus1());
        assertEquals(0L, verdict.getRequetesEvitables());
    }

    @Test
    public void leVerdictConserveLesEntitesAnalysees() {
        DetecteurNPlus1.Verdict verdict = DetecteurNPlus1.analyser(7, mesure(8L));

        assertEquals(7, verdict.getEntites());
        assertEquals(8L, verdict.getRequetes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void uneMesureAbsenteEstRefusee() {
        DetecteurNPlus1.analyser(10, null);
    }
}
