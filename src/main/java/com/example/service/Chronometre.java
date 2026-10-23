package com.example.service;

import java.util.Arrays;

/**
 * Chronometre de campagne : execute plusieurs fois le meme travail et retient
 * la mediane, seule valeur robuste face aux pics du ramasse-miettes ou a
 * l'initialisation de la JVM.
 *
 * <p>Le compteur SQL est remis a zero avant chaque tour : la mesure retournee
 * decrit donc le cout d'<em>un seul</em> parcours, et non la somme de tous les
 * tours.</p>
 */
public final class Chronometre {

    private static final int TOURS_PAR_DEFAUT = 3;

    private final int echauffements;
    private final int tours;

    public Chronometre() {
        this(0, TOURS_PAR_DEFAUT);
    }

    public Chronometre(int echauffements, int tours) {
        if (echauffements < 0) {
            throw new IllegalArgumentException("Le nombre d'echauffements ne peut pas etre negatif.");
        }
        if (tours < 1) {
            throw new IllegalArgumentException("Il faut au moins un tour de mesure.");
        }
        this.echauffements = echauffements;
        this.tours = tours;
    }

    public int getEchauffements() {
        return echauffements;
    }

    public int getTours() {
        return tours;
    }

    /**
     * Execute {@code travail} {@code echauffements + tours} fois et retourne la
     * mediane des tours mesures accompagnee du cout SQL du dernier tour.
     */
    public Mesure mesurer(String libelle, Runnable travail) {
        if (travail == null) {
            throw new IllegalArgumentException("Le travail a mesurer est obligatoire.");
        }
        for (int i = 0; i < echauffements; i++) {
            travail.run();
        }

        long[] durees = new long[tours];
        long requetes = 0L;
        int motifs = 0;

        for (int i = 0; i < tours; i++) {
            CompteurRequetes.reinitialiser();
            long depart = System.nanoTime();
            travail.run();
            durees[i] = Math.round((System.nanoTime() - depart) / 1_000_000.0d);
            requetes = CompteurRequetes.total();
            motifs = CompteurRequetes.motifsDistincts();
        }

        return new Mesure(libelle, mediane(durees), requetes, motifs, durees, CompteurRequetes.trace());
    }

    /** Mediane d'une serie non vide ; la serie est copiee pour rester intacte. */
    public static long mediane(long[] valeurs) {
        if (valeurs == null || valeurs.length == 0) {
            throw new IllegalArgumentException("Serie vide : aucune mediane calculable.");
        }
        long[] copie = valeurs.clone();
        Arrays.sort(copie);
        int milieu = copie.length / 2;
        if (copie.length % 2 == 1) {
            return copie[milieu];
        }
        return Math.round((copie[milieu - 1] + copie[milieu]) / 2.0d);
    }
}
