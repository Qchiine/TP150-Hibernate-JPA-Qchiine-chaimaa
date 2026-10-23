package com.example.presentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Rendu d'un tableau texte pour la console.
 *
 * <p>Les valeurs numeriques sont alignees a droite, le texte a gauche : sans cela
 * les compteurs de requetes deviennent illisibles des que les ordres de grandeur
 * changent.</p>
 */
public final class Tableau {

    private final String[] entetes;
    private final List<String[]> lignes = new ArrayList<>();

    public Tableau(String... entetes) {
        if (entetes == null || entetes.length == 0) {
            throw new IllegalArgumentException("Un tableau doit avoir au moins une colonne.");
        }
        this.entetes = entetes.clone();
    }

    public Tableau ligne(Object... cellules) {
        if (cellules == null || cellules.length != entetes.length) {
            throw new IllegalArgumentException(
                    "Attendu " + entetes.length + " cellule(s), recu "
                            + (cellules == null ? 0 : cellules.length) + ".");
        }
        String[] ligne = new String[cellules.length];
        for (int i = 0; i < cellules.length; i++) {
            ligne[i] = cellules[i] == null ? "-" : String.valueOf(cellules[i]);
        }
        lignes.add(ligne);
        return this;
    }

    public String rendre() {
        int[] largeurs = new int[entetes.length];
        for (int i = 0; i < entetes.length; i++) {
            largeurs[i] = entetes[i].length();
        }
        for (String[] ligne : lignes) {
            for (int i = 0; i < ligne.length; i++) {
                largeurs[i] = Math.max(largeurs[i], ligne[i].length());
            }
        }

        StringBuilder sortie = new StringBuilder();
        sortie.append(separateur(largeurs, '\u250c', '\u252c', '\u2510')).append('\n');
        sortie.append(ligneFormatee(entetes, largeurs)).append('\n');
        sortie.append(separateur(largeurs, '\u251c', '\u253c', '\u2524')).append('\n');
        for (String[] ligne : lignes) {
            sortie.append(ligneFormatee(ligne, largeurs)).append('\n');
        }
        sortie.append(separateur(largeurs, '\u2514', '\u2534', '\u2518'));
        return sortie.toString();
    }

    private String separateur(int[] largeurs, char debut, char milieu, char fin) {
        StringBuilder trait = new StringBuilder();
        trait.append(debut);
        for (int i = 0; i < largeurs.length; i++) {
            for (int j = 0; j < largeurs[i] + 2; j++) {
                trait.append('\u2500');
            }
            trait.append(i == largeurs.length - 1 ? fin : milieu);
        }
        return trait.toString();
    }

    private String ligneFormatee(String[] cellules, int[] largeurs) {
        StringBuilder ligne = new StringBuilder();
        for (int i = 0; i < cellules.length; i++) {
            ligne.append('\u2502');
            ligne.append(' ');
            ligne.append(cadrer(cellules[i], largeurs[i]));
            ligne.append(' ');
        }
        ligne.append('\u2502');
        return ligne.toString();
    }

    private String cadrer(String valeur, int largeur) {
        int manque = largeur - valeur.length();
        if (manque <= 0) {
            return valeur;
        }
        StringBuilder remplissage = new StringBuilder();
        for (int i = 0; i < manque; i++) {
            remplissage.append(' ');
        }
        return estNumerique(valeur) ? remplissage + valeur : valeur + remplissage;
    }

    private boolean estNumerique(String valeur) {
        if (valeur.isEmpty()) {
            return false;
        }
        for (int i = 0; i < valeur.length(); i++) {
            char c = valeur.charAt(i);
            if (!Character.isDigit(c) && c != '-' && c != '.' && c != '%' && c != '+') {
                return false;
            }
        }
        return Character.isDigit(valeur.charAt(valeur.length() - 1));
    }
}
