package com.example.service;

import java.util.Collections;
import java.util.List;

/**
 * Resultat d'une campagne de mesure : une duree, un cout en requetes SQL et la
 * trace des instructions reellement emises lors du dernier tour.
 *
 * <p>Objet immuable, ce qui permet de le conserver dans un tableau recapitulatif
 * sans risque de voir les valeurs bouger apres coup.</p>
 */
public final class Mesure {

    private final String libelle;
    private final long millisecondes;
    private final long requetesSql;
    private final int motifsSql;
    private final long[] echantillons;
    private final List<String> traceSql;

    public Mesure(String libelle, long millisecondes, long requetesSql, int motifsSql,
                  long[] echantillons, List<String> traceSql) {
        this.libelle = libelle;
        this.millisecondes = millisecondes;
        this.requetesSql = requetesSql;
        this.motifsSql = motifsSql;
        this.echantillons = echantillons == null ? new long[0] : echantillons.clone();
        this.traceSql = traceSql == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(traceSql);
    }

    public String getLibelle() {
        return libelle;
    }

    public long getMillisecondes() {
        return millisecondes;
    }

    public long getRequetesSql() {
        return requetesSql;
    }

    public int getMotifsSql() {
        return motifsSql;
    }

    public long[] getEchantillons() {
        return echantillons.clone();
    }

    public List<String> getTraceSql() {
        return traceSql;
    }

    public int getNombreDeTours() {
        return echantillons.length;
    }

    @Override
    public String toString() {
        return libelle + " : " + millisecondes + " ms, " + requetesSql + " requete(s) SQL";
    }
}
