package ma.emsi.ayatijini.tp2.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.Dependent;

import java.io.Serializable;

/**
 * Classe "métier" liée au LLM choisi (Gemini) : elle s'appuie sur LangChain4j pour envoyer les questions
 * de l'utilisateur à l'API du LLM et pour conserver l'historique de la conversation.
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
     * LangChain4j envoie cet historique avec chaque nouvelle question.
     */
    private ChatMemory chatMemory;

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

        // Le modèle qui représente le LLM (création avec le pattern "builder")
        ChatModel model = GoogleGenAiChatModel.builder()
                .apiKey(llmKey)
                .modelName("gemini-3.5-flash-lite")
                .build();

        // Création de l'assistant, avec une mémoire qui garde jusqu'à 20 messages
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
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
     * Envoie une requête (la question de l'utilisateur) au LLM et retourne sa réponse.
     * L'historique de la conversation est ajouté à la requête par LangChain4j.
     *
     * @param question la question de l'utilisateur.
     * @return la réponse du LLM.
     */
    public String envoyerRequete(String question) {
        return assistant.chat(question);
    }
}
