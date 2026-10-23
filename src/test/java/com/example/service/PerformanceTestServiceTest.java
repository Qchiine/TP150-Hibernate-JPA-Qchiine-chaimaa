package com.example.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.example.util.Persistance;

/**
 * Reproduit les resultats attendus par l'enonce : N+1 visible, une seule requete
 * avec {@code JOIN FETCH}, une seule avec un graphe d'entites, et zero requete
 * lorsqu'une lecture par cle est servie par le cache de second niveau.
 */
public class PerformanceTestServiceTest {

    private static EntityManagerFactory fabrique;
    private static PerformanceTestService service;

    @BeforeClass
    public static void ouvrirLaPersistance() {
        fabrique = Persistance.fabrique();
        service = new PerformanceTestService(fabrique);
        new DataInitService(fabrique).initData();
    }

    @AfterClass
    public static void fermerLaPersistance() {
        Persistance.fermer();
    }

    @Before
    public void repartirDUnCacheFroid() {
        service.getAuditCache().evincerTout();
        service.getAuditCache().reinitialiserCompteurs();
        CompteurRequetes.reinitialiser();
    }

    @Test
    public void leParcoursParesseuxCouteUneRequeteParAuteurEnPlusDeLaListe() {
        Mesure mesure = service.testPerformanceSansCache();

        assertEquals(1L + DataInitService.AUTEURS_PAR_DEFAUT, mesure.getRequetesSql());
        assertTrue(DetecteurNPlus1.analyser(DataInitService.AUTEURS_PAR_DEFAUT, mesure).isNPlus1());
    }

    @Test
    public void leParcoursParesseuxNeConsultePasLeCacheDeSecondNiveau() {
        service.testPerformanceSansCache();

        assertEquals(0L, service.getAuditCache().hits(AuditCache.REGION_AUTEUR));
    }

    @Test
    public void joinFetchRameneToutEnUneSeuleRequete() {
        Mesure mesure = service.testPerformanceAvecJoinFetch();

        assertEquals(1L, mesure.getRequetesSql());
        assertEquals("OK (1 requete)", DetecteurNPlus1.analyser(10, mesure).getConclusion());
    }

    @Test
    public void leGrapheDEntitesNommeEviteAussiLesRequetesSupplementaires() {
        Mesure mesure = service.testPerformanceAvecEntityGraph();

        assertEquals(1L, mesure.getRequetesSql());
    }

    @Test
    public void leGrapheDEntitesProgrammatiqueEviteAussiLesRequetesSupplementaires() {
        Mesure mesure = service.testPerformanceAvecGrapheProgrammatique();

        assertEquals(1L, mesure.getRequetesSql());
    }

    @Test
    public void joinFetchSansDistinctGonfleLaListeDesAuteurs() {
        int[] tailles = service.comparerDistinct();

        assertEquals(DataInitService.AUTEURS_PAR_DEFAUT * DataInitService.LIVRES_PAR_AUTEUR, tailles[0]);
        assertEquals(DataInitService.AUTEURS_PAR_DEFAUT, tailles[1]);
    }

    @Test
    public void laRelectureParCleEstEntierementServieParLeCache() {
        Mesure mesure = service.testRelectureParCle();

        assertEquals(0L, mesure.getRequetesSql());
        assertTrue(service.getAuditCache().hits(AuditCache.REGION_AUTEUR) > 0L);
    }

    @Test
    public void lesIdentifiantsDesAuteursSontTousDistincts() {
        List<Long> identifiants = service.identifiantsAuteurs();

        assertEquals(DataInitService.AUTEURS_PAR_DEFAUT, identifiants.size());
        assertEquals(identifiants.size(), new ArrayList<Long>(new java.util.LinkedHashSet<Long>(identifiants)).size());
        assertNotNull(identifiants.get(0));
    }

    @Test
    public void leCacheDeSecondNiveauRendLaRelecturePlusRapideQueLeParcoursComplet() {
        Mesure jointure = service.testPerformanceAvecJoinFetch();
        Mesure cache = service.testPerformanceAvecCache();

        assertTrue("la lecture en cache doit couter moins de requetes : " + cache.getRequetesSql(),
                cache.getRequetesSql() <= jointure.getRequetesSql());
    }
}
