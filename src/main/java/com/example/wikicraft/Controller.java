package com.example.wikicraft;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSException;
import javafx.scene.layout.*;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities;

public class Controller {
    @FXML
    private AnchorPane rootPane;

    @FXML
    private VBox rootVBox;

    @FXML
    private HTMLEditor htmlEditor;

    private boolean isInsertMode = false;

    private WebEngine engine;
    private ToolBar topToolBar;
    private ToolBar bottomToolBar;

    private CustomTitleBar titleBar;
    private Stage stage;

    private Popup modeInfoPopup;
    private Popup fileInfoPopup;

    private Path currentContentFile = Paths.get(
            System.getProperty("user.home"),
            "Documents",
            "Projects",
            "WikiCraft",
            "WikiCraft",
            "src",
            "test",
            "example-wiki",
            "index.html"
    );

    private String lastSavedInsertModeContent = "";
    private String insertModeContent = "";
    private String viewModeContent = "";

    private List<String> detectedPatterns = new ArrayList<>();

    private int selectedPatternIndex = -1;

    private Stack<String> navigationStack = new Stack<>();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            stage = (Stage) rootVBox.getScene().getWindow();

            titleBar = new CustomTitleBar(stage);
            rootVBox.getChildren().add(0, titleBar);

            Scene scene = stage.getScene();
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    titleBar.toggleFullScreen();
                    event.consume();
                    adjustLayout();
                }
            });

            AnchorPane.setTopAnchor(rootVBox, 0.0);
            AnchorPane.setBottomAnchor(rootVBox, 0.0);
            AnchorPane.setLeftAnchor(rootVBox, 0.0);
            AnchorPane.setRightAnchor(rootVBox, 0.0);

            titleBar.addTransparentResizeRegions(rootPane);

            adjustHTMLEditorBehavior();
        });
        VBox.setVgrow(htmlEditor, Priority.ALWAYS);

        modeInfoPopup = new Popup(200, 50);
        modeInfoPopup.setPopupColor(Color.CHOCOLATE);
        modeInfoPopup.setPopupOpacity(0.85);
        modeInfoPopup.setTextFont(javafx.scene.text.Font.font("Monocraft", 20));

        fileInfoPopup = new Popup(400, 50);
        fileInfoPopup.setPopupColor(Color.CHOCOLATE);
        fileInfoPopup.setPopupOpacity(0.85);
        fileInfoPopup.setTextFont(javafx.scene.text.Font.font("Monocraft", 20));

        rootPane.getChildren().add(modeInfoPopup);
        rootPane.getChildren().add(fileInfoPopup);

        htmlEditor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        stage = (Stage) newWindow;

                        stage.heightProperty().addListener((obsHeight, oldHeight, newHeight) -> {
                            adjustTooltipPos(modeInfoPopup);
                            adjustTooltipPos(fileInfoPopup);
                        });
                        stage.widthProperty().addListener((obsWidth, oldWidth, newWidth) -> {
                            adjustTooltipPos(modeInfoPopup);
                            adjustTooltipPos(fileInfoPopup);
                        });
                    }
                });

                addCustomButtonToHTMLEditor(htmlEditor);
                setupModeSwitching(newScene);
                setupKeyHandlers(newScene);

                loadContent();

                enterViewMode();
            }
        });
    }

    private void adjustHTMLEditorBehavior() {
        Node webView = htmlEditor.lookup(".web-view");
        if (webView instanceof WebView) {
            ((WebView) webView).setContextMenuEnabled(true);
            ((WebView) webView).getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    if (titleBar.isFullScreen) ((WebView) webView).prefHeightProperty().bind(rootVBox.heightProperty());
                    else ((WebView) webView).prefHeightProperty().bind(rootVBox.heightProperty().subtract(titleBar.getHeight()));
                }
            });
        }
    }

    private void adjustLayout() {
        double titleBarHeight = titleBar.getHeight();
        if (titleBar.isFullScreen) htmlEditor.setPrefHeight(stage.getHeight());
        else htmlEditor.setPrefHeight(stage.getHeight() - titleBarHeight);
    }

    private void setupKeyHandlers(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isInsertMode) return;

            if (event.getCode() == KeyCode.TAB) {
                handleTabPress();
            } else if (event.getCode() == KeyCode.ENTER) {
                handleEnterKeyPress();
                enterViewMode();
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                if (!navigationStack.isEmpty()) {
                    handleBackspacePress();
                    enterViewMode();
                }
            }
            event.consume();
        });
    }

    private void handleTabPress() {
        if (detectedPatterns.isEmpty()) return;

        // Cycle through detected patterns
        selectedPatternIndex = (selectedPatternIndex + 1) % detectedPatterns.size();
        String selectedPattern = detectedPatterns.get(selectedPatternIndex);

        // Highlight the selected pattern
        highlightPattern(selectedPattern);

        // Transform and load View Mode content
        transformToViewModeContent();
        htmlEditor.setHtmlText(viewModeContent);

        // Debugging output
        System.out.println("Highlighted pattern: " + selectedPattern);
    }

    private void transformToViewModeContent() {
        // Apply transformations: underline and remove brackets
        viewModeContent = viewModeContent.replaceAll(
                "\\(\\((.*?)\\)\\)", "<span style=\"text-decoration: underline;\">$1</span>"
        ).replaceAll(
                "\\[\\[(.*?)\\]\\]", "<span style=\"text-decoration: underline;\">$1</span>"
        );
    }

    private void highlightPattern(String pattern) {
        // Underline and set light-green background for the selected pattern
        String transformedContent = insertModeContent.replaceAll(
                Pattern.quote(pattern),
                "<span style=\"background-color: lightgreen; text-decoration: underline;\">" +
                        pattern.substring(2, pattern.length() - 2) + "</span>"
        );

        viewModeContent = transformedContent;
    }

    private void handleEnterKeyPress() {
        if (selectedPatternIndex < 0 || selectedPatternIndex >= detectedPatterns.size()) return;


        String selectedPattern = detectedPatterns.get(selectedPatternIndex);
        if (selectedPattern == null || selectedPattern.isEmpty()) return;


        boolean isDoubleBrackets = isPatternDoubleBrackets(selectedPattern);
        String fileName = extractFileName(selectedPattern);
        Path targetFile;

        if (isDoubleBrackets) {
            String folderName = capitalizeFirstLetter(fileName);
            targetFile = currentContentFile.getParent().resolve(folderName).resolve(fileName + ".html");
        } else {
            targetFile = currentContentFile.getParent().resolve(fileName + ".html");
        }

        saveContent();

        navigationStack.push(currentContentFile.toString());

        if (Files.exists(targetFile)) {
            loadContentFromFile(targetFile);
            fileInfoPopup.show("Loaded " + targetFile.getFileName().toString());
        } else {
            try {
                if (isDoubleBrackets) {
                    Files.createDirectories(targetFile.getParent());
                }
                Files.createFile(targetFile);
                Files.write(targetFile, "Welcome to WikiCraft!".getBytes());
                loadContentFromFile(targetFile);
                fileInfoPopup.show("Created and loaded " + targetFile.getFileName().toString());
            } catch (IOException e) {
                e.printStackTrace();
                fileInfoPopup.show("Failed to create " + targetFile.getFileName().toString());
            }
        }
    }

    private void handleBackspacePress() {
        saveContent();

        String previousFilePath = navigationStack.pop();
        Path previousFile = Paths.get(previousFilePath);

        if (Files.exists(previousFile)) {
            loadContentFromFile(previousFile);
            fileInfoPopup.show("Returned to " + previousFile.getFileName().toString());
        } else {
            fileInfoPopup.show("File not found: " + previousFile.getFileName().toString());
        }
    }

    private boolean isPatternDoubleBrackets(String pattern) {
        return pattern.startsWith("[[") && pattern.endsWith("]]");
    }

    private String extractFileName(String pattern) {
        if (pattern.startsWith("((") && pattern.endsWith("))")) {
            return pattern.substring(2, pattern.length() - 2);
        } else if (pattern.startsWith("[[") && pattern.endsWith("]]")) {
            return pattern.substring(2, pattern.length() - 2);
        }
        return "";
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void detectPatterns() {
        detectedPatterns.clear();
        selectedPatternIndex = -1;

        String plainText = insertModeContent.replaceAll("<[^>]*>", "");

        Pattern pattern = Pattern.compile("\\(\\((.*?)\\)\\)|\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(plainText);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                detectedPatterns.add("((" + matcher.group(1) + "))");
            } else if (matcher.group(2) != null) {
                detectedPatterns.add("[[" + matcher.group(2) + "]]");
            }
        }
        System.out.println("Detected patterns: " + detectedPatterns);
    }

    private void setupModeSwitching(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.INSERT) {
                if (isInsertMode) {
                    enterViewMode();
                } else {
                    enterInsertMode();
                }
                isInsertMode = !isInsertMode;
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                if (isInsertMode) {
                    enterViewMode();
                    isInsertMode = false;
                    event.consume();
                } else {
                    saveContent();
                    if (stage != null) {
                        stage.close();
                    }
                    Platform.exit();
                }
            }
        });
    }

    private void enterViewMode() {
        if (engine != null) {
            engine.executeScript("document.body.contentEditable = false;");
        }
        if (topToolBar != null) {
            topToolBar.setVisible(false);
            topToolBar.setManaged(false);
        }
        if (bottomToolBar != null) {
            bottomToolBar.setVisible(false);
            bottomToolBar.setManaged(false);
        }
        modeInfoPopup.show("View Mode");

        insertModeContent = htmlEditor.getHtmlText();
        detectPatterns();
        viewModeContent = insertModeContent;
        transformToViewModeContent();
        htmlEditor.setHtmlText(viewModeContent);

        saveContent();
    }

    private void enterInsertMode() {
        if (engine != null) {
            engine.executeScript("document.body.contentEditable = true;");
        }
        if (topToolBar != null) {
            topToolBar.setVisible(true);
            topToolBar.setManaged(true);
        }
        if (bottomToolBar != null) {
            bottomToolBar.setVisible(true);
            bottomToolBar.setManaged(true);
        }
        modeInfoPopup.show("Insert Mode");

        htmlEditor.setHtmlText(insertModeContent);
    }

    private void saveContent() {
        if (!insertModeContent.equals(lastSavedInsertModeContent)) {
            lastSavedInsertModeContent = insertModeContent;

            try {
                Document document = Jsoup.parse(insertModeContent);
                document.outputSettings(new OutputSettings().indentAmount(4).prettyPrint(true));
                document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
                String formattedContent = document.html();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentContentFile.toString()))) {
                    writer.write(formattedContent);
                    System.out.println("Content saved successfully to " + currentContentFile);
                    fileInfoPopup.show("Content Saved");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to format or save content to " + currentContentFile);
                fileInfoPopup.show("Save Failed");
            }
        }
    }

    private void loadContent() {
        Path contentFilePath = currentContentFile;

        if (Files.exists(contentFilePath)) {
            try {
                String content = new String(Files.readAllBytes(contentFilePath));
                htmlEditor.setHtmlText(content);
                lastSavedInsertModeContent = content;
                insertModeContent = content;

                System.out.println("Content loaded successfully from " + contentFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load content from " + contentFilePath);
                htmlEditor.setHtmlText("Welcome to WikiCraft!");
            }
        } else {
            htmlEditor.setHtmlText("Welcome to WikiCraft!");
            System.out.println(contentFilePath + " does not exist. Setting default content.");
        }
    }

    private void loadContentFromFile(Path filePath) {
        currentContentFile = filePath;
        loadContent();
    }

    private void addCustomButtonToHTMLEditor(HTMLEditor htmlEditor) {
        Node toolNode = htmlEditor.lookup(".top-toolbar");
        Node bottomToolNode = htmlEditor.lookup(".bottom-toolbar");
        Node webNode = htmlEditor.lookup(".web-view");
        if (toolNode instanceof ToolBar && webNode instanceof WebView && bottomToolNode instanceof ToolBar) {
            topToolBar = (ToolBar) toolNode;
            bottomToolBar = (ToolBar) bottomToolNode;
            WebView webView = (WebView) webNode;
            engine = webView.getEngine();

            Button btnCaretAddImage = new Button("Add Shrex");
            btnCaretAddImage.setMinSize(100.0, 24.0);
            btnCaretAddImage.setMaxSize(100.0, 24.0);

            topToolBar.getItems().addAll(btnCaretAddImage);

            String resourcePath = "/shrex.txt";
            String base64ImageData = "";

            try (InputStream inputStream = Main.class.getResourceAsStream(resourcePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                base64ImageData = reader.readLine();

            } catch (Exception e) {
                e.printStackTrace();
            }

            String img = String.format("<img alt=\"Embedded Image\" src=\"%s\" />", base64ImageData);

            String jsCodeInsertHtml = "function insertHtmlAtCursor(html) {\n" +
                    "    var range, node;\n" +
                    "    if (window.getSelection && window.getSelection().getRangeAt) {\n" +
                    "        range = window.getSelection().getRangeAt(0);\n" +
                    "        node = range.createContextualFragment(html);\n" +
                    "        range.insertNode(node);\n" +
                    "    } else if (document.selection && document.selection.createRange) {\n" +
                    "        document.selection.createRange().pasteHTML(html);\n" +
                    "    }\n" +
                    "}insertHtmlAtCursor('####html####');";

            btnCaretAddImage.setOnAction((ActionEvent event) -> {
                try {
                    engine.executeScript(jsCodeInsertHtml.replace("####html####",
                            escapeJavaStyleString(img, true, true)));
                } catch (JSException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static String escapeJavaStyleString(String str, boolean escapeSingleQuote, boolean escapeForwardSlash) {
        StringBuilder out = new StringBuilder("");
        if (str == null) {
            return null;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            if (ch > 0xfff) {
                out.append("\\u").append(hex(ch));
            } else if (ch > 0xff) {
                out.append("\\u0").append(hex(ch));
            } else if (ch > 0x7f) {
                out.append("\\u00").append(hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        out.append('\\');
                        out.append('b');
                        break;
                    case '\n':
                        out.append('\\');
                        out.append('n');
                        break;
                    case '\t':
                        out.append('\\');
                        out.append('t');
                        break;
                    case '\f':
                        out.append('\\');
                        out.append('f');
                        break;
                    case '\r':
                        out.append('\\');
                        out.append('r');
                        break;
                    default:
                        if (ch > 0xf) {
                            out.append("\\u00").append(hex(ch));
                        } else {
                            out.append("\\u000").append(hex(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':
                        if (escapeSingleQuote) {
                            out.append('\\');
                        }
                        out.append('\'');
                        break;
                    case '"':
                        out.append('\\');
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        out.append('\\');
                        break;
                    case '/':
                        if (escapeForwardSlash) {
                            out.append('\\');
                        }
                        out.append('/');
                        break;
                    default:
                        out.append(ch);
                        break;
                }
            }
        }
        return out.toString();
    }

    private static String hex(int i) {
        return Integer.toHexString(i);
    }

    private void adjustTooltipPos(Popup popup) {
        if (stage != null) {
            double windowWidth = stage.getWidth();
            double popupWidth = popup.getPopupWidth();
            double verticalOffset = 20;

            AnchorPane.setBottomAnchor(popup, verticalOffset);
            AnchorPane.setLeftAnchor(popup, (windowWidth - popupWidth) / 2);
        }
    }

    private void runOnUiThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
