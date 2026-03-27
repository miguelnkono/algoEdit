package io.dream.algoedit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import org.fxmisc.richtext.CodeArea;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {

    // Menu items
    @FXML
    private MenuItem menuNew, menuOpen, menuSave, menuSaveAs, menuClose, menuQuit;
    @FXML
    private MenuItem menuUndo, menuRedo, menuCut, menuCopy, menuPaste,
            menuDelete, menuSelectAll, menuFind, menuReplace;
    @FXML
    private MenuItem menuRun;

    // Main UI
    @FXML
    private TabPane tabPane;
    @FXML
    private Label statusLabel;
    @FXML
    private Label aiStatusLabel;

    // File explorer
    @FXML
    private ListView<String> fileListView;

    // AI chat
    @FXML
    private VBox chatMessagesBox;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private TextArea chatInput;

    private int untitledCounter = 1;

    @FXML
    public void initialize() {
        setupMenuAccelerators();
        createNewTab();
        updateStatus();
        addWelcomeAiMessage();

        // Defer file-list refresh until after full FXML injection (avoids NPE
        // if fileListView is inside a nested container and injects late).
        Platform.runLater(this::refreshFileList);

        // Ctrl+Enter sends the chat message (Enter alone is a newline in TextArea)
        if (chatInput != null) {
            chatInput.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                    handleSendChat();
                    event.consume();
                }
            });
        }
    }

    // Keyboard shortcuts
    private void setupMenuAccelerators() {
        menuNew.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        menuOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        menuSaveAs.setAccelerator(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        menuClose.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
        menuQuit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        menuUndo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        menuRedo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        menuCut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        menuCopy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        menuPaste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        menuDelete.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        menuSelectAll.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        menuFind.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        menuReplace.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        menuRun.setAccelerator(new KeyCodeCombination(KeyCode.F5));
    }

    // File menu
    @FXML
    private void handleNew() {
        createNewTab();
    }

    @FXML
    private void handleOpen() {
        File file = buildFileChooser("Open File").showOpenDialog(getStage());
        if (file != null)
            openFile(file);
    }

    @FXML
    private void handleSave() {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null)
            return;
        if (tab.getFile() == null)
            handleSaveAs();
        else
            saveFile(tab);
    }

    @FXML
    private void handleSaveAs() {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null)
            return;
        FileChooser fc = buildFileChooser("Save File As");
        if (tab.getFile() != null) {
            fc.setInitialDirectory(tab.getFile().getParentFile());
            fc.setInitialFileName(tab.getFile().getName());
        }
        File file = fc.showSaveDialog(getStage());
        if (file != null) {
            tab.setFile(file);
            saveFile(tab);
        }
    }

    @FXML
    private void handleClose() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        if (tab != null)
            closeTab(tab);
    }

    @FXML
    private void handleQuit() {
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab et && et.isModified()) {
                tabPane.getSelectionModel().select(tab);
                if (!promptSaveChanges(et))
                    return;
            }
        }
        Platform.exit();
    }

    // Edit menu
    @FXML
    private void handleUndo() {
        EditorTab t = getCurrentEditorTab();
        if (t != null)
            t.getCodeArea().undo();
    }

    @FXML
    private void handleRedo() {
        EditorTab t = getCurrentEditorTab();
        if (t != null)
            t.getCodeArea().redo();
    }

    @FXML
    private void handleCut() {
        EditorTab t = getCurrentEditorTab();
        if (t != null)
            t.getCodeArea().cut();
    }

    @FXML
    private void handleCopy() {
        EditorTab t = getCurrentEditorTab();
        if (t != null)
            t.getCodeArea().copy();
    }

    @FXML
    private void handlePaste() {
        EditorTab t = getCurrentEditorTab();
        if (t != null)
            t.getCodeArea().paste();
    }

    @FXML
    private void handleSelectAll() {
        EditorTab t = getCurrentEditorTab();
        if (t != null)
            t.getCodeArea().selectAll();
    }

    @FXML
    private void handleDelete() {
        EditorTab t = getCurrentEditorTab();
        if (t != null && !t.getCodeArea().getSelectedText().isEmpty())
            t.getCodeArea().replaceSelection("");
    }

    @FXML
    private void handleFind() {
        if (getCurrentEditorTab() != null)
            showFindDialog(false);
    }

    @FXML
    private void handleReplace() {
        if (getCurrentEditorTab() != null)
            showFindDialog(true);
    }

    // Run menu
    @FXML
    private void handleRun() {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null) {
            showError("No File", "Please open or create a file first.");
            return;
        }
        if (tab.isModified()) {
            if (tab.getFile() == null)
                handleSaveAs();
            else
                saveFile(tab);
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Run AlgoLang Program");
        a.setHeaderText("Ready to Execute");
        a.setContentText("File: " + (tab.getFile() != null ? tab.getFile().getName() : "Untitled") +
                "\n\nIntegration point: pass the file path or text content to your AlgoLang interpreter.");
        a.showAndWait();
    }

    // Help menu
    @FXML
    private void handleAbout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("About algoEdit");
        a.setHeaderText("algoEdit — Text Editor for AlgoLang");
        a.setContentText(
                "Version 3.0\n\n" +
                        "Features:\n" +
                        "• Three-panel layout: Explorer · Editor · AI Assistant\n" +
                        "• Dark theme with syntax highlighting\n" +
                        "• Styled line numbers\n" +
                        "• Auto-completion (Ctrl+Space)\n" +
                        "• Code templates\n" +
                        "• Multi-tab editing\n" +
                        "• Find and replace (Ctrl+F / Ctrl+H)\n\n" +
                        "© 2024 Dream.io");
        a.showAndWait();
    }

    // AI Chat panel
    @FXML
    public void handleSendChat() {
        if (chatInput == null)
            return;
        String question = chatInput.getText().trim();
        if (question.isEmpty())
            return;
        chatInput.clear();
        addChatBubble(question, true);
        setAiStatus("thinking…");

        EditorTab tab = getCurrentEditorTab();
        String context = "";
        if (tab != null) {
            String sel = tab.getCodeArea().getSelectedText();
            context = (sel != null && !sel.isBlank()) ? sel : tab.getEditorContent();
        }

        final String q = question, ctx = context;
        Thread t = new Thread(() -> {
            String response = AiAssistant.ask(q, ctx);
            Platform.runLater(() -> {
                addChatBubble(response, false);
                setAiStatus("idle");
            });
        }, "ai-chat-thread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleExplainCode() {
        if (chatInput == null)
            return;
        EditorTab tab = getCurrentEditorTab();
        if (tab == null) {
            addChatBubble("Please open a file first.", false);
            return;
        }
        String sel = tab.getCodeArea().getSelectedText();
        String code = (sel != null && !sel.isBlank()) ? sel : tab.getEditorContent();
        if (code.isBlank()) {
            addChatBubble("The current file is empty.", false);
            return;
        }
        chatInput.setText("Explain this AlgoLang code:");
        handleSendChat();
    }

    private void addWelcomeAiMessage() {
        if (chatMessagesBox == null)
            return;
        addChatBubble(
                "Hello! I'm your AlgoLang assistant.\n\n" +
                        "I can explain your code, help you debug, or answer questions as you type.\n\n" +
                        "Tip: use Ctrl+Space for auto-completion in the editor.",
                false);
    }

    private void addChatBubble(String text, boolean isUser) {
        if (chatMessagesBox == null)
            return;
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);
        bubble.getStyleClass().add(isUser ? "chat-bubble-user" : "chat-bubble-ai");

        HBox row = new HBox(bubble);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        if (isUser)
            HBox.setMargin(bubble, new Insets(0, 0, 0, 16));

        chatMessagesBox.getChildren().add(row);
        Platform.runLater(() -> {
            if (chatScrollPane != null)
                chatScrollPane.setVvalue(1.0);
        });
    }

    private void setAiStatus(String status) {
        if (aiStatusLabel != null)
            aiStatusLabel.setText("AI: " + status);
    }

    // Tab / file management
    private void createNewTab() {
        String title = "Untitled-" + untitledCounter++;
        EditorTab tab = new EditorTab(title);
        tab.setOnCloseRequest(e -> {
            if (!closeTab(tab))
                e.consume();
        });
        tab.getCodeArea().textProperty().addListener((o, a, b) -> updateStatus());
        tab.getCodeArea().caretPositionProperty().addListener((o, a, b) -> updateStatus());
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        tab.getCodeArea().requestFocus();
        refreshFileList();
    }

    private void openFile(File file) {
        // Switch to tab if already open
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab et && file.equals(et.getFile())) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        try {
            String content = Files.readString(file.toPath());
            EditorTab tab = new EditorTab(file.getName());
            tab.setFile(file);
            tab.setEditorContent(content);
            tab.setModified(false);
            tab.setOnCloseRequest(e -> {
                if (!closeTab(tab))
                    e.consume();
            });
            tab.getCodeArea().textProperty().addListener((o, a, b) -> updateStatus());
            tab.getCodeArea().caretPositionProperty().addListener((o, a, b) -> updateStatus());
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
            tab.getCodeArea().requestFocus();
            refreshFileList();
        } catch (IOException e) {
            showError("Error opening file", "Could not open file: " + e.getMessage());
        }
    }

    private void saveFile(EditorTab tab) {
        try {
            Files.writeString(tab.getFile().toPath(), tab.getEditorContent());
            tab.setModified(false);
            updateStatus();
            refreshFileList();
        } catch (IOException e) {
            showError("Error saving file", "Could not save file: " + e.getMessage());
        }
    }

    private boolean closeTab(Tab tab) {
        if (tab instanceof EditorTab et && et.isModified())
            if (!promptSaveChanges(et))
                return false;
        tabPane.getTabs().remove(tab);
        refreshFileList();
        return true;
    }

    private boolean promptSaveChanges(EditorTab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Save changes to " + tab.getText() + "?");
        alert.setContentText("Your changes will be lost if you don't save them.");
        ButtonType save = new ButtonType("Save");
        ButtonType dontSave = new ButtonType("Don't Save");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, dontSave, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancel)
            return false;
        if (result.get() == save) {
            if (tab.getFile() == null)
                handleSaveAs();
            else
                saveFile(tab);
        }
        return true;
    }

    /** Refresh the file explorer list with the names of all open tabs. */
    private void refreshFileList() {
        if (fileListView == null)
            return; // guard against early / late FXML injection
        fileListView.getItems().clear();
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab et) {
                String name = (et.getFile() != null) ? et.getFile().getName() : et.getText();
                fileListView.getItems().add(name);
            }
        }
        fileListView.setOnMouseClicked(event -> {
            int idx = fileListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tabPane.getTabs().size()) {
                tabPane.getSelectionModel().select(idx);
                EditorTab et = getCurrentEditorTab();
                if (et != null)
                    et.getCodeArea().requestFocus();
            }
        });
    }

    // Find / Replace
    private void showFindDialog(boolean showReplace) {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null)
            return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(showReplace ? "Find and Replace" : "Find");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        TextField findField = new TextField();
        findField.setPromptText("Find");
        TextField replaceField = new TextField();
        replaceField.setPromptText("Replace with");
        grid.add(new Label("Find:"), 0, 0);
        grid.add(findField, 1, 0);
        if (showReplace) {
            grid.add(new Label("Replace:"), 0, 1);
            grid.add(replaceField, 1, 1);
        }
        dialog.getDialogPane().setContent(grid);

        ButtonType findNextBT = new ButtonType("Find Next", ButtonBar.ButtonData.OK_DONE);
        ButtonType replaceBT = new ButtonType("Replace", ButtonBar.ButtonData.OTHER);
        ButtonType replaceAllBT = new ButtonType("Replace All", ButtonBar.ButtonData.OTHER);
        ButtonType closeBT = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (showReplace)
            dialog.getDialogPane().getButtonTypes().addAll(findNextBT, replaceBT, replaceAllBT, closeBT);
        else
            dialog.getDialogPane().getButtonTypes().addAll(findNextBT, closeBT);

        Platform.runLater(findField::requestFocus);

        ((Button) dialog.getDialogPane().lookupButton(findNextBT))
                .addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                    findNext(tab, findField.getText());
                    e.consume();
                });
        if (showReplace) {
            ((Button) dialog.getDialogPane().lookupButton(replaceBT))
                    .addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                        replace(tab, findField.getText(), replaceField.getText());
                        e.consume();
                    });
            ((Button) dialog.getDialogPane().lookupButton(replaceAllBT))
                    .addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                        replaceAll(tab, findField.getText(), replaceField.getText());
                        dialog.close();
                        e.consume();
                    });
        }
        dialog.showAndWait();
    }

    private void findNext(EditorTab tab, String searchText) {
        if (searchText.isEmpty())
            return;
        CodeArea ca = tab.getCodeArea();
        String text = ca.getText();
        int pos = text.indexOf(searchText, ca.getCaretPosition());
        if (pos == -1)
            pos = text.indexOf(searchText, 0);
        if (pos != -1) {
            ca.selectRange(pos, pos + searchText.length());
            ca.requestFocus();
        } else
            showInfo("Find", "No matches found.");
    }

    private void replace(EditorTab tab, String searchText, String replaceText) {
        if (searchText.isEmpty())
            return;
        CodeArea ca = tab.getCodeArea();
        if (ca.getSelectedText().equals(searchText))
            ca.replaceSelection(replaceText);
        findNext(tab, searchText);
    }

    private void replaceAll(EditorTab tab, String searchText, String replaceText) {
        if (searchText.isEmpty())
            return;
        CodeArea ca = tab.getCodeArea();
        String text = ca.getText();
        int count = 0, idx = 0;
        while ((idx = text.indexOf(searchText, idx)) != -1) {
            count++;
            idx += searchText.length();
        }
        ca.replaceText(text.replace(searchText, replaceText));
        showInfo("Replace All", "Replaced " + count + " occurrence(s).");
    }

    // Status bar
    private void updateStatus() {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null) {
            if (statusLabel != null)
                statusLabel.setText("Ready");
            return;
        }
        CodeArea ca = tab.getCodeArea();
        int pos = ca.getCaretPosition();
        String text = ca.getText();
        int line = 1, col = 1;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                col = 1;
            } else
                col++;
        }
        int totalLines = text.isEmpty() ? 1 : text.split("\n", -1).length;
        String s = String.format("Ln %d, Col %d   |   %d lines   |   %d chars",
                line, col, totalLines, text.length());
        if (tab.isModified())
            s += "   |   ●  modified";
        if (tab.getFile() != null)
            s += "   |   " + tab.getFile().getAbsolutePath();
        if (statusLabel != null)
            statusLabel.setText(s);
    }

    // Utilities
    private EditorTab getCurrentEditorTab() {
        Tab sel = tabPane.getSelectionModel().getSelectedItem();
        return (sel instanceof EditorTab et) ? et : null;
    }

    private Stage getStage() {
        return (Stage) tabPane.getScene().getWindow();
    }

    private FileChooser buildFileChooser(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("AlgoLang Files", "*.algo"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fc;
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
