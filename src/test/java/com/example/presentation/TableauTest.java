package com.example.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Verifie le rendu du tableau : largeurs, alignements et refus des lignes incompletes. */
public class TableauTest {

    @Test
    public void chaqueLigneALaLargeurDeLaColonneLaPlusLarge() {
        String rendu = new Tableau("REGION", "HITS")
                .ligne("com.example.model.Auteur", 4)
                .ligne("Livre", 100)
                .rendre();

        String[] lignes = rendu.split("\\R");
        assertEquals(6, lignes.length);
        for (String ligne : lignes) {
            assertEquals("largeur constante attendue : " + ligne, lignes[0].length(), ligne.length());
        }
    }

    @Test
    public void lesNombresSontAlignesADroiteEtLeTexteAGauche() {
        String rendu = new Tableau("CLE", "VALEUR").ligne("a", 7).ligne("bbbb", 12).rendre();

        assertTrue(rendu.contains("\u2502 a    \u2502      7 \u2502"));
        assertTrue(rendu.contains("\u2502 bbbb \u2502     12 \u2502"));
    }

    @Test
    public void lesBorduresEtLesSeparateursUtilisentLeMemeJeuDeCaracteres() {
        String rendu = new Tableau("A").ligne("x").rendre();

        assertTrue(rendu.startsWith("\u250c"));
        assertTrue(rendu.contains("\u2502"));
        assertTrue(rendu.endsWith("\u2518"));
    }

    @Test
    public void uneCelluleNulleEstRemplaceeParUnTiret() {
        String rendu = new Tableau("A", "B").ligne("x", null).rendre();

        assertTrue(rendu.contains("\u2502 x \u2502 - \u2502"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void uneLigneIncompleteEstRefusee() {
        new Tableau("A", "B").ligne("x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unTableauSansColonneEstRefuse() {
        new Tableau();
    }
}
