package io.dream.algoedit;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced EditorTab with RichTextFX CodeArea, syntax highlighting,
 * line numbers, and auto-completion.
 */
public class EditorTab extends Tab {

    private final CodeArea codeArea;
    private File file;
    private boolean modified;
    private final String originalTitle;
    private Popup autoCompletePopup;
    private ListView<String> suggestionList;

    public EditorTab(String title) {
        super(title);
        this.originalTitle = title;
        this.modified = false;

        // Create code area with RichTextFX
        codeArea = new CodeArea();
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 14px;");

        // Add line numbers with custom styling
        IntFunction<javafx.scene.Node> numberFactory = LineNumberFactory.get(codeArea);
        IntFunction<javafx.scene.Node> graphicFactory = line -> {
            HBox hbox = new HBox(
                    numberFactory.apply(line)
            );
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 0 5 0 5;");
            return hbox;
        };
        codeArea.setParagraphGraphicFactory(graphicFactory);

        // Enable syntax highlighting with a small delay to avoid performance issues
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(50))
                .subscribe(ignore -> codeArea.setStyleSpans(0, AlgoLangSyntaxHighlighter.computeHighlighting(codeArea.getText())));

        // Track modifications
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!modified && oldText != null && !oldText.equals(newText)) {
                setModified(true);
            }
        });

        // Setup auto-completion
        setupAutoCompletion();

        // Wrap in StackPane
        StackPane container = new StackPane(codeArea);
        setContent(container);
        setClosable(true);
    }

    /**
     * Setup auto-completion popup.
     */
    private void setupAutoCompletion() {
        autoCompletePopup = new Popup();
        autoCompletePopup.setAutoHide(true);

        suggestionList = new ListView<>();
        suggestionList.setPrefHeight(150);
        suggestionList.setPrefWidth(250);
        suggestionList.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        // Handle suggestion selection
        suggestionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                insertSuggestion();
            }
        });

        suggestionList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                insertSuggestion();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                autoCompletePopup.hide();
                codeArea.requestFocus();
                event.consume();
            }
        });

        autoCompletePopup.getContent().add(suggestionList);

        // Trigger auto-completion on Ctrl+Space
        codeArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE && event.isControlDown()) {
                showAutoComplete();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && autoCompletePopup.isShowing()) {
                autoCompletePopup.hide();
                event.consume();
            }
        });

        // Trigger on typing (optional - can be disabled if too intrusive)
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (autoCompletePopup.isShowing()) {
                String currentWord = getCurrentWord();
                if (currentWord.length() < 2) {
                    autoCompletePopup.hide();
                } else {
                    updateAutoComplete(currentWord);
                }
            }
        });
    }

    /**
     * Show auto-completion popup with suggestions.
     */
    private void showAutoComplete() {
        String currentWord = getCurrentWord();
        if (currentWord.length() < 1) {
            autoCompletePopup.hide();
            return;
        }

        updateAutoComplete(currentWord);
    }

    /**
     * Update auto-complete suggestions based on current word.
     */
    private void updateAutoComplete(String currentWord) {
        // Get matching keywords and templates
        List<String> suggestions = new ArrayList<>();

        // Add matching keywords
        for (String keyword : AlgoLangSyntaxHighlighter.getKeywords()) {
            if (keyword.toLowerCase().startsWith(currentWord.toLowerCase())) {
                suggestions.add(keyword);
            }
        }

        // Add matching templates
        for (Map.Entry<String, String> entry : AlgoLangSyntaxHighlighter.getCodeTemplates().entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(currentWord.toLowerCase())) {
                suggestions.add(entry.getKey() + " (template)");
            }
        }

        if (suggestions.isEmpty()) {
            autoCompletePopup.hide();
            return;
        }

        suggestionList.getItems().setAll(suggestions);
        suggestionList.getSelectionModel().selectFirst();

        // Position popup near caret
        Optional<Bounds> caretBounds = codeArea.getCaretBounds();
        if (caretBounds.isPresent()) {
            Bounds bounds = caretBounds.get();
            if (!autoCompletePopup.isShowing()) {
                autoCompletePopup.show(
                        codeArea,
                        bounds.getMaxX(),
                        bounds.getMaxY()
                );
            }
        }
    }

    /**
     * Insert selected suggestion into code area.
     */
    private void insertSuggestion() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        autoCompletePopup.hide();

        // Remove " (template)" suffix if present
        boolean isTemplate = selected.endsWith(" (template)");
        if (isTemplate) {
            selected = selected.replace(" (template)", "");
        }

        String currentWord = getCurrentWord();
        int caretPos = codeArea.getCaretPosition();
        int wordStart = caretPos - currentWord.length();

        // Replace current word with suggestion
        codeArea.replaceText(wordStart, caretPos, "");

        if (isTemplate) {
            // Insert template
            String template = AlgoLangSyntaxHighlighter.getCodeTemplates().get(selected);
            if (template != null) {
                // Find first placeholder
                Pattern placeholder = Pattern.compile("\\$\\{([^}]+)\\}");
                Matcher matcher = placeholder.matcher(template);

                if (matcher.find()) {
                    String firstPlaceholder = matcher.group(0);
                    codeArea.insertText(wordStart, template);

                    // Select first placeholder
                    int placeholderStart = wordStart + template.indexOf(firstPlaceholder);
                    codeArea.selectRange(placeholderStart, placeholderStart + firstPlaceholder.length());
                } else {
                    codeArea.insertText(wordStart, template);
                }
            }
        } else {
            // Insert keyword
            codeArea.insertText(wordStart, selected);
        }

        codeArea.requestFocus();
    }

    /**
     * Get the current word being typed at caret position.
     */
    private String getCurrentWord() {
        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();

        if (caretPos == 0 || caretPos > text.length()) return "";

        int start = caretPos - 1;
        while (start >= 0 && Character.isLetterOrDigit(text.charAt(start))) {
            start--;
        }
        start++;

        return text.substring(start, caretPos);
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public String getEditorContent() {
        return codeArea.getText();
    }

    public void setEditorContent(String text) {
        codeArea.replaceText(text);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        updateTitle();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
        updateTitle();
    }

    private void updateTitle() {
        String title = file != null ? file.getName() : originalTitle;
        if (modified) {
            title = "* " + title;
        }
        setText(title);
    }
}
