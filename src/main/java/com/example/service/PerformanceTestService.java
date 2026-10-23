package com.example.service;

import java.util.List;
import java.util.function.Function;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.Session;

import com.example.model.Auteur;
import com.example.util.Persistance;

/**
 * Banc d'essai des differentes strategies de chargement.
 *
 * <p>Chaque methode publique correspond a une etape du TP et affiche la ligne de
 * mesure attendue ({@code Durée ... : n ms}). Toutes les mesures sont en plus
 * retournees sous forme de {@link Mesure} afin d'etre agregees dans le tableau
 * recapitulatif final : un chiffre affiche sans tracabilite ne prouve rien.</p>
 *
 * <p>Chaque tour de mesure ouvre sa propre session : c'est la condition pour que
 * le cache de premier niveau (propre a la session) ne masque pas le cout reel et
 * pour que seul le cache de second niveau soit reellement teste.</p>
 */
public class PerformanceTestService {

    private static final String REQUETE_AUTEURS = "SELECT a FROM Auteur a";
    private static final String REQUETE_JOIN_FETCH = "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres";

    private final EntityManagerFactory fabrique;
    private final Chronometre chronometre;
    private final AuditCache audit;

    public PerformanceTestService() {
        this(Persistance.fabrique());
    }

    public PerformanceTestService(EntityManagerFactory fabrique) {
        this(fabrique, new Chronometre(), new AuditCache(fabrique));
    }

    public PerformanceTestService(EntityManagerFactory fabrique, Chronometre chronometre, AuditCache audit) {
        if (fabrique == null) {
            throw new IllegalArgumentException("La fabrique d'entites est obligatoire.");
        }
        this.fabrique = fabrique;
        this.chronometre = chronometre == null ? new Chronometre() : chronometre;
        this.audit = audit == null ? new AuditCache(fabrique) : audit;
    }

    public EntityManagerFactory getFabrique() {
        return fabrique;
    }

    public AuditCache getAuditCache() {
        return audit;
    }

    // ------------------------------------------------------------------
    // Etape 7 : parcours paresseux, le N+1 a l'etat brut
    // ------------------------------------------------------------------

    /**
     * Parcourt tous les auteurs en laissant les livres en {@code LAZY} :
     * Hibernate emet une requete pour la liste, puis une par auteur.
     *
     * <p>Le cache de second niveau est volontairement neutralise ici
     * ({@link CacheMode#IGNORE}) : sans cela, les collections deja presentes
     * depuis l'insertion initiale seraient servies par le cache et le N+1
     * resterait invisible. La mesure doit montrer le cout reel du parcours
     * paresseux, pas celui d'un cache deja chaud.</p>
     */
    public Mesure testPerformanceSansCache() {
        Mesure mesure = chronometre.mesurer("Sans optimisation (N+1)", new Runnable() {
            @Override
            public void run() {
                parcoursParesseuxSansCache();
            }
        });
        System.out.println("Durée sans cache : " + mesure.getMillisecondes() + " ms");
        return mesure;
    }

    // ------------------------------------------------------------------
    // Etape 8 : une jointure explicite ramene tout en une passe
    // ------------------------------------------------------------------

    /** Charge auteurs et livres en une seule requete grace a {@code JOIN FETCH}. */
    public Mesure testPerformanceAvecJoinFetch() {
        Mesure mesure = chronometre.mesurer("JOIN FETCH", new Runnable() {
            @Override
            public void run() {
                parcoursJoinFetch();
            }
        });
        System.out.println("Durée avec JOIN FETCH : " + mesure.getMillisecondes() + " ms");
        return mesure;
    }

    // ------------------------------------------------------------------
    // Etape 9 : graphes d'entites, sans toucher au JPQL
    // ------------------------------------------------------------------

    /** Utilise le graphe nomme declare sur {@link Auteur} via le hint {@code fetchgraph}. */
    public Mesure testPerformanceAvecEntityGraph() {
        Mesure mesure = chronometre.mesurer("Entity Graph nomme", new Runnable() {
            @Override
            public void run() {
                parcoursEntityGraphNomme();
            }
        });
        System.out.println("Durée avec Entity Graph : " + mesure.getMillisecondes() + " ms");
        return mesure;
    }

    /** Meme intention, mais le graphe est construit a la volee (aucune annotation). */
    public Mesure testPerformanceAvecGrapheProgrammatique() {
        Mesure mesure = chronometre.mesurer("Entity Graph programmatique", new Runnable() {
            @Override
            public void run() {
                parcoursGrapheProgrammatique();
            }
        });
        System.out.println("Durée avec Entity Graph programmatique : " + mesure.getMillisecondes() + " ms");
        return mesure;
    }

    // ------------------------------------------------------------------
    // Etape 10 : cache de second niveau
    // ------------------------------------------------------------------

