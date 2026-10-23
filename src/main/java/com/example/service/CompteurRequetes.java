package com.example.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Espion SQL branche sur Hibernate via
 * {@code hibernate.session_factory.statement_inspector}.
 *
 * <p>Chaque instruction envoyee au pilote JDBC passe par {@link #inspect(String)}.
 * Le compteur est volontairement statique : Hibernate instancie lui-meme cette
 * classe par reflexion, mais toute la mesure doit converger vers le meme
 * compteur pour rester exploitable depuis les tests et depuis l'application.</p>
 *
 * <p>C'est cet outil qui transforme le « probleme N+1 » en chiffre verifiable :
 * 11 requetes pour 10 auteurs sans optimisation contre 1 seule avec
 * {@code JOIN FETCH}.</p>
 */
public class CompteurRequetes implements StatementInspector {

    private static final AtomicLong TOTAL = new AtomicLong();
    private static final List<String> TRACE = Collections.synchronizedList(new ArrayList<String>());
    private static final int TRACE_MAX = 500;

    /** Constructeur sans argument impose par le mecanisme d'instanciation d'Hibernate. */
    public CompteurRequetes() {
    }

    @Override
    public String inspect(String sql) {
        TOTAL.incrementAndGet();
        if (TRACE.size() < TRACE_MAX) {
            TRACE.add(compacter(sql));
        }
        return sql;
    }

    /** Remet le compteur et la trace a zero : a appeler avant chaque mesure. */
    public static void reinitialiser() {
        TOTAL.set(0L);
        TRACE.clear();
    }

    public static long total() {
        return TOTAL.get();
    }

    public static List<String> trace() {
        synchronized (TRACE) {
            return new ArrayList<>(TRACE);
        }
    }

    /** Nombre de formes de requetes differentes (signature du N+1 : peu de motifs, beaucoup d'executions). */
    public static int motifsDistincts() {
        synchronized (TRACE) {
            Set<String> motifs = new LinkedHashSet<>(TRACE);
            return motifs.size();
        }
    }

    public static long nombreDeSelect() {
        long selects = 0L;
        synchronized (TRACE) {
            for (String sql : TRACE) {
                if (sql.toLowerCase().startsWith("select")) {
                    selects++;
                }
            }
        }
        return selects;
    }

    /** Reduit les espaces pour rendre les requetes comparables entre elles. */
    private static String compacter(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }
}
