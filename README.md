# algoEdit v2.0 - Advanced IDE for AlgoLang

A professional IDE-like text editor built with JavaFX and RichTextFX, specifically designed for editing and running AlgoLang programs with syntax highlighting, auto-completion, and line numbers.

## 🎯 Advanced Features

### ✨ Syntax Highlighting
Real-time color-coded syntax highlighting for AlgoLang:
- **Keywords** (blue, bold): `Algorithme`, `Variables`, `Debut`, `Fin`, `Ecrire`, `Lire`, `Si`, `alors`, `Sinon`, `TantQue`, `faire`
- **Types** (teal, bold): `entier`, `reel`, `chaine_charactere`, `caractere`, `booleen`
- **Boolean Literals** (blue): `vrai`, `faux`, `nil`
- **Operators** (gray, bold): `<-`, `==`, `!=`, `<=`, `>=`, `<`, `>`, `=`, `+`, `-`, `*`, `/`, `!`
- **Numbers** (green): Numeric literals
- **Strings** (red): String literals
- **Comments** (green, italic): `//` single-line and `/* */` multi-line
- **Identifiers** (dark blue): Variable and function names

### 📊 Line Numbers
- Professional line number gutter
- Gray background for distinction
- Synchronized scrolling

### 🔮 Auto-Completion (Ctrl+Space)
Intelligent code completion with:
- **Keyword suggestions**: All AlgoLang keywords
- **Type suggestions**: Data types
- **Code templates**: Pre-built structures with placeholders

#### Available Templates:
- `algo` → Complete algorithm structure
- `si` → If-then-else statement
- `tantque` → While loop
- `ecrire` → Write statement
- `lire` → Read statement
- `var` → Variable declaration

### ▶️ Run Button (F5)
- Execute AlgoLang programs
- Auto-saves before running
- Integration point for your interpreter

## 📦 Installation

### Prerequisites
- Java 21+
- Maven 3.6+

### Setup

```bash
# Clone or copy all files to your project

# Project structure:
algoEdit/
├── pom.xml
├── src/main/
│   ├── java/
│   │   ├── module-info.java
│   │   └── io/dream/algoedit/
│   │       ├── Launcher.java
│   │       ├── Main.java
│   │       ├── MainController.java
│   │       ├── EditorTab.java
│   │       └── AlgoLangSyntaxHighlighter.java
│   └── resources/io/dream/algoedit/
│       ├── main-view.fxml
│       └── styles.css

# Build and run
mvn clean install
mvn javafx:run
```

## 🔌 Integrating Your Interpreter

### Quick Integration

Edit `MainController.java`, find the `handleRun()` method, and replace the TODO section:

```java
@FXML
private void handleRun() {
    EditorTab tab = getCurrentEditorTab();
    if (tab == null) return;
    
    // Save if modified
    if (tab.isModified()) {
        if (tab.getFile() == null) {
            handleSaveAs();
        } else {
            saveFile(tab);
        }
    }
    
    // YOUR INTERPRETER INTEGRATION HERE:
    try {
        String code = tab.getText();
        
        // Option 1: Direct API call
        AlgoLangInterpreter interpreter = new AlgoLangInterpreter();
        String output = interpreter.execute(code);
        showOutputDialog("Output", output);
        
        // Option 2: Execute file
        // String output = interpreter.executeFile(tab.getFile().getAbsolutePath());
        
        // Option 3: External process
        // ProcessBuilder pb = new ProcessBuilder("java", "-jar", "interpreter.jar", file);
        // Process p = pb.start();
        // ... capture output ...
        
    } catch (Exception e) {
        showError("Execution Error", e.getMessage());
    }
}
```

### Add Output Dialog Helper

```java
private void showOutputDialog(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    
    TextArea textArea = new TextArea(content);
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setPrefHeight(400);
    textArea.setPrefWidth(600);
    
    alert.getDialogPane().setContent(textArea);
    alert.showAndWait();
}
```

## ⌨️ Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| New File | Ctrl+N |
| Open File | Ctrl+O |
| Save | Ctrl+S |
| Save As | Ctrl+Shift+S |
| Close Tab | Ctrl+W |
| Undo | Ctrl+Z |
| Redo | Ctrl+Y |
| Cut/Copy/Paste | Ctrl+X/C/V |
| Select All | Ctrl+A |
| Find | Ctrl+F |
| Replace | Ctrl+H |
| **Auto-Complete** | **Ctrl+Space** |
| **Run Program** | **F5** |

## 🎨 Customization

### Change Syntax Colors

Edit `styles.css`:

```css
.keyword {
    -fx-fill: #0000ff;  /* Your color */
    -fx-font-weight: bold;
}
```

### Add Keywords

Edit `AlgoLangSyntaxHighlighter.java`:

```java
private static final String[] KEYWORDS = {
    "Algorithme", "Variables", "Debut", "Fin",
    "YourNewKeyword"  // Add here
};
```

### Add Templates

Edit `AlgoLangSyntaxHighlighter.java`:

```java
templates.put("mytemplate", "Code with ${placeholder}");
```

## 📝 AlgoLang Example

```algolang
Algorithme: CalculateurMoyenne;
Variables:
    nombre1: entier;
    nombre2: entier;
    moyenne: reel;
Debut:
    Ecrire("Entrez le premier nombre:");
    Lire(nombre1);
    Ecrire("Entrez le deuxième nombre:");
    Lire(nombre2);
    moyenne <- (nombre1 + nombre2) / 2;
    Ecrire("La moyenne est:", moyenne);
Fin
```

## 🐛 Troubleshooting

**Syntax highlighting not working?**
- Check styles.css is loaded
- Verify AlgoLangSyntaxHighlighter.java is compiled

**Auto-completion not appearing?**
- Press Ctrl+Space (not just Space)
- Type at least 1 character first

**RichTextFX errors?**
- Run `mvn clean install` to download dependencies
- Check module-info.java includes RichTextFX modules

**Run button does nothing?**
- Default shows placeholder dialog
- Integrate your interpreter in handleRun() method

## 🔮 Future Features

- Code folding for Debut/Fin blocks
- Bracket matching
- Error highlighting
- Integrated console panel
- Debugger with breakpoints
- Variable hover info
- Go to definition

## 📄 License

Personal project for AlgoLang development.

---

**Version 2.0** | Built with ❤️ for AlgoLang
