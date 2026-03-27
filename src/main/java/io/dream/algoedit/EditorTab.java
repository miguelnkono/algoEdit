package io.dream.algoedit;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.fxmisc.richtext.CodeArea;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;

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

        // code area
        codeArea = new CodeArea();
        codeArea.setStyle(
                "-fx-font-family: 'Iosevka Charon Mono', 'Cascadia Code', " +
                        "'Consolas', 'Monaco', monospace; -fx-font-size: 17px;");

        // line numbering
        // RichTextFX calls the factory each time a paragraph is added/removed,
        // so the lineIndex is always current — no listener needed.
        codeArea.setParagraphGraphicFactory(lineIndex -> {
            Label numLabel = new Label(String.valueOf(lineIndex + 1));
            numLabel.getStyleClass().add("lineno");
            numLabel.setMinWidth(42);
            numLabel.setPrefWidth(42);
            numLabel.setAlignment(Pos.CENTER_RIGHT);
            numLabel.setPadding(new Insets(0, 10, 0, 6));

            HBox hbox = new HBox(numLabel);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setStyle(
                    "-fx-background-color: #161b22;" +
                            "-fx-border-color: transparent #30363d transparent transparent;" +
                            "-fx-border-width: 0 1 0 0;");
            return hbox;
        });

        // Syntax highlighting (debounced 50 ms)
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(50))
                .subscribe(ignore -> codeArea.setStyleSpans(
                        0, AlgoLangSyntaxHighlighter.computeHighlighting(codeArea.getText())));

        // Track modifications
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!modified && oldText != null && !oldText.equals(newText)) {
                setModified(true);
            }
        });

        // Auto-completion
        setupAutoCompletion();

        // Layout
        StackPane container = new StackPane(codeArea);
        container.setStyle("-fx-background-color: #0d1117;");
        setContent(container);
        setClosable(true);
    }

    // ── Auto-completion
    // ───────────────────────────────────────────────────────────

    private void setupAutoCompletion() {
        autoCompletePopup = new Popup();
        autoCompletePopup.setAutoHide(true);

        suggestionList = new ListView<>();
        suggestionList.setPrefHeight(180);
        suggestionList.setPrefWidth(260);
        suggestionList.setStyle(
                "-fx-background-color: #1c2330;" +
                        "-fx-border-color: #30363d; -fx-border-width: 1;" +
                        "-fx-font-family: 'Iosevka Charon Mono', 'JetBrains Mono','Consolas',monospace; -fx-font-size: 12px;");

        suggestionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2)
                insertSuggestion();
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

        codeArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE && event.isControlDown()) {
                showAutoComplete();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && autoCompletePopup.isShowing()) {
                autoCompletePopup.hide();
                event.consume();
            }
        });

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (autoCompletePopup.isShowing()) {
                String word = getCurrentWord();
                if (word.length() < 2)
                    autoCompletePopup.hide();
                else
                    updateAutoComplete(word);
            }
        });
    }

    private void showAutoComplete() {
        String word = getCurrentWord();
        if (word.isEmpty()) {
            autoCompletePopup.hide();
            return;
        }
        updateAutoComplete(word);
    }

    private void updateAutoComplete(String currentWord) {
        List<String> suggestions = new ArrayList<>();

        for (String w : AlgoLangSyntaxHighlighter.getAllCompletions()) {
            if (w.toLowerCase().startsWith(currentWord.toLowerCase()))
                suggestions.add(w);
        }
        for (Map.Entry<String, String> e : AlgoLangSyntaxHighlighter.getCodeTemplates().entrySet()) {
            String key = e.getKey();
            if (key.toLowerCase().startsWith(currentWord.toLowerCase()) && !suggestions.contains(key))
                suggestions.add(key + "  [template]");
        }

        if (suggestions.isEmpty()) {
            autoCompletePopup.hide();
            return;
        }

        suggestionList.getItems().setAll(suggestions);
        suggestionList.getSelectionModel().selectFirst();

        Optional<Bounds> caretBounds = codeArea.getCaretBounds();
        if (caretBounds.isPresent()) {
            Bounds b = caretBounds.get();
            if (!autoCompletePopup.isShowing())
                autoCompletePopup.show(codeArea, b.getMaxX(), b.getMaxY());
        }
    }

    private void insertSuggestion() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;
        autoCompletePopup.hide();

        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();
        int start = caretPos - 1;
        while (start >= 0 && Character.isLetterOrDigit(text.charAt(start)))
            start--;
        start++;

        boolean isTemplate = selected.endsWith("  [template]");
        String keyword = isTemplate
                ? selected.substring(0, selected.length() - "  [template]".length())
                : selected;
        String insertion = isTemplate
                ? AlgoLangSyntaxHighlighter.getCodeTemplates().getOrDefault(keyword, keyword)
                : keyword;

        codeArea.replaceText(start, caretPos, insertion);
        codeArea.requestFocus();
    }

    private String getCurrentWord() {
        int caretPos = codeArea.getCaretPosition();
        String text = codeArea.getText();
        if (caretPos == 0 || caretPos > text.length())
            return "";
        int start = caretPos - 1;
        while (start >= 0 && Character.isLetterOrDigit(text.charAt(start)))
            start--;
        start++;
        return text.substring(start, caretPos);
    }

    // ── Public API
    // ────────────────────────────────────────────────────────────────

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public String getEditorContent() {
        return codeArea.getText();
    }

    public void setEditorContent(String text) {
        codeArea.replaceText(text);
        codeArea.setStyleSpans(0, AlgoLangSyntaxHighlighter.computeHighlighting(text));
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
        if (modified)
            title = "● " + title; // dot instead of asterisk — more modern
        setText(title);
    }
}
