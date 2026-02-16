package io.dream.algoedit;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syntax highlighter for AlgoLang based on the provided grammar.
 */
public class AlgoLangSyntaxHighlighter {

    // Keywords from the grammar
    private static final String[] KEYWORDS = {
            "Algorithme", "Variables", "Debut", "Fin",
            "Ecrire", "Lire", "Si", "alors", "Sinon",
            "TantQue", "faire"
    };

    // Data types
    private static final String[] TYPES = {
            "entier", "reel", "chaine_charactere", "caractere", "booleen"
    };

    // Boolean literals
    private static final String[] BOOLEAN_LITERALS = {
            "vrai", "faux", "nil"
    };

    // Operators
    private static final String[] OPERATORS = {
            "<-", "==", "!=", "<=", ">=", "<", ">", "=", "\\+", "-", "\\*", "/", "!"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String TYPE_PATTERN = "\\b(" + String.join("|", TYPES) + ")\\b";
    private static final String BOOLEAN_PATTERN = "\\b(" + String.join("|", BOOLEAN_LITERALS) + ")\\b";
    private static final String OPERATOR_PATTERN = String.join("|", OPERATORS);
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String COLON_PATTERN = ":";
    private static final String COMMA_PATTERN = ",";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN = "\\b\\d+\\.?\\d*\\b";
    private static final String IDENTIFIER_PATTERN = "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<TYPE>" + TYPE_PATTERN + ")"
                    + "|(?<BOOLEAN>" + BOOLEAN_PATTERN + ")"
                    + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<COLON>" + COLON_PATTERN + ")"
                    + "|(?<COMMA>" + COMMA_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<IDENTIFIER>" + IDENTIFIER_PATTERN + ")"
    );

    /**
     * Compute syntax highlighting for the given text.
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("TYPE") != null ? "type" :
                                    matcher.group("BOOLEAN") != null ? "boolean" :
                                            matcher.group("OPERATOR") != null ? "operator" :
                                                    matcher.group("PAREN") != null ? "paren" :
                                                            matcher.group("BRACE") != null ? "brace" :
                                                                    matcher.group("BRACKET") != null ? "bracket" :
                                                                            matcher.group("SEMICOLON") != null ? "semicolon" :
                                                                                    matcher.group("COLON") != null ? "colon" :
                                                                                            matcher.group("COMMA") != null ? "comma" :
                                                                                                    matcher.group("STRING") != null ? "string" :
                                                                                                            matcher.group("COMMENT") != null ? "comment" :
                                                                                                                    matcher.group("NUMBER") != null ? "number" :
                                                                                                                            matcher.group("IDENTIFIER") != null ? "identifier" :
                                                                                                                                    null;

            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    /**
     * Get all keywords for auto-completion.
     */
    public static List<String> getKeywords() {
        List<String> allKeywords = new ArrayList<>();
        allKeywords.addAll(Arrays.asList(KEYWORDS));
        allKeywords.addAll(Arrays.asList(TYPES));
        allKeywords.addAll(Arrays.asList(BOOLEAN_LITERALS));
        return allKeywords;
    }

    /**
     * Get code templates for auto-completion.
     */
    public static Map<String, String> getCodeTemplates() {
        Map<String, String> templates = new LinkedHashMap<>();

        templates.put("algo", "Algorithme: ${name};\nVariables:\n\t${variable}: entier;\nDebut:\n\t${code}\nFin");
        templates.put("si", "Si (${condition}) alors\n\t${statement}\nSinon\n\t${else_statement}");
        templates.put("tantque", "TantQue (${condition}) faire\n\t${statement}");
        templates.put("ecrire", "Ecrire(${expression});");
        templates.put("lire", "Lire(${variable});");
        templates.put("var", "${name}: ${type};");

        return templates;
    }

    /**
     * Find foldable regions in the code (between Debut and Fin).
     */
    public static List<FoldableRegion> findFoldableRegions(String text) {
        List<FoldableRegion> regions = new ArrayList<>();
        String[] lines = text.split("\n");

        Stack<Integer> debutStack = new Stack<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith("Debut")) {
                debutStack.push(i);
            } else if (line.startsWith("Fin") && !debutStack.isEmpty()) {
                int startLine = debutStack.pop();
                regions.add(new FoldableRegion(startLine, i));
            }
        }

        return regions;
    }

    /**
     * Represents a foldable region in the code.
     */
    public static class FoldableRegion {
        private final int startLine;
        private final int endLine;

        public FoldableRegion(int startLine, int endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }

        public int getStartLine() {
            return startLine;
        }

        public int getEndLine() {
            return endLine;
        }
    }
}
