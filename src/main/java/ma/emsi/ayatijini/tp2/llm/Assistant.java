package ma.emsi.ayatijini.tp2.llm;

import dev.langchain4j.service.TokenStream;

/**
 * Interface du service IA. LangChain4j (classe AiServices) fournit l'implémentation de cette interface
 * (un objet "proxy") ; il n'y a donc pas besoin de l'implémenter.
 * Une seule interaction avec le LLM : une question (prompt) est envoyée et le LLM répond.
 * <p>
 * La méthode retourne un TokenStream (et pas un String) pour recevoir la réponse au fur et à mesure
 * de sa génération par le LLM (streaming) : l'appelant configure le TokenStream, puis le démarre.
 */
public interface Assistant {
  TokenStream chat(String prompt);
}
