package com.example.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.example.util.Persistance;

/**
 * Verifie l'initialisation du catalogue sur une base H2 en memoire.
 *
 * <p>Les tests partagent la meme fabrique : elle porte le cache de second niveau
 * et les statistiques, la recreer a chaque test reviendrait a mesurer un cache
 * toujours froid.</p>
 */
public class DataInitServiceTest {

    private static EntityManagerFactory fabrique;

    @BeforeClass
    public static void ouvrirLaPersistance() {
        fabrique = Persistance.fabrique();
    }

    @AfterClass
    public static void fermerLaPersistance() {
        Persistance.fermer();
    }

    @Before
    public void viderLeCatalogue() {
        Persistance.dansUneTransaction(new java.util.function.Function<EntityManager, Void>() {
            @Override
            public Void apply(EntityManager em) {
                em.createQuery("delete from Livre").executeUpdate();
                em.createQuery("delete from Auteur").executeUpdate();
                return null;
            }
        });
    }

    @Test
    public void leJeuDeDonneesStandardContientDixAuteursEtCinquanteLivres() {
        DataInitService service = new DataInitService(fabrique);

        int livres = service.initData();

        assertEquals(50, livres);
        assertEquals(10L, service.compterAuteurs());
        assertEquals(50L, service.compterLivres());
    }

    @Test
    public void lesVolumesPeuventEtreAdaptesAuScenarioTeste() {
        DataInitService service = new DataInitService(fabrique);

        assertEquals(12, service.initialiser(4, 3));
        assertEquals(4L, service.compterAuteurs());
        assertEquals(12L, service.compterLivres());
    }

    @Test
    public void chaqueAuteurPorteExactementLeNombreDeLivresDemande() {
        DataInitService service = new DataInitService(fabrique);
        service.initialiser(3, 2);

        Integer maximum = Persistance.enLecture(new java.util.function.Function<EntityManager, Integer>() {
            @Override
            public Integer apply(EntityManager em) {
                return em.createQuery("select max(size(a.livres)) from Auteur a", Integer.class).getSingleResult();
            }
        });

        assertEquals(2, maximum.intValue());
    }

    @Test
    public void lesTitresRendentChaqueLivreIdentifiable() {
        DataInitService service = new DataInitService(fabrique);
        service.initialiser(2, 2);

        java.util.List<String> titres = Persistance.enLecture(
                new java.util.function.Function<EntityManager, java.util.List<String>>() {
                    @Override
                    public java.util.List<String> apply(EntityManager em) {
                        return em.createQuery("select l.titre from Livre l order by l.id", String.class)
                                .getResultList();
                    }
                });

        assertEquals(4, titres.size());
        assertEquals("Livre 1 de l'auteur 1", titres.get(0));
        assertEquals("Livre 2 de l'auteur 2", titres.get(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unCatalogueVideEstRefuse() {
        new DataInitService(fabrique).initialiser(0, 5);
    }

    @Test
    public void leServiceSansFabriqueUtiliseLUniteParDefaut() {
        assertTrue(new DataInitService().initData() > 0);
    }
}
