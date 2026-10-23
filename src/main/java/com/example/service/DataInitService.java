package com.example.service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.example.model.Auteur;
import com.example.model.Livre;
import com.example.util.Persistance;

/**
 * Alimente la base avec un catalogue representatif.
 *
 * <p>Par defaut : 10 auteurs de 5 livres, soit 50 livres. Ce volume est
 * volontairement modeste mais suffisant pour que le N+1 produise
 * 1 + 10 = 11 requetes, ce qui rend l'ecart avec {@code JOIN FETCH} immediatement
 * lisible.</p>
 */
public class DataInitService {

    public static final int AUTEURS_PAR_DEFAUT = 10;
    public static final int LIVRES_PAR_AUTEUR = 5;

    private final EntityManagerFactory fabrique;

    public DataInitService() {
        this(Persistance.fabrique());
    }

    public DataInitService(EntityManagerFactory fabrique) {
        if (fabrique == null) {
            throw new IllegalArgumentException("La fabrique d'entites est obligatoire.");
        }
        this.fabrique = fabrique;
    }

    /** Insere le jeu de donnees standard (10 auteurs, 5 livres chacun). */
    public int initData() {
        return initialiser(AUTEURS_PAR_DEFAUT, LIVRES_PAR_AUTEUR);
    }

    /**
     * Insere {@code nombreAuteurs} auteurs portant chacun {@code livresParAuteur}
     * livres, dans une transaction unique.
     *
     * @return le nombre de livres inseres
     */
    public int initialiser(final int nombreAuteurs, final int livresParAuteur) {
        if (nombreAuteurs < 1 || livresParAuteur < 1) {
            throw new IllegalArgumentException("Il faut au moins un auteur et un livre.");
        }
        return Persistance.dansUneTransaction(new java.util.function.Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                for (int i = 1; i <= nombreAuteurs; i++) {
                    Auteur auteur = new Auteur("Auteur " + i);
                    for (int j = 1; j <= livresParAuteur; j++) {
                        String titre = "Livre " + j + " de l'auteur " + i;
                        auteur.ajouterLivre(new Livre(titre));
                    }
                    em.persist(auteur);
                }
                return nombreAuteurs * livresParAuteur;
            }
        });
    }

    public long compterAuteurs() {
        return compter("select count(a) from Auteur a");
    }

    public long compterLivres() {
        return compter("select count(l) from Livre l");
    }

    private long compter(final String jpql) {
        return Persistance.enLecture(new java.util.function.Function<EntityManager, Long>() {
            @Override
            public Long apply(EntityManager em) {
                return em.createQuery(jpql, Long.class).getSingleResult();
            }
        });
    }
}
