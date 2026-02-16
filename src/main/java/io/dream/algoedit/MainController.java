package io.dream.algoedit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class MainController {

    @FXML private MenuItem menuNew;
    @FXML private MenuItem menuOpen;
    @FXML private MenuItem menuSave;
    @FXML private MenuItem menuSaveAs;
    @FXML private MenuItem menuClose;
    @FXML private MenuItem menuQuit;

    @FXML private MenuItem menuUndo;
    @FXML private MenuItem menuRedo;
    @FXML private MenuItem menuCut;
    @FXML private MenuItem menuCopy;
    @FXML private MenuItem menuPaste;
    @FXML private MenuItem menuDelete;
    @FXML private MenuItem menuSelectAll;
    @FXML private MenuItem menuFind;
    @FXML private MenuItem menuReplace;

    @FXML private MenuItem menuRun;

    @FXML private TabPane tabPane;
    @FXML private Label statusLabel;

    private int untitledCounter = 1;

    @FXML
    public void initialize() {
        setupMenuAccelerators();
        createNewTab();
        updateStatus();
    }

    private void setupMenuAccelerators() {
        // File menu shortcuts
        menuNew.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        menuOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        menuSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        menuClose.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
        menuQuit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        // Edit menu shortcuts
        menuUndo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        menuRedo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        menuCut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        menuCopy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        menuPaste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        menuDelete.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        menuSelectAll.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        menuFind.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        menuReplace.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));

        // Run menu shortcut
        menuRun.setAccelerator(new KeyCodeCombination(KeyCode.F5));
    }

    // ==================== FILE OPERATIONS ====================

    @FXML
    private void handleNew() {
        createNewTab();
    }

    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Algo Files", "*.algo"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            openFile(file);
        }
    }

    @FXML
    private void handleSave() {
        EditorTab currentTab = getCurrentEditorTab();
        if (currentTab != null) {
            if (currentTab.getFile() == null) {
                handleSaveAs();
            } else {
                saveFile(currentTab);
            }
        }
    }

    @FXML
    private void handleSaveAs() {
        EditorTab currentTab = getCurrentEditorTab();
        if (currentTab != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File As");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Algo Files", "*.algo"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );

            if (currentTab.getFile() != null) {
                fileChooser.setInitialDirectory(currentTab.getFile().getParentFile());
                fileChooser.setInitialFileName(currentTab.getFile().getName());
            }

            File file = fileChooser.showSaveDialog(getStage());
            if (file != null) {
                currentTab.setFile(file);
                saveFile(currentTab);
            }
        }
    }

    @FXML
    private void handleClose() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            closeTab(currentTab);
        }
    }

    @FXML
    private void handleQuit() {
        // Check all tabs for unsaved changes
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                if (editorTab.isModified()) {
                    tabPane.getSelectionModel().select(tab);
                    if (!promptSaveChanges(editorTab)) {
                        return; // User cancelled
                    }
                }
            }
        }
        Platform.exit();
    }

    // ==================== EDIT OPERATIONS ====================

    @FXML
    private void handleUndo() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            tab.getCodeArea().undo();
        }
    }

    @FXML
    private void handleRedo() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            tab.getCodeArea().redo();
        }
    }

    @FXML
    private void handleCut() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            tab.getCodeArea().cut();
        }
    }

    @FXML
    private void handleCopy() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            tab.getCodeArea().copy();
        }
    }

    @FXML
    private void handlePaste() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            tab.getCodeArea().paste();
        }
    }

    @FXML
    private void handleDelete() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            if (tab.getCodeArea().getSelectedText().length() > 0) {
                tab.getCodeArea().replaceSelection("");
            }
        }
    }

    @FXML
    private void handleSelectAll() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            tab.getCodeArea().selectAll();
        }
    }

    @FXML
    private void handleFind() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            showFindDialog(false);
        }
    }

    @FXML
    private void handleReplace() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            showFindDialog(true);
        }
    }

    @FXML
    private void handleRun() {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null) {
            showError("No File", "Please open or create a file first.");
            return;
        }

        // Save file if modified
        if (tab.isModified()) {
            if (tab.getFile() == null) {
                handleSaveAs();
            } else {
                saveFile(tab);
            }
        }

        // Show placeholder dialog - you'll integrate your interpreter here
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Run AlgoLang Program");
        alert.setHeaderText("Ready to Execute");
        alert.setContentText(
                "File: " + (tab.getFile() != null ? tab.getFile().getName() : "Untitled") + "\n\n" +
                        "Integration Point:\n" +
                        "This is where you'll call your AlgoLang interpreter.\n\n" +
                        "Suggested integration:\n" +
                        "1. Pass the file path or text content to your interpreter\n" +
                        "2. Capture stdout/stderr\n" +
                        "3. Display output in a console panel or dialog\n\n" +
                        "Press F5 or Run → Execute to run your program."
        );
        alert.showAndWait();

        // TODO: Replace with actual interpreter integration
        // Example:
        // String code = tab.getEditorContent();
        // AlgoLangInterpreter interpreter = new AlgoLangInterpreter();
        // String output = interpreter.execute(code);
        // showOutputDialog(output);
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About algoEdit");
        alert.setHeaderText("algoEdit - Text Editor for AlgoLang");
        alert.setContentText(
                "Version 2.0\n\n" +
                        "A modern text editor designed for editing AlgoLang files.\n\n" +
                        "Features:\n" +
                        "• Syntax highlighting for AlgoLang\n" +
                        "• Line numbers\n" +
                        "• Auto-completion (Ctrl+Space)\n" +
                        "• Code templates\n" +
                        "• Multi-tab editing\n" +
                        "• Find and replace\n\n" +
                        "© 2024 Dream.io"
        );
        alert.showAndWait();
    }

    // ==================== HELPER METHODS ====================

    private void createNewTab() {
        String title = "Untitled-" + untitledCounter++;
        EditorTab tab = new EditorTab(title);

        // Setup tab close request
        tab.setOnCloseRequest(event -> {
            if (!closeTab(tab)) {
                event.consume();
            }
        });

        // Update status on text changes
        tab.getCodeArea().textProperty().addListener((obs, old, newVal) -> {
            updateStatus();
        });

        // Update status on caret position changes
        tab.getCodeArea().caretPositionProperty().addListener((obs, old, newVal) -> {
            updateStatus();
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        tab.getCodeArea().requestFocus();
    }

    private void openFile(File file) {
        // Check if file is already open
        for (Tab tab : tabPane.getTabs()) {
            if (tab instanceof EditorTab editorTab) {
                if (file.equals(editorTab.getFile())) {
                    tabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        }

        try {
            String content = Files.readString(file.toPath());
            EditorTab tab = new EditorTab(file.getName());
            tab.setFile(file);
            tab.setEditorContent(content);
            tab.setModified(false);

            // Setup tab close request
            tab.setOnCloseRequest(event -> {
                if (!closeTab(tab)) {
                    event.consume();
                }
            });

            // Update status on text changes
            tab.getCodeArea().textProperty().addListener((obs, old, newVal) -> {
                updateStatus();
            });

            // Update status on caret position changes
            tab.getCodeArea().caretPositionProperty().addListener((obs, old, newVal) -> {
                updateStatus();
            });

            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
            tab.getCodeArea().requestFocus();

        } catch (IOException e) {
            showError("Error opening file", "Could not open file: " + e.getMessage());
        }
    }

    private void saveFile(EditorTab tab) {
        try {
            Files.writeString(tab.getFile().toPath(), tab.getEditorContent());
            tab.setModified(false);
            updateStatus();
        } catch (IOException e) {
            showError("Error saving file", "Could not save file: " + e.getMessage());
        }
    }

    private boolean closeTab(Tab tab) {
        if (tab instanceof EditorTab editorTab) {
            if (editorTab.isModified()) {
                if (!promptSaveChanges(editorTab)) {
                    return false;
                }
            }
        }
        tabPane.getTabs().remove(tab);

        // Create new tab if all closed
        if (tabPane.getTabs().isEmpty()) {
            createNewTab();
        }

        return true;
    }

    private boolean promptSaveChanges(EditorTab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Do you want to save changes to " + tab.getText() + "?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType dontSaveButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                if (tab.getFile() == null) {
                    handleSaveAs();
                } else {
                    saveFile(tab);
                }
                return true;
            } else if (result.get() == dontSaveButton) {
                return true;
            }
        }
        return false;
    }

    private void showFindDialog(boolean showReplace) {
        EditorTab tab = getCurrentEditorTab();
        if (tab == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(showReplace ? "Find and Replace" : "Find");
        dialog.setHeaderText(null);

        // Create the dialog content
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

        ButtonType findNextButton = new ButtonType("Find Next", ButtonBar.ButtonData.OK_DONE);
        ButtonType replaceButton = new ButtonType("Replace", ButtonBar.ButtonData.OTHER);
        ButtonType replaceAllButton = new ButtonType("Replace All", ButtonBar.ButtonData.OTHER);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (showReplace) {
            dialog.getDialogPane().getButtonTypes().addAll(findNextButton, replaceButton, replaceAllButton, closeButton);
        } else {
            dialog.getDialogPane().getButtonTypes().addAll(findNextButton, closeButton);
        }

        Platform.runLater(() -> findField.requestFocus());

        // Handle button actions
        final Button findNextBtn = (Button) dialog.getDialogPane().lookupButton(findNextButton);
        findNextBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            findNext(tab, findField.getText());
            event.consume();
        });

        if (showReplace) {
            final Button replaceBtn = (Button) dialog.getDialogPane().lookupButton(replaceButton);
            replaceBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                replace(tab, findField.getText(), replaceField.getText());
                event.consume();
            });

            final Button replaceAllBtn = (Button) dialog.getDialogPane().lookupButton(replaceAllButton);
            replaceAllBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                replaceAll(tab, findField.getText(), replaceField.getText());
                dialog.close();
                event.consume();
            });
        }

        dialog.showAndWait();
    }

    private void findNext(EditorTab tab, String searchText) {
        if (searchText.isEmpty()) return;

        CodeArea codeArea = tab.getCodeArea();
        String text = codeArea.getText();
        int startPos = codeArea.getCaretPosition();

        int foundPos = text.indexOf(searchText, startPos);
        if (foundPos == -1) {
            // Search from beginning
            foundPos = text.indexOf(searchText, 0);
        }

        if (foundPos != -1) {
            codeArea.selectRange(foundPos, foundPos + searchText.length());
            codeArea.requestFocus();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Find");
            alert.setHeaderText(null);
            alert.setContentText("No matches found.");
            alert.showAndWait();
        }
    }

    private void replace(EditorTab tab, String searchText, String replaceText) {
        if (searchText.isEmpty()) return;

        CodeArea codeArea = tab.getCodeArea();
        String selected = codeArea.getSelectedText();

        if (selected.equals(searchText)) {
            codeArea.replaceSelection(replaceText);
        }

        findNext(tab, searchText);
    }

    private void replaceAll(EditorTab tab, String searchText, String replaceText) {
        if (searchText.isEmpty()) return;

        CodeArea codeArea = tab.getCodeArea();
        String text = codeArea.getText();
        String newText = text.replace(searchText, replaceText);

        int count = (text.length() - newText.length()) / (searchText.length() - replaceText.length());
        codeArea.replaceText(newText);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Replace All");
        alert.setHeaderText(null);
        alert.setContentText("Replaced " + count + " occurrence(s).");
        alert.showAndWait();
    }

    private void updateStatus() {
        EditorTab tab = getCurrentEditorTab();
        if (tab != null) {
            CodeArea codeArea = tab.getCodeArea();
            int caretPos = codeArea.getCaretPosition();
            String text = codeArea.getText();

            // Calculate line and column
            int line = 1;
            int column = 1;
            for (int i = 0; i < caretPos && i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }

            // Calculate total lines
            int totalLines = text.isEmpty() ? 1 : text.split("\n", -1).length;

            String status = String.format("Line %d, Col %d | Total Lines: %d | Length: %d",
                    line, column, totalLines, text.length());

            if (tab.isModified()) {
                status += " | Modified";
            }

            if (tab.getFile() != null) {
                status += " | " + tab.getFile().getAbsolutePath();
            }

            statusLabel.setText(status);
        } else {
            statusLabel.setText("Ready");
        }
    }

    private EditorTab getCurrentEditorTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected instanceof EditorTab) {
            return (EditorTab) selected;
        }
        return null;
    }

    private Stage getStage() {
        return (Stage) tabPane.getScene().getWindow();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
