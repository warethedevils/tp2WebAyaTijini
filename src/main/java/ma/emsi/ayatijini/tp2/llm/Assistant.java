package ma.emsi.ayatijini.tp2.llm;

/**
 * Interface du service IA. LangChain4j (classe AiServices) fournit l'implémentation de cette interface
 * (un objet "proxy") ; il n'y a donc pas besoin de l'implémenter.
 * Une seule interaction avec le LLM : une question (prompt) est envoyée et le LLM répond.
 */
public interface Assistant {
  String chat(String prompt);
}
