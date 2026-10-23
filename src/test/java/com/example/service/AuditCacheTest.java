package com.example.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.example.model.Auteur;
import com.example.util.Persistance;

/** Verifie la lecture des compteurs du cache de second niveau, region par region. */
public class AuditCacheTest {

    private static EntityManagerFactory fabrique;
    private static AuditCache audit;

    @BeforeClass
    public static void ouvrirLaPersistance() {
        fabrique = Persistance.fabrique();
        audit = new AuditCache(fabrique);
        new DataInitService(fabrique).initData();
    }

    @AfterClass
    public static void fermerLaPersistance() {
        Persistance.fermer();
    }

    @Before
    public void repartirDUnCacheFroid() {
        audit.evincerTout();
        audit.reinitialiserCompteurs();
    }

    @Test
    public void laPremiereLectureParClePuisLaSecondeProduisentDesHits() {
        List<Long> identifiants = identifiants();
        lireParCle(identifiants);
        long apresAmorcage = audit.hits(AuditCache.REGION_AUTEUR);

        lireParCle(identifiants);

        assertTrue(audit.hits(AuditCache.REGION_AUTEUR) > apresAmorcage);
        // Cache froid : un miss puis un hit par auteur, soit un taux de 50 %.
        assertEquals(audit.misses(AuditCache.REGION_AUTEUR), audit.hits(AuditCache.REGION_AUTEUR));
        assertEquals(50L, audit.tauxDeReussite(AuditCache.REGION_AUTEUR));
    }

    @Test
    public void lesEntitesSontPlaceesDansLeurRegionDesLaPremiereLecture() {
        lireParCle(identifiants());

        assertTrue(audit.puts(AuditCache.REGION_AUTEUR) > 0L);
        assertTrue(audit.puts(AuditCache.REGION_LIVRE) > 0L);
        assertTrue(audit.puts(AuditCache.REGION_LIVRES_AUTEUR) > 0L);
    }

    @Test
    public void uneEntiteAmorceeEstSignaleeCommePresenteDansLeCache() {
        Long premier = identifiants().get(0);

        assertFalse(audit.contientUnAuteur(premier));

        lireParCle(java.util.Collections.singletonList(premier));

        assertTrue(audit.contientUnAuteur(premier));
    }

    @Test
    public void levictionVideLesCompteursSansCasserLaLectureDesRegions() {
        lireParCle(identifiants());
        audit.evincerTout();

        assertEquals(0L, audit.hits(AuditCache.REGION_AUTEUR));
        assertEquals("0 %", audit.tauxDeReussite(AuditCache.REGION_AUTEUR) + " %");
        assertNotNull(audit.entreesEnMemoire(AuditCache.REGION_AUTEUR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void uneRegionInconnueEstRefusee() {
        audit.hits("com.example.model.Inconnue");
    }

    private static List<Long> identifiants() {
        return Persistance.enLecture(new java.util.function.Function<EntityManager, List<Long>>() {
            @Override
            public List<Long> apply(EntityManager em) {
                return em.createQuery("select a.id from Auteur a order by a.id", Long.class).getResultList();
            }
        });
    }

    private static void lireParCle(List<Long> identifiants) {
        Persistance.enLecture(new java.util.function.Function<EntityManager, Integer>() {
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
}
