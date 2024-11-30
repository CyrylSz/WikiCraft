package com.example.wikicraft;

import com.jfoenix.controls.JFXToggleButton;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSException;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.*;
import java.nio.file.DirectoryStream;
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
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeCell;

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

    private VBox settingsMenu;
    private boolean isSettingsMenuVisible = false;

    private VBox searchMenu;
    private boolean isSearchMenuVisible = false;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            stage = (Stage) rootVBox.getScene().getWindow();

            titleBar = new CustomTitleBar(stage);
            rootVBox.getChildren().add(0, titleBar);

            titleBar.setSearchButtonHandler(event -> {
                toggleSearchMenu();
            });
            initSearchMenu();

            titleBar.setPencilButtonHandler(event -> {
                toggleInsertViewMode();
            });
            titleBar.setPencilButtonIcon("\uD83D\uDC41"); // Eye icon for view mode

            titleBar.setOptionsButtonHandler(event -> {
                toggleSettingsMenu();
            });
            initSettingsMenu();

            Scene scene = stage.getScene();
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    titleBar.toggleFullScreen();
                    event.consume();
                    adjustLayout();
                }
            });

            titleBar.addTransparentResizeRegions(rootPane);

            adjustHTMLEditorBehavior();
        });
        VBox.setVgrow(htmlEditor, Priority.ALWAYS);

        htmlEditor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        stage = (Stage) newWindow;

                        stage.heightProperty().addListener((obsHeight, oldHeight, newHeight) -> {
                            if (titleBar != null) adjustLayout();
                        });
                        stage.widthProperty().addListener((obsWidth, oldWidth, newWidth) -> {
                            if (titleBar != null) adjustLayout();
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

    private void initSearchMenu() {
        searchMenu = new VBox();
        searchMenu.setStyle("-fx-background-color: #2e2e2e;");
        searchMenu.setPadding(new Insets(20));
        searchMenu.setSpacing(10);

        Label searchLabel = new Label("Search");
        searchLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Create TreeView for folder structure
        Path rootPath = currentContentFile.getParent(); // "example-wiki" folder
        TreeItem<Path> rootItem = new TreeItem<>(rootPath);
        rootItem.setExpanded(true);

        buildFileTree(rootItem, rootPath);

        TreeView<Path> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false); // Hide the root item if desired
        treeView.setStyle("-fx-background-color: #118191;");

        // Customize the TreeView to display file names without extensions
        treeView.setCellFactory(tv -> new TreeCell<Path>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #072226;");
                } else {
                    String displayName;
                    if (Files.isDirectory(item)) {
                        displayName = item.getFileName().toString();
                        setStyle("-fx-text-fill: yellow; -fx-background-color: #072226; -fx-font-weight: bold;");
                    } else {
                        displayName = item.getFileName().toString().replaceFirst("\\.html$", "");
                        setStyle("-fx-text-fill: white; -fx-background-color: #072226; -fx-font-weight: bold;");
                    }
                    setText(displayName);

                    // Highlight the current file
                    if (item.toAbsolutePath().normalize().equals(currentContentFile.toAbsolutePath().normalize())) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-background-color: #072226;");
                    }
                }
            }
        });

        // Handle tree item selection
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && newSelection.isLeaf()) {
                Path selectedPath = newSelection.getValue();
                if (selectedPath != null) {
                    saveContent();

                    // Update the navigation stack
                    //navigationStack.push(currentContentFile.toString());

                    loadContentFromFile(selectedPath);
                    //enterViewMode();

                    // Optionally hide the search menu after selection - settings
                    if (isSearchMenuVisible) {
                        toggleSearchMenu();
                    }
                }
            }
        });

        expandAndSelectCurrentFile(treeView, rootItem);

        searchMenu.getChildren().addAll(searchLabel, treeView);
        searchMenu.setPrefWidth(300); // Adjust width as needed

        // Initially hide the search menu to the left
        searchMenu.setTranslateX(-searchMenu.getPrefWidth());

        // Add searchMenu to rootPane
        rootPane.getChildren().add(searchMenu);

        // Anchor searchMenu to top, bottom, and left
        AnchorPane.setTopAnchor(searchMenu, 0.0);
        AnchorPane.setBottomAnchor(searchMenu, 0.0);
        AnchorPane.setLeftAnchor(searchMenu, 0.0);
    }

    private void expandAndSelectCurrentFile(TreeView<Path> treeView, TreeItem<Path> rootItem) {
        TreeItem<Path> currentItem = findTreeItem(rootItem, currentContentFile);
        if (currentItem != null) {
            // Expand parent items
            TreeItem<Path> parent = currentItem.getParent();
            while (parent != null) {
                parent.setExpanded(true);
                parent = parent.getParent();
            }

            // Select the current item
            treeView.getSelectionModel().select(currentItem);

            // Scroll to the selected item
            int index = treeView.getRow(currentItem);
            treeView.scrollTo(index);
        }
    }

    private TreeItem<Path> findTreeItem(TreeItem<Path> rootItem, Path targetPath) {
        if (rootItem.getValue().toAbsolutePath().normalize().equals(targetPath.toAbsolutePath().normalize())) {
            return rootItem;
        }
        for (TreeItem<Path> child : rootItem.getChildren()) {
            TreeItem<Path> result = findTreeItem(child, targetPath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private void buildFileTree(TreeItem<Path> parentItem, Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    TreeItem<Path> dirItem = new TreeItem<>(entry);
                    parentItem.getChildren().add(dirItem);
                    buildFileTree(dirItem, entry);
                } else if (entry.getFileName().toString().endsWith(".html")) {
                    TreeItem<Path> fileItem = new TreeItem<>(entry);
                    parentItem.getChildren().add(fileItem);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleSearchMenu() {
        if (isSearchMenuVisible) {
            // Hide search menu
            TranslateTransition hideMenu = new TranslateTransition(Duration.millis(300), searchMenu);
            hideMenu.setToX(-searchMenu.getPrefWidth());
            hideMenu.setInterpolator(Interpolator.EASE_IN);

            // Rotate searchButton icon
            RotateTransition rotateOut = new RotateTransition(Duration.millis(300), titleBar.getSearchButton());
            rotateOut.setByAngle(-180);

            ParallelTransition parallelTransition = new ParallelTransition(hideMenu, rotateOut);
            parallelTransition.setOnFinished(event -> {
                isSearchMenuVisible = false;
                titleBar.setSearchButtonIcon("\uD83D\uDD0D"); // Search icon
                titleBar.getSearchButton().setRotate(0);
            });
            parallelTransition.play();
        } else {
            // Show search menu
            TranslateTransition showMenu = new TranslateTransition(Duration.millis(300), searchMenu);
            showMenu.setToX(0);
            showMenu.setInterpolator(Interpolator.EASE_OUT);

            // Rotate searchButton icon
            RotateTransition rotateIn = new RotateTransition(Duration.millis(300), titleBar.getSearchButton());
            rotateIn.setByAngle(180);

            ParallelTransition parallelTransition = new ParallelTransition(showMenu, rotateIn);
            parallelTransition.setOnFinished(event -> {
                isSearchMenuVisible = true;
                titleBar.setSearchButtonIcon("→"); // Left arrow icon
            });
            parallelTransition.play();
        }
    }

    private void toggleInsertViewMode() {
        if (isInsertMode) {
            enterViewMode();
        } else {
            enterInsertMode();
        }

        // Animate icon change
        RotateTransition rotate = new RotateTransition(Duration.millis(300), titleBar.getPencilButton());
        rotate.setByAngle(180);

        rotate.setOnFinished(event -> {
            if (isInsertMode) {
                titleBar.setPencilButtonIcon("\uD83D\uDD89"); // Pencil icon
            } else {
                titleBar.setPencilButtonIcon("\uD83D\uDC41"); // Eye icon
            }
            titleBar.getPencilButton().setRotate(0);
        });

        rotate.play();

        isInsertMode = !isInsertMode;
    }

    private void initSettingsMenu() {
        settingsMenu = new VBox();
        settingsMenu.setStyle("-fx-background-color: #2e2e2e;");
        settingsMenu.setPadding(new Insets(20));
        settingsMenu.setSpacing(20);

        Label settingsLabel = new Label("Settings");
        settingsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Create toggles
        JFXToggleButton toggle1 = new JFXToggleButton();
        toggle1.setText("Toggle 1");
        toggle1.setStyle("-fx-text-fill: white;");

        JFXToggleButton toggle2 = new JFXToggleButton();
        toggle2.setText("Toggle 2");
        toggle2.setStyle("-fx-text-fill: white;");

        JFXToggleButton toggle3 = new JFXToggleButton();
        toggle3.setText("Toggle 3");
        toggle3.setStyle("-fx-text-fill: white;");

        settingsMenu.getChildren().addAll(settingsLabel, toggle1, toggle2, toggle3);

        settingsMenu.setPrefWidth(300); // Adjust as needed

        // Initially hide the settings menu to the right
        settingsMenu.setTranslateX(settingsMenu.getPrefWidth());

        // Add settingsMenu to rootPane
        rootPane.getChildren().add(settingsMenu);

        // Anchor settingsMenu to top, bottom, and right
        AnchorPane.setTopAnchor(settingsMenu, 0.0);
        AnchorPane.setBottomAnchor(settingsMenu, 0.0);
        AnchorPane.setRightAnchor(settingsMenu, 0.0);
    }

    private void toggleSettingsMenu() {
        if (isSettingsMenuVisible) {
            // Hide settings menu
            TranslateTransition hideMenu = new TranslateTransition(Duration.millis(300), settingsMenu);
            hideMenu.setToX(settingsMenu.getPrefWidth());
            hideMenu.setInterpolator(Interpolator.EASE_IN);

            // Rotate optionsButton icon
            RotateTransition rotateOut = new RotateTransition(Duration.millis(300), titleBar.getOptionsButton());
            rotateOut.setByAngle(-180);

            ParallelTransition parallelTransition = new ParallelTransition(hideMenu, rotateOut);
            parallelTransition.setOnFinished(event -> {
                isSettingsMenuVisible = false;
                titleBar.setOptionsButtonIcon("\u2699"); // Gear icon
                titleBar.getOptionsButton().setRotate(0);
            });
            parallelTransition.play();
        } else {
            // Show settings menu
            TranslateTransition showMenu = new TranslateTransition(Duration.millis(300), settingsMenu);
            showMenu.setToX(0);
            showMenu.setInterpolator(Interpolator.EASE_OUT);

            // Rotate optionsButton icon
            RotateTransition rotateIn = new RotateTransition(Duration.millis(300), titleBar.getOptionsButton());
            rotateIn.setByAngle(180);

            ParallelTransition parallelTransition = new ParallelTransition(showMenu, rotateIn);
            parallelTransition.setOnFinished(event -> {
                isSettingsMenuVisible = true;
                titleBar.setOptionsButtonIcon("←"); // Arrow icon
            });
            parallelTransition.play();
        }
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
        } else {
            try {
                if (isDoubleBrackets) {
                    Files.createDirectories(targetFile.getParent());
                }
                Files.createFile(targetFile);
                Files.write(targetFile, "Welcome to WikiCraft!".getBytes());
                loadContentFromFile(targetFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleBackspacePress() {
        saveContent();

        String previousFilePath = navigationStack.pop();
        Path previousFile = Paths.get(previousFilePath);

        if (Files.exists(previousFile)) loadContentFromFile(previousFile);
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

        insertModeContent = htmlEditor.getHtmlText();
        detectPatterns();
        viewModeContent = insertModeContent;
        transformToViewModeContent();
        htmlEditor.setHtmlText(viewModeContent);

        saveContent();

        if (titleBar != null) titleBar.setPencilButtonIcon("\uD83D\uDC41");
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

        htmlEditor.setHtmlText(insertModeContent);

        if (titleBar != null) titleBar.setPencilButtonIcon("\uD83D\uDD89");
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
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to format or save content to " + currentContentFile);
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
        refreshTreeView();
    }

    private void refreshTreeView() {
        if (searchMenu != null) {
            for (Node node : searchMenu.getChildren()) {
                if (node instanceof TreeView) {
                    TreeView<Path> treeView = (TreeView<Path>) node;
                    treeView.refresh();
                    expandAndSelectCurrentFile(treeView, treeView.getRoot());
                    break;
                }
            }
        }
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
        if (str == null) return null;
        StringBuilder out = new StringBuilder(str.length());
        for (char ch : str.toCharArray()) {
            if (ch > 0xfff) out.append("\\u").append(hex(ch));
            else if (ch > 0xff) out.append("\\u0").append(hex(ch));
            else if (ch > 0x7f) out.append("\\u00").append(hex(ch));
            else if (ch < 32) {
                out.append(switch (ch) {
                    case '\b' -> "\\b";
                    case '\n' -> "\\n";
                    case '\t' -> "\\t";
                    case '\f' -> "\\f";
                    case '\r' -> "\\r";
                    default -> ch > 0xf ? "\\u00" + hex(ch) : "\\u000" + hex(ch);
                });
            } else {
                switch (ch) {
                    case '\'': if (escapeSingleQuote) out.append('\\'); out.append('\''); break;
                    case '"': out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    case '/': if (escapeForwardSlash) out.append('\\'); out.append('/'); break;
                    default: out.append(ch); break;
                }
            }
        }
        return out.toString();
    }


    private static String hex(int i) {
        return Integer.toHexString(i);
    }

    private void runOnUiThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
