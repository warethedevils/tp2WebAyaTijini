package ma.emsi.ayatijini.tp2.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.google.genai.GoogleGenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Classe "métier" liée au LLM choisi (Gemini) : elle s'appuie sur LangChain4j pour envoyer les questions
 * de l'utilisateur à l'API du LLM et pour conserver l'historique de la conversation.
 * Les réponses du LLM sont reçues en streaming, au fur et à mesure de leur génération.
 * <p>
 * La portée Dependent fait que l'instance injectée dans le backing bean est supprimée en même temps que lui.
 * La classe implémente Serializable car le backing bean (de portée view) l'est, et donc toutes les classes
 * des instances qu'il contient doivent l'être.
 */
@Dependent
public class LlmClient implements Serializable {

    /**
     * Nom de la variable d'environnement qui contient la clé secrète de l'API de Gemini.
     * La clé n'est jamais écrite dans le code.
     */
    private static final String NOM_VARIABLE_CLE = "GEMINI_KEY";

    /**
     * Le rôle système que l'utilisateur a choisi pour l'assistant IA.
     */
    private String systemRole;

    /**
     * Interface du service IA ; son implémentation est fournie par LangChain4j.
     */
    private Assistant assistant;

    /**
     * Mémoire de la conversation. L'API des LLM est sans état : c'est le client qui doit garder
     * l'historique de la conversation (tous les messages échangés depuis le début).
     * LangChain4j envoie cet historique avec chaque nouvelle question, et il ajoute lui-même à la mémoire
     * la question et la réponse quand le LLM a fini de répondre.
     */
    private ChatMemory chatMemory;

    /**
     * Accumule le nombre de tokens utilisés pendant la session (même instance pour toute la session).
     */
    @Inject
    private CompteurTokens compteurTokens;

    /**
     * Total des tokens de la session, récupéré dans le thread de la requête HTTP au moment de l'envoi d'une question.
     * Il sera utilisé dans l'autre thread qui reçoit la fin de la réponse du LLM (voir CompteurTokens).
     */
    private transient AtomicLong totalTokensSession;

    /**
     * Crée le modèle qui représente le LLM, la mémoire de la conversation et l'assistant.
     */
    public LlmClient() {
        // Récupère la clé secrète du LLM dans la variable d'environnement
        String llmKey = System.getenv(NOM_VARIABLE_CLE);
        if (llmKey == null || llmKey.isBlank()) {
            throw new IllegalStateException("La variable d'environnement " + NOM_VARIABLE_CLE
                    + " (clé de l'API de Gemini) n'est pas définie.");
        }

        // Le modèle qui représente le LLM, en version "streaming" (création avec le pattern "builder")
        StreamingChatModel model = GoogleGenAiStreamingChatModel.builder()
                .apiKey(llmKey)
                .modelName("gemini-3.5-flash-lite")
                .build();

        // Création de l'assistant, avec une mémoire qui garde jusqu'à 20 messages
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        this.assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(model)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * Définit le rôle système. Le contexte change complètement avec un nouveau rôle : la mémoire est donc vidée
     * avant d'y ajouter le rôle, sous la forme d'un SystemMessage pour que le LLM en tienne compte.
     *
     * @param systemRole le rôle système choisi par l'utilisateur.
     */
    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
        this.chatMemory.clear();
        this.chatMemory.add(SystemMessage.from(systemRole));
    }

    /**
     * Retourne le rôle système en cours.
     *
     * @return le rôle système.
     */
    public String getSystemRole() {
        return systemRole;
    }

    /**
     * Envoie une requête (la question de l'utilisateur) au LLM. La requête n'est pas encore envoyée :
     * l'appelant doit configurer le TokenStream retourné (actions à effectuer pour chaque réponse partielle,
     * à la fin de la réponse et en cas d'erreur), puis appeler sa méthode start().
     * L'historique de la conversation est ajouté à la requête par LangChain4j.
     *
     * @param question la question de l'utilisateur.
     * @return le flux des réponses partielles du LLM.
     */
    public TokenStream envoyerRequete(String question) {
        // Cette méthode est appelée dans le thread de la requête HTTP, où le contexte de la session est actif :
        // c'est le moment de récupérer l'objet qui accumule les tokens de la session.
        this.totalTokensSession = compteurTokens.getAccumulateur();
        return assistant.chat(question);
    }

    /**
     * Retourne le nombre total de tokens utilisés depuis le début de la session.
     * Peut être appelée dans l'autre thread, celui qui reçoit la fin de la réponse du LLM.
     *
     * @return le total des tokens de la session.
     */
    public long getTotalTokens() {
        return totalTokensSession == null ? 0 : totalTokensSession.get();
    }

    /**
     * À appeler quand le LLM a fini de répondre : ajoute les tokens utilisés par la requête au total de la session.
     * Cette méthode est appelée dans un autre thread que celui de la requête HTTP.
     * <p>
     * La mémoire de la conversation n'a pas besoin d'être mise à jour : LangChain4j y ajoute la réponse du LLM
     * tout seul quand la réponse est terminée.
     *
     * @param reponse la réponse complète du LLM, avec les nombres de tokens utilisés.
     */
    public void addReponse(ChatResponse reponse) {
        Integer tokens = reponse.tokenUsage() == null ? null : reponse.tokenUsage().totalTokenCount();
        if (tokens != null && totalTokensSession != null) {
            totalTokensSession.addAndGet(tokens);
        }
    }
}
