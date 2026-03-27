package io.dream.algoedit;

/**
 * AiAssistant — integration bridge between the chat panel and an AI back-end.
 *
 * HOW TO INTEGRATE YOUR MODEL
 * ────────────────────────────
 * Replace the body of {@link #ask(String, String)} with a real HTTP call to
 * your preferred AI API (Anthropic Claude, OpenAI, Ollama, etc.).
 *
 * Example (Anthropic):
 * POST https://api.anthropic.com/v1/messages
 * Headers: x-api-key, anthropic-version, content-type
 * Body: { "model": "claude-3-5-haiku", "max_tokens": 1024, "messages": [...] }
 *
 * This method is always called from a background thread by MainController,
 * so blocking HTTP is fine here. Return the assistant's reply as a plain
 * String.
 */
public class AiAssistant {

    private static final String SYSTEM_PROMPT = "You are an expert AlgoLang programming assistant embedded in algoEdit, "
            +
            "a code editor for the AlgoLang educational pseudocode language. " +
            "When given code context, explain it clearly and concisely in the same language " +
            "the user is writing in. Keep responses focused and practical. " +
            "AlgoLang keywords include: Algorithme, Variables, Debut, Fin, Ecrire, Lire, " +
            "Si/alors/Sinon/FinSi, TantQue/faire/FinTantQue, Pour/allant/de/a/FinPour. " +
            "Types: entier, reel, chaine_charactere, caractere, booleen.";

    /**
     * Sends a question (and optional code context) to the AI model.
     *
     * @param question the user's question from the chat input
     * @param context  the current file content or selected text (may be empty)
     * @return the AI's plain-text response
     */
    public static String ask(String question, String context) {
        // STUB — replace with your real API call
        // Example placeholder responses so the UI is functional out of the box:
        if (question.toLowerCase().contains("explain")) {
            if (context == null || context.isBlank()) {
                return "The editor is empty. Start writing some AlgoLang code and I'll explain it!";
            }
            return buildExplanationStub(context);
        }
        return "This is a placeholder response.\n\n" +
                "To enable real AI responses, open AiAssistant.java and replace the body of " +
                "ask() with an HTTP call to your preferred model API.\n\n" +
                "Your question was:\n\"" + question + "\"";
    }

    // Helpers
    private static String buildExplanationStub(String code) {
        String[] lines = code.split("\n");
        StringBuilder sb = new StringBuilder();
        sb.append("Here is what I can see in your code:\n\n");
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("Algorithme"))
                sb.append("• Declares an algorithm named ")
                        .append(t.replace("Algorithme", "").replace(":", "").trim())
                        .append("\n");
            else if (t.startsWith("Variables"))
                sb.append("• Opens the variable declaration block\n");
            else if (t.startsWith("Debut"))
                sb.append("• Marks the start of the algorithm body\n");
            else if (t.startsWith("Fin") && !t.startsWith("FinSi") && !t.startsWith("FinTantQue"))
                sb.append("• Marks the end of the algorithm\n");
            else if (t.startsWith("Ecrire"))
                sb.append("• Outputs: ").append(t.replace("Ecrire", "").trim()).append("\n");
            else if (t.startsWith("Lire"))
                sb.append("• Reads input into: ").append(t.replace("Lire", "").trim()).append("\n");
            else if (t.startsWith("Si "))
                sb.append("• Conditional check: ").append(t.replace("Si", "").replace("alors", "").trim()).append("\n");
            else if (t.startsWith("TantQue"))
                sb.append("• Loop while: ").append(t.replace("TantQue", "").replace("faire", "").trim()).append("\n");
        }
        sb.append("\nTo get full AI explanations, integrate AiAssistant.java with your model API.");
        return sb.toString();
    }
}
