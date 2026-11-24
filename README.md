# TP 2 - Application Web de chat avec LangChain4j et Gemini (Aya Tijini)

Application Web (Jakarta EE 11, JSF et PrimeFaces) qui reprend l'interface du TP 0 : l'utilisateur choisit un rôle
« système », pose des questions à un LLM (**Gemini**) et la conversation s'affiche à droite de la page.
Contrairement au TP 1, l'API du LLM n'est plus appelée avec des documents JSON écrits à la main :
l'application utilise **LangChain4j**, qui s'occupe des requêtes, de l'historique de la conversation et du format
propre à chaque LLM.

Les tests qui ont préparé ce TP sont dans un autre dépôt : `tp2TestsAyaTijini`.

## Versions utilisées

| Élément | Version |
|---|---|
| Java (JDK Temurin) | 25 |
| Jakarta EE (Web Profile) | 11 |
| Serveur d'application | Payara 7.2026.10 |
| PrimeFaces | 16.0.1 |
| LangChain4j | 1.22.0 (`langchain4j-google-genai` 1.22.0-beta32) |
| Modèle Gemini | `gemini-3.5-flash-lite` |

## Lancer l'application

1. Créer une clé pour l'API de Gemini (Google AI Studio) et la mettre dans la variable d'environnement
   **`GEMINI_KEY`** (la clé n'est jamais écrite dans le code ; elle est lue dans le constructeur de `LlmClient`).
   Redémarrer Payara ou l'IDE pour que la variable soit prise en compte.
2. Construire le WAR : `mvn clean package` (génère `target/tp2WebAyaTijini-1.0-SNAPSHOT.war`).
3. Déployer le WAR sur Payara (par exemple avec IntelliJ IDEA, ou avec
   `asadmin deploy target/tp2WebAyaTijini-1.0-SNAPSHOT.war`).
4. Ouvrir http://localhost:8080/tp2WebAyaTijini-1.0-SNAPSHOT/ (la page `index.xhtml` est la page d'accueil).

Sous Windows, si Java refuse le certificat de l'API de Gemini (« PKIX path building failed »), ajouter l'option JVM
`-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT` au serveur d'application.

## Organisation du code

| Fichier | Rôle |
|---|---|
| `src/main/webapp/index.xhtml` | Page JSF : rôle système, question, réponse et conversation |
| `src/main/webapp/resources/` | Fichiers CSS et JavaScript utilisés par la page |
| `.../tp2/jsf/Bb.java` | Backing bean (`@ViewScoped`) : état de la page ; il délègue le travail avec le LLM à `LlmClient` |
| `.../tp2/llm/LlmClient.java` | Classe « métier » (`@Dependent`) : crée le modèle Gemini, l'assistant (`AiServices`) et la mémoire de la conversation |
| `.../tp2/llm/Assistant.java` | Interface du service IA ; LangChain4j en fournit l'implémentation |

Enchaînement : `Bb.envoyer()` → `LlmClient.envoyerRequete(question)` → `Assistant.chat(question)` (proxy créé par
LangChain4j) → API de Gemini.

## L'historique de la conversation

L'API des LLM est sans état : c'est le client qui doit renvoyer l'historique à chaque requête. Avec LangChain4j,
c'est la mémoire (`ChatMemory`, qui garde les 20 derniers messages) qui s'en charge. Le rôle système choisi par
l'utilisateur est ajouté à cette mémoire sous la forme d'un `SystemMessage` au début de la conversation
(la mémoire est vidée avant, car le contexte change complètement). « Nouveau chat » crée un nouveau backing bean,
donc une nouvelle mémoire.
