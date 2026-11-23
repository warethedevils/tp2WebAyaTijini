/* Copier le contenu d'un textarea dans le clipboard */
async function copyToClipboard(idTextArea) {
    var textArea = document.getElementById("form:" + idTextArea);
    if(textArea) {
        try {
            await navigator.clipboard.writeText(textArea.value);
        } catch (err) {
            console.error("Erreur lors de la copie : ", err)
        }
    }
    // Cet ancien code est deprecated :
    // textArea.select();
    // document.execCommand('copy');
}

/* Effacer la dernière question et la dernière réponse */
function toutEffacer() {
    document.getElementById("form:question").value = "";
    document.getElementById("form:reponse").value = "";
}

/* ---------- Streaming de la réponse du LLM avec un websocket ---------- */

/* Appelée à chaque événement de l'appel Ajax qui envoie la question.
   Au début de l'envoi, la zone de la réponse est vidée pour recevoir la nouvelle réponse. */
function evenementAjax(data) {
    if (data.status === "begin") {
        document.getElementById("form:reponse").value = "";
    }
}

/* Appelée à chaque message envoyé par le serveur dans le websocket (channel "chat").
   - un texte : une réponse partielle du LLM, ajoutée à la fin de la zone de la réponse ;
   - un objet avec "conversation" et "tokens" : la réponse est terminée, la conversation et le nombre
     de tokens utilisés depuis le début sont mis à jour ;
   - un objet avec "erreur" : un problème est survenu avec l'API du LLM. */
function socketListener(message, channel, event) {
    var zoneReponse = document.getElementById("form:reponse");
    if (typeof message === "string") {
        zoneReponse.value += message;
        zoneReponse.scrollTop = zoneReponse.scrollHeight;
    } else if (message.erreur) {
        zoneReponse.value = message.erreur;
    } else {
        var zoneConversation = document.getElementById("form:conversation");
        zoneConversation.value = message.conversation;
        zoneConversation.scrollTop = zoneConversation.scrollHeight;
        document.getElementById("form:nombreTokens").textContent =
            "Tokens utilisés depuis le début : " + message.tokens;
    }
}
