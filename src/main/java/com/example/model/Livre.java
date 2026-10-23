package com.example.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Livre rattache a un auteur.
 *
 * <p>Le cote proprietaire de l'association est porte par cette entite : la cle
 * etrangere {@code auteur_id} vit dans la table {@code livres}. Le chargement
 * de l'auteur est paresseux, ce qui evite un aller-retour inutile lorsque seule
 * la liste des titres est demandee.</p>
 */
@Entity
@Table(name = "livres")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Livre implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false, length = 160)
    private String titre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Auteur auteur;

    public Livre() {
    }

    public Livre(String titre) {
        this.titre = titre;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public Auteur getAuteur() {
        return auteur;
    }

    public void setAuteur(Auteur auteur) {
        this.auteur = auteur;
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof Livre)) {
            return false;
        }
        Livre l = (Livre) autre;
        return id != null && id.equals(l.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Livre#" + id + " (" + titre + ")";
    }
}
