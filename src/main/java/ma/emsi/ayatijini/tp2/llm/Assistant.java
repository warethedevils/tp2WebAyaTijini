package ma.emsi.ayatijini.tp2.llm;

import dev.langchain4j.service.Result;

/**
 * Interface du service IA. LangChain4j (classe AiServices) fournit l'implémentation de cette interface
 * (un objet "proxy") ; il n'y a donc pas besoin de l'implémenter.
 * Une seule interaction avec le LLM : une question (prompt) est envoyée et le LLM répond.
 * <p>
 * La méthode retourne un Result<String> et pas un String pour pouvoir récupérer, avec la réponse
 * du LLM, le nombre de tokens utilisés par la requête.
 */
public interface Assistant {
  Result<String> chat(String prompt);
}
