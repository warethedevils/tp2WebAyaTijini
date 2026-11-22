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
     * Un AtomicLong car le total est modifié par un autre thread que celui de la requête HTTP (voir le streaming).
     */
    private final AtomicLong total = new AtomicLong();

    /**
     * Retourne l'objet qui contient le total, pour pouvoir y ajouter des tokens.
     * <p>
     * Avec le streaming, les tokens sont comptés quand le LLM a fini de répondre, dans un thread différent de
     * celui de la requête HTTP. Dans ce thread le contexte de la session n'est pas actif : le bean de portée
     * session ne peut donc pas être utilisé directement. Il faut appeler cette méthode dans le thread de la requête
     * HTTP, et garder l'objet retourné pour l'utiliser plus tard dans l'autre thread.
     *
     * @return l'accumulateur du nombre total de tokens de la session.
     */
    public AtomicLong getAccumulateur() {
        return total;
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
