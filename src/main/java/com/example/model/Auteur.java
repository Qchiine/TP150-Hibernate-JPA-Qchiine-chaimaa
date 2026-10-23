package com.example.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Ecricain du catalogue.
 *
 * <p>La collection de livres est declaree en {@code LAZY} : c'est precisement
 * ce choix (le defaut JPA pour un {@code @OneToMany}) qui rend le probleme N+1
 * observable lorsque l'on parcourt les auteurs sans precaution.</p>
 *
 * <p>L'entite est candidate au cache de second niveau ({@code READ_WRITE}) :
 * les lectures repetees dans des sessions differentes n'atteindront plus la
 * base. Un graphe d'entites nomme est egalement declare pour permettre un
 * chargement anticipe purement declaratif.</p>
 */
@Entity
@Table(name = "auteurs")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(name = Auteur.GRAPHE_LIVRES, attributeNodes = @NamedAttributeNode("livres"))
public class Auteur implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nom du graphe d'entites reutilisable par les couches de service. */
    public static final String GRAPHE_LIVRES = "Auteur.livres";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 120)
    private String nom;

    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Livre> livres = new ArrayList<>();

    public Auteur() {
    }

    public Auteur(String nom) {
        this.nom = nom;
    }

    /**
     * Ajoute un livre en maintenant les deux cotes de l'association.
     * Le parametre {@code null} est ignore silencieusement afin que les jeux de
     * donnees de test puissent etre construits sans garde supplementaire.
     */
    public void ajouterLivre(Livre livre) {
        if (livre == null) {
            return;
        }
        livres.add(livre);
        livre.setAuteur(this);
    }

    /** Retire un livre et casse le lien inverse (utilise par orphanRemoval). */
    public void retirerLivre(Livre livre) {
        if (livre == null) {
            return;
        }
        if (livres.remove(livre)) {
            livre.setAuteur(null);
        }
    }

    public int nombreDeLivres() {
        return livres.size();
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    /** Vue non modifiable : protege l'invariant « un livre -> un seul auteur ». */
    public List<Livre> getLivres() {
        return Collections.unmodifiableList(livres);
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof Auteur)) {
            return false;
        }
        Auteur a = (Auteur) autre;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Auteur#" + id + " (" + nom + ", " + livres.size() + " livre(s))";
    }
}
