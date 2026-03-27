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

    private static final String EDITOR_BG = "#0d1117";
    private static final String GUTTER_BG = "#161b22";
    private static final String GUTTER_BORDER = "#30363d";

    private final CodeArea codeArea;
    private File file;
    private boolean modified;
    private final String originalTitle;

    // Auto-complete
    private Popup autoCompletePopup;
    private ListView<String> suggestionList;

    public EditorTab(String title) {
        super(title);
        this.originalTitle = title;
        this.modified = false;

        // CodeArea
        codeArea = new CodeArea();

        // Force dark background inline — RichTextFX ignores the CSS cascade for
        // its internal canvas nodes, so we must set both the inline style AND the
        // CSS class. The CSS class handles text colour; the inline style handles bg.
        codeArea.setStyle(
                "-fx-background-color: " + EDITOR_BG + "; " +
                        "-fx-font-family: 'Iosevka Charon Mono', 'JetBrains Mono', " +
                        "'Cascadia Code', 'Consolas', 'Monaco', monospace; " +
                        "-fx-font-size: 17px;");
        codeArea.getStyleClass().add("code-area");

        // Line number gutter
        // RichTextFX calls this factory fresh for every paragraph change, so
        // lineIndex is always up-to-date — no listener is needed.
        codeArea.setParagraphGraphicFactory(lineIndex -> {
            Label num = new Label(String.valueOf(lineIndex + 1));
            num.getStyleClass().add("lineno");
            // Also enforce inline so it applies even before stylesheet loads
            num.setStyle(
                    "-fx-text-fill: #404d5b; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-family: 'Iosevka Charon Mono', 'JetBrains Mono','Consolas',monospace; " +
                            "-fx-alignment: center-right; " +
                            "-fx-padding: 0 10 0 6; " +
                            "-fx-min-width: 42; -fx-pref-width: 42;");
            num.setMinWidth(42);
            num.setPrefWidth(42);
            num.setAlignment(Pos.CENTER_RIGHT);
            num.setPadding(new Insets(0, 10, 0, 6));

            HBox hbox = new HBox(num);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setStyle(
                    "-fx-background-color: " + GUTTER_BG + "; " +
                            "-fx-border-color: transparent " + GUTTER_BORDER +
                            " transparent transparent; " +
                            "-fx-border-width: 0 1 0 0;");
            return hbox;
        });

        // Syntax highlighting (debounced 50 ms)
        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(50))
                .subscribe(ignore -> codeArea.setStyleSpans(0,
                        AlgoLangSyntaxHighlighter.computeHighlighting(
                                codeArea.getText())));

        // Track unsaved modifications
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!modified && oldText != null && !oldText.equals(newText))
                setModified(true);
        });

        // Auto-completion
        setupAutoCompletion();

        // Wrap in a StackPane that also enforces the dark background
        StackPane container = new StackPane(codeArea);
        container.setStyle("-fx-background-color: " + EDITOR_BG + ";");
        setContent(container);
        setClosable(true);
    }

    // Auto-completion
    private void setupAutoCompletion() {
        autoCompletePopup = new Popup();
        autoCompletePopup.setAutoHide(true);

        suggestionList = new ListView<>();
        suggestionList.setPrefHeight(180);
        suggestionList.setPrefWidth(260);
        suggestionList.setStyle(
                "-fx-background-color: #1c2330; " +
                        "-fx-border-color: #30363d; -fx-border-width: 1; " +
                        "-fx-font-family: 'Iosevka Charon Mono', 'JetBrains Mono','Consolas',monospace; " +
                        "-fx-font-size: 12px;");

        suggestionList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2)
                insertSuggestion();
        });
        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                insertSuggestion();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                autoCompletePopup.hide();
                codeArea.requestFocus();
                e.consume();
            }
        });

        autoCompletePopup.getContent().add(suggestionList);

        codeArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE && e.isControlDown()) {
                showAutoComplete();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE && autoCompletePopup.isShowing()) {
                autoCompletePopup.hide();
                e.consume();
            }
        });

        codeArea.textProperty().addListener((obs, o, n) -> {
            if (autoCompletePopup.isShowing()) {
                String w = getCurrentWord();
                if (w.length() < 2)
                    autoCompletePopup.hide();
                else
                    updateAutoComplete(w);
            }
        });
    }

    private void showAutoComplete() {
        String w = getCurrentWord();
        if (w.isEmpty()) {
            autoCompletePopup.hide();
            return;
        }
        updateAutoComplete(w);
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

        Optional<Bounds> b = codeArea.getCaretBounds();
        if (b.isPresent() && !autoCompletePopup.isShowing())
            autoCompletePopup.show(codeArea, b.get().getMaxX(), b.get().getMaxY());
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
        return text.substring(start + 1, caretPos);
    }

    // Public API
    public CodeArea getCodeArea() {
        return codeArea;
    }

    public String getEditorContent() {
        return codeArea.getText();
    }

    public void setEditorContent(String content) {
        codeArea.replaceText(content);
        // Force an immediate syntax-highlight pass after loading file content
        if (!content.isEmpty())
            codeArea.setStyleSpans(0,
                    AlgoLangSyntaxHighlighter.computeHighlighting(content));
    }

    public File getFile() {
        return file;
    }

    public boolean isModified() {
        return modified;
    }

    public void setFile(File file) {
        this.file = file;
        updateTitle();
    }

    public void setModified(boolean modified) {
        this.modified = modified;
        updateTitle();
    }

    private void updateTitle() {
        String title = (file != null) ? file.getName() : originalTitle;
        setText(modified ? "● " + title : title);
    }
}