    /**
     * Rejoue la meme requete une seconde fois : les entites et la collection sont
     * desormais presentes dans le cache de second niveau, partage par toutes les
     * sessions.
     */
    public Mesure testPerformanceAvecCache() {
        // Premiere lecture : elle remplit le cache de second niveau sans etre
        // chronometree, la mesure ne devant porter que sur la relecture.
        parcoursJoinFetch();

        Mesure mesure = chronometre.mesurer("Cache 2e niveau (lecture 2)", new Runnable() {
            @Override
            public void run() {
                parcoursJoinFetch();
            }
        });
        System.out.println("Durée avec cache (lecture 2) : " + mesure.getMillisecondes() + " ms");
        return mesure;
    }

    /**
     * Touche personnelle : relecture par cle primaire.
     *
     * <p>Une requete JPQL n'est jamais servie par le cache d'entites (sauf cache de
     * requetes explicite) : c'est pourquoi la lecture 2 ci-dessus conserve une
     * instruction SQL. En relisant par identifiant, le cache de second niveau joue
     * pleinement son role et le nombre de requetes tombe a zero.</p>
     */
    public Mesure testRelectureParCle() {
        final List<Long> identifiants = identifiantsAuteurs();
        parcoursParCle(identifiants);

        CompteurRequetes.reinitialiser();
        long depart = System.nanoTime();
        parcoursParCle(identifiants);
        long duree = Math.round((System.nanoTime() - depart) / 1_000_000.0d);

        Mesure mesure = new Mesure("Cache 2e niveau (relecture par cle)", duree,
                CompteurRequetes.total(), CompteurRequetes.motifsDistincts(),
                new long[] { duree }, CompteurRequetes.trace());
        System.out.println("Durée avec cache (relecture par clé) : " + duree + " ms");
        return mesure;
    }

    // ------------------------------------------------------------------
    // Touche personnelle : maitrise des doublons produits par JOIN FETCH
    // ------------------------------------------------------------------

    /**
     * Compare le nombre d'elements ramenes par une jointure avec et sans
     * {@code DISTINCT} : la jointure multiplie les lignes racines, ce qui gonfle
     * la liste et fausse tout comptage ulterieur.
     *
     * @return un tableau de deux valeurs : sans DISTINCT, puis avec DISTINCT
     */
    public int[] comparerDistinct() {
        int sans = tailleListe("SELECT a FROM Auteur a JOIN FETCH a.livres");
        int avec = tailleListe(REQUETE_JOIN_FETCH);
        return new int[] { sans, avec };
    }

    public List<Long> identifiantsAuteurs() {
        return enLecture(new Function<EntityManager, List<Long>>() {
            @Override
            public List<Long> apply(EntityManager em) {
                return em.createQuery("SELECT a.id FROM Auteur a ORDER BY a.id", Long.class).getResultList();
            }
        });
    }

    // ------------------------------------------------------------------
    // Parcours internes
    // ------------------------------------------------------------------

    private void parcoursParesseuxSansCache() {
        enLecture(new Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                em.unwrap(Session.class).setCacheMode(CacheMode.IGNORE);
                List<Auteur> auteurs = em.createQuery(REQUETE_AUTEURS, Auteur.class).getResultList();
                int total = 0;
                for (Auteur auteur : auteurs) {
                    total += auteur.getLivres().size();
                }
                return total;
            }
        });
    }

    private void parcoursJoinFetch() {
        enLecture(new Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                List<Auteur> auteurs = em.createQuery(REQUETE_JOIN_FETCH, Auteur.class).getResultList();
                int total = 0;
                for (Auteur auteur : auteurs) {
                    total += auteur.getLivres().size();
                }
                return total;
            }
        });
    }

    private void parcoursEntityGraphNomme() {
        enLecture(new Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                EntityGraph<?> graphe = em.getEntityGraph(Auteur.GRAPHE_LIVRES);
                TypedQuery<Auteur> requete = em.createQuery(REQUETE_AUTEURS, Auteur.class);
                requete.setHint("javax.persistence.fetchgraph", graphe);
                List<Auteur> auteurs = requete.getResultList();
                int total = 0;
                for (Auteur auteur : auteurs) {
                    total += auteur.getLivres().size();
                }
                return total;
            }
        });
    }

    private void parcoursGrapheProgrammatique() {
        enLecture(new Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                EntityGraph<Auteur> graphe = em.createEntityGraph(Auteur.class);
                graphe.addAttributeNodes("livres");
                TypedQuery<Auteur> requete = em.createQuery(REQUETE_AUTEURS, Auteur.class);
                requete.setHint("javax.persistence.loadgraph", graphe);
                List<Auteur> auteurs = requete.getResultList();
                int total = 0;
                for (Auteur auteur : auteurs) {
                    total += auteur.getLivres().size();
                }
                return total;
            }
        });
    }

    private void parcoursParCle(final List<Long> identifiants) {
        enLecture(new Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                int total = 0;
                for (Long id : identifiants) {
                    Auteur auteur = em.find(Auteur.class, id);
                    if (auteur != null) {
                        total += auteur.getLivres().size();
                    }
                }
                return total;
            }
        });
    }

    private int tailleListe(final String jpql) {
        return enLecture(new Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                return em.createQuery(jpql, Auteur.class).getResultList().size();
            }
        });
    }

    private <T> T enLecture(Function<EntityManager, T> travail) {
        EntityManager em = fabrique.createEntityManager();
        try {
            return travail.apply(em);
        } finally {
            em.close();
        }
    }
}
