# algoEdit v2.0 - Advanced IDE for AlgLang

A professional IDE-like text editor built with JavaFX and RichTextFX, specifically designed for editing and running AlgLang programs with syntax highlighting, auto-completion, and line numbers.

## 🎯 Advanced Features

### 🔮 Auto-Completion (Ctrl+Space)
Intelligent code completion with:
- **Keyword suggestions**: All AlgLang keywords
- **Type suggestions**: Data types
- **Code templates**: Pre-built structures with placeholders

### ▶️ Run Button (F5)
- Execute AlgLang programs
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
│   │       └── AlgLangSyntaxHighlighter.java
│   └── resources/io/dream/algoedit/
│       ├── main-view.fxml
│       └── styles.css

# Build and run
mvn clean install
mvn javafx:run
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

## 🔮 Future Features

- Code folding for Debut/Fin blocks
- Bracket matching
- Error highlighting
- Integrated console panel
- Debugger with breakpoints
- Variable hover info
- Go to definition

## 📄 License

Personal project for AlgLang development.

---

**Version 2.0** | Built with ❤️ for AlgLang
