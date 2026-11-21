package ma.emsi.ayatijini.tp2.llm;

import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accumule le nombre de tokens utilisés pour toutes les requêtes envoyées au LLM pendant une session entière.
 * <p>
 * Le total ne peut pas être gardé dans le backing bean, de portée view : il serait perdu quand l'utilisateur
 * change de rôle pour le LLM (nouveau chat). Cette classe est donc à part, avec la portée session. Un bean de portée
 * view peut injecter un bean de portée session : tous les beans de portée view d'une même session reçoivent la même
 * instance. Elle est injectée dans LlmClient, qui y ajoute les tokens de chaque requête, et dans le backing bean,
 * qui affiche le total.
 * <p>
 * La classe n'est pas liée à l'interface utilisateur : c'est une classe "métier", utilisable par un service.
 */
@SessionScoped
public class CompteurTokens implements Serializable {

    /**
     * Nombre total de tokens utilisés depuis le début de la session.
     * Un AtomicLong car le total peut être modifié par plusieurs threads (voir le streaming).
     */
    private final AtomicLong total = new AtomicLong();

    /**
     * Ajoute des tokens au total.
     *
     * @param nombre le nombre de tokens utilisés par une requête (ignoré s'il est inconnu, c'est-à-dire null).
     */
    public void ajouter(Integer nombre) {
        if (nombre != null) {
            total.addAndGet(nombre);
        }
    }

    /**
     * Retourne le nombre total de tokens utilisés depuis le début de la session.
     *
     * @return le total.
     */
    public long getTotal() {
        return total.get();
    }
}
