package com.example.util;

import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

/**
 * Point d'acces unique a l'unite de persistance {@code hibernate-cache-pu}.
 *
 * <p>La fabrique est creee une seule fois (initialisation paresseuse doublement
 * verrouillee) : c'est elle, et non la session, qui porte le cache de second
 * niveau. Recreer une fabrique entre deux mesures reviendrait donc a vider le
 * cache et fausserait toutes les comparaisons de performances.</p>
 */
public final class Persistance {

    public static final String UNITE = "hibernate-cache-pu";

    private static volatile EntityManagerFactory fabrique;

    private Persistance() {
    }

    public static EntityManagerFactory fabrique() {
        EntityManagerFactory locale = fabrique;
        if (locale == null) {
            synchronized (Persistance.class) {
                locale = fabrique;
                if (locale == null) {
                    locale = Persistence.createEntityManagerFactory(UNITE);
                    fabrique = locale;
                    Runtime.getRuntime().addShutdownHook(new Thread(Persistance::fermer, "arret-persistance"));
                }
            }
        }
        return locale;
    }

    /** Ferme la fabrique si elle a ete ouverte (idempotent). */
    public static synchronized void fermer() {
        EntityManagerFactory locale = fabrique;
        fabrique = null;
        if (locale != null && locale.isOpen()) {
            locale.close();
        }
    }

    /**
     * Execute un traitement dans une transaction explicite.
     * Toute exception provoque un rollback puis est propagee a l'appelant.
     */
    public static <T> T dansUneTransaction(Function<EntityManager, T> travail) {
        EntityManager em = fabrique().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T resultat = travail.apply(em);
            tx.commit();
            return resultat;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                try {
                    tx.rollback();
                } catch (RuntimeException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /** Variante en lecture seule, sans transaction explicite. */
    public static <T> T enLecture(Function<EntityManager, T> travail) {
        EntityManager em = fabrique().createEntityManager();
        try {
            return travail.apply(em);
        } finally {
            em.close();
        }
    }
}
