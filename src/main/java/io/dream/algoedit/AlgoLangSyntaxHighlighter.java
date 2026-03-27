package io.dream.algoedit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class AlgoLangSyntaxHighlighter {

    private static final List<String> KEYWORDS = List.of(
            "Algorithme", "Variables", "Debut", "Fin",
            "Ecrire", "Lire",
            "Si", "alors", "Sinon", "FinSi",
            "TantQue", "faire", "FinTantQue",
            "Pour", "allant", "de", "a", "FinPour",
            "Repeter", "Jusqu",
            "Fonction", "Procedure", "Retourner",
            "et", "ou", "non");

    private static final List<String> TYPES = List.of(
            "entier", "reel", "chaine_charactere", "caractere", "booleen", "tableau");

    private static final List<String> BOOLEANS = List.of("vrai", "faux", "nil");

    private static String wordAlt(List<String> words) {
        return words.stream()
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("(?!x)x");
    }

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*)"
                    + "|(?<STRING>\"[^\"]*\"|'[^']*')"
                    + "|(?<KEYWORD>\\b(" + wordAlt(KEYWORDS) + ")\\b)"
                    + "|(?<TYPE>\\b(" + wordAlt(TYPES) + ")\\b)"
                    + "|(?<BOOLEAN>\\b(" + wordAlt(BOOLEANS) + ")\\b)"
                    + "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)"
                    + "|(?<OPERATOR><-|==|!=|<=|>=|[<>]=?|[+\\-*/!]|=)"
                    + "|(?<PAREN>[()])"
                    + "|(?<BRACE>[{}])"
                    + "|(?<BRACKET>[\\[\\]])"
                    + "|(?<SEMICOLON>;)"
                    + "|(?<COLON>:)"
                    + "|(?<COMMA>,)");

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        while (matcher.find()) {
            String styleClass = styleFor(matcher);
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }

    private static String styleFor(Matcher m) {
        if (m.group("COMMENT") != null)
            return "comment";
        if (m.group("STRING") != null)
            return "string";
        if (m.group("KEYWORD") != null)
            return "keyword";
        if (m.group("TYPE") != null)
            return "type";
        if (m.group("BOOLEAN") != null)
            return "boolean";
        if (m.group("NUMBER") != null)
            return "number";
        if (m.group("OPERATOR") != null)
            return "operator";
        if (m.group("PAREN") != null)
            return "paren";
        if (m.group("BRACE") != null)
            return "brace";
        if (m.group("BRACKET") != null)
            return "bracket";
        if (m.group("SEMICOLON") != null)
            return "semicolon";
        if (m.group("COLON") != null)
            return "colon";
        if (m.group("COMMA") != null)
            return "comma";
        return "identifier";
    }

    public static List<String> getKeywords() {
        return KEYWORDS;
    }

    public static List<String> getTypes() {
        return TYPES;
    }

    public static List<String> getAllCompletions() {
        return List.of(
                KEYWORDS.toArray(new String[0]),
                TYPES.toArray(new String[0]),
                BOOLEANS.toArray(new String[0])).stream().flatMap(java.util.Arrays::stream).toList();
    }

    public static Map<String, String> getCodeTemplates() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("Algorithme",
                "Algorithme NomAlgorithme\nVariables\n    // déclarez vos variables ici\nDebut\n    // code ici\nFin");
        t.put("Si", "Si (condition) alors\n    // ...\nSinon\n    // ...\nFinSi");
        t.put("TantQue", "TantQue (condition) faire\n    // ...\nFinTantQue");
        t.put("Pour", "Pour i allant de 1 a N faire\n    // ...\nFinPour");
        t.put("Fonction", "Fonction NomFonction() : entier\n    // ...\n    Retourner 0\nFin");
        return t;
    }
}
