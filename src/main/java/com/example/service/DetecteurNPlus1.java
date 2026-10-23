package com.example.service;

/**
 * Diagnostic automatique du probleme N+1.
 *
 * <p>L'idee : pour un parcours de {@code n} entites, un chargement correct coute
 * une requete. Des que le nombre d'instructions depasse ce seuil, chaque entite
 * a declenche son propre aller-retour vers la base : c'est la signature du N+1.</p>
 */
public final class DetecteurNPlus1 {

    private DetecteurNPlus1() {
    }

    public static Verdict analyser(int entites, Mesure mesure) {
        if (mesure == null) {
            throw new IllegalArgumentException("La mesure est obligatoire.");
        }
        boolean detecte = mesure.getRequetesSql() > 1L;
        long evitables = detecte ? mesure.getRequetesSql() - 1L : 0L;
        return new Verdict(mesure.getLibelle(), entites, mesure.getRequetesSql(), detecte, evitables);
    }

    /** Conclusion du diagnostic pour un parcours donne. */
    public static final class Verdict {

        private final String libelle;
        private final int entites;
        private final long requetes;
        private final boolean nPlus1;
        private final long requetesEvitables;

        private Verdict(String libelle, int entites, long requetes, boolean nPlus1, long requetesEvitables) {
            this.libelle = libelle;
            this.entites = entites;
            this.requetes = requetes;
            this.nPlus1 = nPlus1;
            this.requetesEvitables = requetesEvitables;
        }

        public String getLibelle() {
            return libelle;
        }

        public int getEntites() {
            return entites;
        }

        public long getRequetes() {
            return requetes;
        }

        public boolean isNPlus1() {
            return nPlus1;
        }

        public long getRequetesEvitables() {
            return requetesEvitables;
        }

        public String getConclusion() {
            if (!nPlus1) {
                return "OK (1 requete)";
            }
            return "N+1 DETECTE (" + requetesEvitables + " requete(s) evitables)";
        }

        @Override
        public String toString() {
            return libelle + " -> " + getConclusion();
        }
    }
}
