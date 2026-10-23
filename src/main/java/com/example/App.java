package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;

import com.example.presentation.Tableau;
import com.example.service.AuditCache;
import com.example.service.DataInitService;
import com.example.service.DetecteurNPlus1;
import com.example.service.Mesure;
import com.example.service.PerformanceTestService;
import com.example.util.Persistance;

/**
 * Point d'entree du TP 7.
 *
 * <p>Le programme enchaine six etapes : catalogue de test, parcours paresseux
 * (N+1), {@code JOIN FETCH}, graphes d'entites, cache de second niveau puis
 * audit. Chaque etape affiche la ligne de mesure attendue par l'enonce et
 * alimente le tableau recapitulatif final.</p>
 */
public final class App {

    private static final String SEPARATEUR = "=======================================================================";
    private static final int ETAPES = 6;

    private App() {
    }

    public static void main(String[] args) {
        reduireLeBruitDesBibliotheques();
        banniere();

        EntityManagerFactory fabrique = Persistance.fabrique();
        List<Mesure> mesures = new ArrayList<>();
        try {
            DataInitService initialisation = new DataInitService(fabrique);
            int livres = initialisation.initData();
            System.out.println("Catalogue insere : " + DataInitService.AUTEURS_PAR_DEFAUT + " auteurs et " + livres
                    + " livres  [controle base : " + initialisation.compterAuteurs() + " / "
                    + initialisation.compterLivres() + "]");
            System.out.println();

            PerformanceTestService service = new PerformanceTestService(fabrique);
            AuditCache audit = service.getAuditCache();
            audit.evincerTout();
            audit.reinitialiserCompteurs();

            etape(1, "Lecture paresseuse : le probleme N+1 a l'etat brut");
            Mesure paresseuse = service.testPerformanceSansCache();
            mesures.add(paresseuse);
            System.out.println("    >> "
                    + DetecteurNPlus1.analyser(DataInitService.AUTEURS_PAR_DEFAUT, paresseuse).getConclusion());

            etape(2, "JOIN FETCH : auteurs et livres ramenes en une passe");
            Mesure jointure = service.testPerformanceAvecJoinFetch();
            mesures.add(jointure);
            System.out.println("    >> " + DetecteurNPlus1.analyser(DataInitService.AUTEURS_PAR_DEFAUT, jointure)
                    .getConclusion());

            etape(3, "Entity Graphs : charger sans toucher au JPQL");
            mesures.add(service.testPerformanceAvecEntityGraph());
            mesures.add(service.testPerformanceAvecGrapheProgrammatique());
            int[] doublons = service.comparerDistinct();
            System.out.println("    >> doublons de jointure : sans DISTINCT = " + doublons[0]
                    + " elements, avec DISTINCT = " + doublons[1]);

            etape(4, "Cache de second niveau EhCache : lectures successives");
            mesures.add(service.testPerformanceAvecCache());
            Mesure relecture = service.testRelectureParCle();
            mesures.add(relecture);
            System.out.println("    >> relecture par cle : " + relecture.getRequetesSql()
                    + " requete(s) SQL, tout vient du cache de second niveau");

            etape(5, "Audit des regions de cache");
            System.out.println(tableauCache(audit));

            etape(6, "Recapitulatif des mesures");
            System.out.println(tableauMesures(mesures));

            bilan(paresseuse, jointure, relecture);
        } finally {
            Persistance.fermer();
        }
    }

    private static void banniere() {
        System.out.println(SEPARATEUR);
        System.out.println(" TP 7 - Optimisation JPA : cache de second niveau, JOIN FETCH");
        System.out.println("        et Entity Graphs");
        System.out.println(" Hibernate 5.4 / JCache + EhCache 3 / H2 memoire");
        System.out.println(" Qchiine Chaimaa");
        System.out.println(SEPARATEUR);
        System.out.println();
    }

    /**
     * Hibernate, EhCache et JCache ecrivent des dizaines de lignes de journal a
     * chaque session, ce qui noie les mesures. Les instructions SQL demandees par
     * l'enonce restent visibles : {@code hibernate.show_sql} les ecrit sur la
     * sortie standard, independamment du journal.
     */
    private static void reduireLeBruitDesBibliotheques() {
        Logger racine = Logger.getLogger("");
        racine.setLevel(Level.WARNING);
        for (Handler handler : racine.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
        Logger.getLogger("org.ehcache").setLevel(Level.WARNING);
        Logger.getLogger("javax.cache").setLevel(Level.WARNING);
        // Le pool de connexions integre n'est pas destine a la production : ce
        // rappel n'apporte rien a la lecture des mesures.
        Logger.getLogger("org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl")
                .setLevel(Level.SEVERE);
    }

    private static void etape(int numero, String titre) {
        System.out.println();
        System.out.println("[Etape " + numero + "/" + ETAPES + "] " + titre);
        System.out.println("-----------------------------------------------------------------------");
    }

    private static String tableauCache(AuditCache audit) {
        Tableau tableau = new Tableau("REGION", "HITS", "MISSES", "PUTS", "EN MEMOIRE", "TAUX");
        String[] regions = { AuditCache.REGION_AUTEUR, AuditCache.REGION_LIVRE, AuditCache.REGION_LIVRES_AUTEUR };
        for (String region : regions) {
            tableau.ligne(region,
                    audit.hits(region),
                    audit.misses(region),
                    audit.puts(region),
                    audit.entreesEnMemoire(region),
                    audit.tauxDeReussite(region) + " %");
        }
        return tableau.rendre();
    }

    private static String tableauMesures(List<Mesure> mesures) {
        Tableau tableau = new Tableau("STRATEGIE", "REQUETES SQL", "MOTIFS", "MEDIANE (ms)", "TOURS");
        for (Mesure mesure : mesures) {
            tableau.ligne(mesure.getLibelle(),
                    mesure.getRequetesSql(),
                    mesure.getMotifsSql(),
                    mesure.getMillisecondes(),
                    mesure.getNombreDeTours());
        }
        return tableau.rendre();
    }

    private static void bilan(Mesure paresseuse, Mesure jointure, Mesure relecture) {
        System.out.println();
        System.out.println(SEPARATEUR);
        System.out.println(" BILAN");
        System.out.println("   - N+1 : " + paresseuse.getRequetesSql() + " requetes pour "
                + DataInitService.AUTEURS_PAR_DEFAUT + " auteurs, contre " + jointure.getRequetesSql()
                + " avec JOIN FETCH");
        System.out.println("   - Relecture par cle entierement servie par le cache : "
                + relecture.getRequetesSql() + " requete(s) SQL");
        System.out.println("   - Le cache de second niveau ne remplace pas l'optimisation du JPQL :");
        System.out.println("     JOIN FETCH agit sur la requete, le cache agit sur les lectures repetees.");
        System.out.println(SEPARATEUR);
    }
}
