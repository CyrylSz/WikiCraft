package com.example.wikicraft;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.web.HTMLEditor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSException;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Controller {
    @FXML
    private AnchorPane rootPane;

    @FXML
    private VBox rootVBox;

    @FXML
    private HTMLEditor htmlEditor;

    private boolean isInsertMode = false; // Start in View Mode

    private WebEngine engine;
    private ToolBar topToolBar;
    private ToolBar bottomToolBar;

    private CustomTitleBar titleBar;
    private Stage stage;

    private ModeInfoPopup modeInfoPopup;

    // Resize variables
    private static final double RESIZE_MARGIN = 5;
    private boolean isResizing = false;
    private double startX, startY, startWidth, startHeight;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            stage = (Stage) rootVBox.getScene().getWindow();

            // Initialize the CustomTitleBar with the stage
            titleBar = new CustomTitleBar(stage);
            rootVBox.getChildren().add(0, titleBar);

            // Add F11 key listener to the scene
            Scene scene = stage.getScene();
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F11) {
                    titleBar.toggleFullScreen();
                    event.consume();
                }
            });
            addTransparentResizeRegions();
        });
        VBox.setVgrow(htmlEditor, Priority.ALWAYS);

        // ModeInfoPopup
        modeInfoPopup = new ModeInfoPopup(200, 50);
        modeInfoPopup.setPopupColor(Color.DARKGRAY);
        modeInfoPopup.setPopupOpacity(0.85);
        modeInfoPopup.setTextFont(Font.font("Monocraft", 20));

        rootPane.getChildren().add(modeInfoPopup);

        htmlEditor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Wait for the window to be set
                newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        stage = (Stage) newWindow;

                        // Adjust the tooltip position whenever the window size changes
                        stage.heightProperty().addListener((obsHeight, oldHeight, newHeight) -> {
                            adjustTooltipPos(modeInfoPopup);
                        });
                        stage.widthProperty().addListener((obsWidth, oldWidth, newWidth) -> {
                            adjustTooltipPos(modeInfoPopup);
                        });

                        // Initial adjustment
                        adjustTooltipPos(modeInfoPopup);
                    }
                });

                addCustomButtonToHTMLEditor(htmlEditor);
                setupModeSwitching(newScene);
                enterViewMode();

                // Delay enterViewMode() by 1 second
                PauseTransition delay = new PauseTransition(Duration.seconds(1));
                delay.setOnFinished(event -> {
                    enterViewMode();
                });
                delay.play();
            }
        });
    }

    private void addTransparentResizeRegions() {
        // Create resize panes
        Pane topResizePane = new Pane();
        Pane bottomResizePane = new Pane();
        Pane leftResizePane = new Pane();
        Pane rightResizePane = new Pane();
        Pane topLeftResizePane = new Pane();
        Pane topRightResizePane = new Pane();
        Pane bottomLeftResizePane = new Pane();
        Pane bottomRightResizePane = new Pane();

        // Set cursor types
        topResizePane.setCursor(Cursor.N_RESIZE);
        bottomResizePane.setCursor(Cursor.S_RESIZE);
        leftResizePane.setCursor(Cursor.W_RESIZE);
        rightResizePane.setCursor(Cursor.E_RESIZE);
        topLeftResizePane.setCursor(Cursor.NW_RESIZE);
        topRightResizePane.setCursor(Cursor.NE_RESIZE);
        bottomLeftResizePane.setCursor(Cursor.SW_RESIZE);
        bottomRightResizePane.setCursor(Cursor.SE_RESIZE);

//        topResizePane.setStyle("-fx-background-color: rgba(255, 0, 0, 0.1);");
//        bottomResizePane.setStyle("-fx-background-color: rgba(0, 255, 0, 0.1);");
//        leftResizePane.setStyle("-fx-background-color: rgba(0, 0, 255, 0.1);");
//        rightResizePane.setStyle("-fx-background-color: rgba(255, 255, 0, 0.1);");
//        topLeftResizePane.setStyle("-fx-background-color: rgba(255, 0, 255, 0.1);");
//        topRightResizePane.setStyle("-fx-background-color: rgba(0, 255, 255, 0.1);");
//        bottomLeftResizePane.setStyle("-fx-background-color: rgba(128, 0, 128, 0.1);");
//        bottomRightResizePane.setStyle("-fx-background-color: rgba(128, 128, 0, 0.1);");

        // Position the panes using AnchorPane constraints
        AnchorPane.setTopAnchor(topResizePane, 0.0);
        AnchorPane.setLeftAnchor(topResizePane, 0.0);
        AnchorPane.setRightAnchor(topResizePane, 0.0);
        topResizePane.setPrefHeight(RESIZE_MARGIN);

        AnchorPane.setBottomAnchor(bottomResizePane, 0.0);
        AnchorPane.setLeftAnchor(bottomResizePane, 0.0);
        AnchorPane.setRightAnchor(bottomResizePane, 0.0);
        bottomResizePane.setPrefHeight(RESIZE_MARGIN);

        AnchorPane.setTopAnchor(leftResizePane, RESIZE_MARGIN);
        AnchorPane.setBottomAnchor(leftResizePane, RESIZE_MARGIN);
        AnchorPane.setLeftAnchor(leftResizePane, 0.0);
        leftResizePane.setPrefWidth(RESIZE_MARGIN);

        AnchorPane.setTopAnchor(rightResizePane, RESIZE_MARGIN);
        AnchorPane.setBottomAnchor(rightResizePane, RESIZE_MARGIN);
        AnchorPane.setRightAnchor(rightResizePane, 0.0);
        rightResizePane.setPrefWidth(RESIZE_MARGIN);

        AnchorPane.setTopAnchor(topLeftResizePane, 0.0);
        AnchorPane.setLeftAnchor(topLeftResizePane, 0.0);
        topLeftResizePane.setPrefWidth(RESIZE_MARGIN);
        topLeftResizePane.setPrefHeight(RESIZE_MARGIN);

        AnchorPane.setTopAnchor(topRightResizePane, 0.0);
        AnchorPane.setRightAnchor(topRightResizePane, 0.0);
        topRightResizePane.setPrefWidth(RESIZE_MARGIN);
        topRightResizePane.setPrefHeight(RESIZE_MARGIN);

        AnchorPane.setBottomAnchor(bottomLeftResizePane, 0.0);
        AnchorPane.setLeftAnchor(bottomLeftResizePane, 0.0);
        bottomLeftResizePane.setPrefWidth(RESIZE_MARGIN);
        bottomLeftResizePane.setPrefHeight(RESIZE_MARGIN);

        AnchorPane.setBottomAnchor(bottomRightResizePane, 0.0);
        AnchorPane.setRightAnchor(bottomRightResizePane, 0.0);
        bottomRightResizePane.setPrefWidth(RESIZE_MARGIN);
        bottomRightResizePane.setPrefHeight(RESIZE_MARGIN);

        // Add resize controls to the panes
        addResizeControl(topResizePane, Cursor.N_RESIZE);
        addResizeControl(bottomResizePane, Cursor.S_RESIZE);
        addResizeControl(leftResizePane, Cursor.W_RESIZE);
        addResizeControl(rightResizePane, Cursor.E_RESIZE);
        addResizeControl(topLeftResizePane, Cursor.NW_RESIZE);
        addResizeControl(topRightResizePane, Cursor.NE_RESIZE);
        addResizeControl(bottomLeftResizePane, Cursor.SW_RESIZE);
        addResizeControl(bottomRightResizePane, Cursor.SE_RESIZE);

        // Add the panes to rootPane
        rootPane.getChildren().addAll(
                topResizePane, bottomResizePane, leftResizePane, rightResizePane,
                topLeftResizePane, topRightResizePane, bottomLeftResizePane, bottomRightResizePane
        );
    }

    private void addResizeControl(Pane pane, Cursor cursorType) {
        pane.setOnMousePressed(event -> {
            isResizing = true;
            startX = event.getScreenX();
            startY = event.getScreenY();
            startWidth = stage.getWidth();
            startHeight = stage.getHeight();
            event.consume();
        });
        pane.setOnMouseDragged(event -> {
            if (isResizing) {
                resizeWindow(event, cursorType);
                event.consume();
            }
        });
        pane.setOnMouseReleased(event -> {
            isResizing = false;
            event.consume();
        });
    }

    private void resizeWindow(MouseEvent event, Cursor cursorType) {
        double deltaX = event.getScreenX() - startX;
        double deltaY = event.getScreenY() - startY;

        if (cursorType == Cursor.E_RESIZE) {
            stage.setWidth(startWidth + deltaX);
        } else if (cursorType == Cursor.W_RESIZE) {
            stage.setX(startX + deltaX);
            stage.setWidth(startWidth - deltaX);
        } else if (cursorType == Cursor.N_RESIZE) {
            stage.setY(startY + deltaY);
            stage.setHeight(startHeight - deltaY);
        } else if (cursorType == Cursor.S_RESIZE) {
            stage.setHeight(startHeight + deltaY);
        } else if (cursorType == Cursor.NE_RESIZE) {
            stage.setWidth(startWidth + deltaX);
            stage.setY(startY + deltaY);
            stage.setHeight(startHeight - deltaY);
        } else if (cursorType == Cursor.NW_RESIZE) {
            stage.setX(startX + deltaX);
            stage.setWidth(startWidth - deltaX);
            stage.setY(startY + deltaY);
            stage.setHeight(startHeight - deltaY);
        } else if (cursorType == Cursor.SE_RESIZE) {
            stage.setWidth(startWidth + deltaX);
            stage.setHeight(startHeight + deltaY);
        } else if (cursorType == Cursor.SW_RESIZE) {
            stage.setX(startX + deltaX);
            stage.setWidth(startWidth - deltaX);
            stage.setHeight(startHeight + deltaY);
        }

        // Enforce minimum window size
        double minWidth = 450;
        double minHeight = 200;

        if (stage.getWidth() < minWidth) {
            stage.setWidth(minWidth);
        }
        if (stage.getHeight() < minHeight) {
            stage.setHeight(minHeight);
        }
    }

    private void adjustTooltipPos(ModeInfoPopup popup) {
        if (stage != null) {
            double windowWidth = stage.getWidth();
            double popupWidth = popup.getPopupWidth();
            double verticalOffset = 20;

            AnchorPane.setBottomAnchor(popup, verticalOffset);
            AnchorPane.setLeftAnchor(popup, (windowWidth - popupWidth) / 2);
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
            htmlEditor.setHtmlText("Welcome to WikiCraft!");

            // Get base64-encoded image
            String resourcePath = "/shrex.txt";
            String base64ImageData = "";

            try (InputStream inputStream = Main.class.getResourceAsStream(resourcePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                base64ImageData = reader.readLine();

            } catch (Exception e) {
                e.printStackTrace();
            }

            String img = String.format("<img alt=\"Embedded Image\" src=\"%s\" />", base64ImageData);

            // JavaScript code to insert HTML at cursor position
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
    }

    // Helper method to escape Java style strings
    private static String escapeJavaStyleString(String str,
                                                boolean escapeSingleQuote, boolean escapeForwardSlash) {
        StringBuilder out = new StringBuilder("");
        if (str == null) {
            return null;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            // Handle Unicode characters
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

    // Helper method to convert int to hex string
    private static String hex(int i) {
        return Integer.toHexString(i);
    }
}