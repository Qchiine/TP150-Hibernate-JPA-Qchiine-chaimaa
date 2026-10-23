package com.example.service;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

/**
 * Lecture des compteurs du cache de second niveau.
 *
 * <p>Sans ces compteurs, « le cache fonctionne » reste une impression. Avec eux,
 * on peut chiffrer les hits et les misses par region et verifier que la region
 * de collection est bien sollicitee, et pas seulement celle des entites.</p>
 */
public final class AuditCache {

    public static final String REGION_AUTEUR = "com.example.model.Auteur";
    public static final String REGION_LIVRE = "com.example.model.Livre";
    public static final String REGION_LIVRES_AUTEUR = "com.example.model.Auteur.livres";

    private final EntityManagerFactory fabrique;

    public AuditCache(EntityManagerFactory fabrique) {
        if (fabrique == null) {
            throw new IllegalArgumentException("La fabrique d'entites est obligatoire.");
        }
        this.fabrique = fabrique;
    }

    private Statistics statistiques() {
        return fabrique.unwrap(SessionFactory.class).getStatistics();
    }

    /** Vide entierement le cache de second niveau (les statistiques ne sont pas remises a zero). */
    public void evincerTout() {
        fabrique.getCache().evictAll();
    }

    public void reinitialiserCompteurs() {
        statistiques().clear();
    }

    public long hits(String region) {
        return statistiquesDe(region).getHitCount();
    }

    public long misses(String region) {
        return statistiquesDe(region).getMissCount();
    }

    public long puts(String region) {
        return statistiquesDe(region).getPutCount();
    }

    /**
     * Nombre d'entrees presentes en memoire pour la region demandee.
     *
     * <p>Le fournisseur JCache n'expose pas toujours ce compteur : la valeur
     * negative renvoyee par Hibernate signifie « inconnu » et est traduite
     * telle quelle plutot que presentee comme un nombre d'entrees.</p>
     */
    public String entreesEnMemoire(String region) {
        long nombre = statistiquesDe(region).getElementCountInMemory();
        return nombre < 0L ? "n/d" : Long.toString(nombre);
    }

    public boolean contientUnAuteur(Long id) {
        return fabrique.getCache().contains(com.example.model.Auteur.class, id);
    }

    /** Taux de reussite du cache sur une region, exprime en pourcentage arrondi. */
    public long tauxDeReussite(String region) {
        CacheRegionStatistics stats = statistiquesDe(region);
        long total = stats.getHitCount() + stats.getMissCount();
        if (total == 0L) {
            return 0L;
        }
        return Math.round((stats.getHitCount() * 100.0d) / total);
    }

    private CacheRegionStatistics statistiquesDe(String region) {
        CacheRegionStatistics stats = statistiques().getDomainDataRegionStatistics(region);
        if (stats == null) {
            throw new IllegalStateException("Region de cache inconnue : " + region);
        }
        return stats;
    }
}
