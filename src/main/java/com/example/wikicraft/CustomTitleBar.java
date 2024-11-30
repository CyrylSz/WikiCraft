package com.example.wikicraft;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import com.jfoenix.controls.JFXButton;

public class CustomTitleBar extends HBox {
    private double xOffset = 0;
    private double yOffset = 0;
    private Stage stage;

    private double prevX, prevY, prevWidth, prevHeight;
    private boolean isMaximized = false;
    public boolean isFullScreen = false;
    private boolean isSnapped = false;

    // Scaling factor for customizing title bar size
    private static double scaling = 1.10;

    private String hoverHighlightColor = "rgba(36, 184, 191, 0.3)";
    private String clickHighlightColor = "rgba(36, 184, 191, 0.6)";

    private boolean isDragging = false;

    private static final double RESIZE_MARGIN = 5;
    private boolean isResizing = false;
    private double startX, startY, startWidth, startHeight;

    private JFXButton optionsButton;
    private JFXButton pencilButton;
    private JFXButton searchButton;


    public void setHoverHighlightColor(Color color) {
        if (color != null) {
            this.hoverHighlightColor = toRgbaString(color);
            updateStyles();
        }
    }

    public void setClickHighlightColor(Color color) {
        if (color != null) {
            this.clickHighlightColor = toRgbaString(color);
            updateStyles();
        }
    }

    public static void setScaling(double scalingFactor) {
        if (scalingFactor > 0) {
            scaling = scalingFactor;
        } else {
            scaling = 1;
        }
    }

    public CustomTitleBar(Stage stage) {
        this.stage = stage;
        initTitleBar();
    }

    private void initTitleBar() {
        this.setStyle("-fx-background-color: #2e2e2e;");
        this.setPadding(new Insets(5 * scaling));
        this.setSpacing(10 * scaling);

        this.setPrefHeight(30 * scaling);

        // Left side: App icon and name
        ImageView appIcon;
        try {
            appIcon = new ImageView(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            System.err.println("Icon not found. Using default icon.");
            appIcon = new ImageView();
        }
        appIcon.setFitWidth(20 * scaling);
        appIcon.setFitHeight(20 * scaling);
        Label appName = new Label("WikiCraft");
        appName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (14 * scaling) + "px; -fx-font-family: 'Monocraft'");

        HBox leftBox = new HBox(appIcon, appName);
        leftBox.setSpacing(5 * scaling);

        // Center: 3 custom buttons
        searchButton = createButton("\uD83D\uDD0D");
        pencilButton = createButton("\uD83D\uDD89");
        optionsButton = createButton("\u2699");

        for (JFXButton btn : new JFXButton[]{searchButton, pencilButton, optionsButton}) {
            btn.getStyleClass().add("title-bar-button");
            btn.setStyle("-fx-font-size: " + (12 * scaling) + "px;");
            btn.setOnAction(e -> {
                // Define actions for each button in the future
            });
        }

        HBox centerBox = new HBox(searchButton, pencilButton, optionsButton);
        centerBox.setSpacing(10 * scaling);

        // Right side: Minimize, Maximize, Close buttons
        JFXButton minimizeButton = createButton("\u2015");
        JFXButton maximizeButton = createButton("\u2610");
        JFXButton closeButton = createButton("\u2716", true);

        for (Button btn : new Button[]{minimizeButton, maximizeButton, closeButton}) {
            btn.getStyleClass().add("title-bar-button");
            btn.setStyle("-fx-font-size: " + (12 * scaling) + "px;");
        }

        minimizeButton.setOnAction(e -> {
            if (stage != null) {
                stage.setIconified(true);
            }
        });
        maximizeButton.setOnAction(e -> {
            if (stage != null) {
                toggleMaximize();
            }
        });
        closeButton.setOnAction(e -> {
            if (stage != null) {
                stage.close();
            }
            Platform.exit();
        });

        HBox rightBox = new HBox(minimizeButton, maximizeButton, closeButton);
        rightBox.setSpacing(5 * scaling);

        HBox spacer1 = new HBox();
        HBox spacer2 = new HBox();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // Assemble title bar
        this.getChildren().addAll(leftBox, spacer1, centerBox, spacer2, rightBox);

        enableWindowDragging();

        updateStyles();
    }

    public JFXButton getPencilButton() {
        return pencilButton;
    }

    public void setPencilButtonIcon(String iconText) {
        if (pencilButton != null) {
            pencilButton.setText(iconText);
        }
    }

    public void setPencilButtonHandler(EventHandler<ActionEvent> handler) {
        if (pencilButton != null) {
            pencilButton.setOnAction(handler);
        }
    }

    public JFXButton getSearchButton() {
        return searchButton;
    }

    public void setSearchButtonIcon(String iconText) {
        if (searchButton != null) {
            searchButton.setText(iconText);
        }
    }

    public void setSearchButtonHandler(EventHandler<ActionEvent> handler) {
        if (searchButton != null) {
            searchButton.setOnAction(handler);
        }
    }

    public JFXButton getOptionsButton() {
        return optionsButton;
    }

    public void setOptionsButtonIcon(String iconText) {
        if (optionsButton != null) {
            optionsButton.setText(iconText);
        }
    }

    public void setOptionsButtonHandler(EventHandler<ActionEvent> handler) {
        if (optionsButton != null) {
            optionsButton.setOnAction(handler);
        }
    }


    private void enableWindowDragging() {
        this.setOnMousePressed(event -> {
            if (stage != null) {
                if (isSnapped) {
                    // Restore previous size and position
                    stage.setWidth(prevWidth);
                    stage.setHeight(prevHeight);
                    isSnapped = false;

                    double mouseX = event.getScreenX();
                    stage.setX(mouseX - prevWidth / 2);
                    stage.setY(event.getScreenY() - yOffset);
                }
                xOffset = event.getScreenX() - stage.getX();
                yOffset = event.getScreenY() - stage.getY();
                isDragging = true;
                this.setCursor(Cursor.MOVE);
                event.consume();
            }
        });

        this.setOnMouseDragged(event -> {
            if (stage != null && isDragging) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
                this.setCursor(Cursor.MOVE);
                event.consume();
            }
        });

        this.setOnMouseReleased(event -> {
            isDragging = false;
            checkForSnapping(event);
            this.setCursor(Cursor.DEFAULT);
            event.consume();
        });
    }

    private void checkForSnapping(MouseEvent event) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        double mouseX = event.getScreenX();
        double mouseY = event.getScreenY();

        int edgeThreshold = 50;

        // Snap to top-left quarter
        if (mouseX <= bounds.getMinX() + edgeThreshold && mouseY <= bounds.getMinY() + edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
            isSnapped = true;
        }
        // Snap to bottom-left quarter
        else if (mouseX <= bounds.getMinX() + edgeThreshold && mouseY >= bounds.getMaxY() - edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY() + bounds.getHeight() / 2);
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
            isSnapped = true;
        }
        // Snap to top-right quarter
        else if (mouseX >= bounds.getMaxX() - edgeThreshold && mouseY <= bounds.getMinY() + edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
            isSnapped = true;
        }
        // Snap to bottom-right quarter
        else if (mouseX >= bounds.getMaxX() - edgeThreshold && mouseY >= bounds.getMaxY() - edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY() + bounds.getHeight() / 2);
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
            isSnapped = true;
        }
        // Maximize when dragged to the top edge
        else if (mouseY <= bounds.getMinY() + edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            isMaximized = false;
            isSnapped = true;
        }
        // Snap to left half
        else if (mouseX <= bounds.getMinX() + edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight());
            isMaximized = false;
            isSnapped = true;
        }
        // Snap to right half
        else if (mouseX >= bounds.getMaxX() - edgeThreshold) {
            if (!isSnapped) {
                savePreviousSize();
            }
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight());
            isMaximized = false;
            isSnapped = true;
        }
        // Close when dragged to the bottom edge
        else if (mouseY >= bounds.getMaxY() - edgeThreshold) {
            if (stage != null) {
                stage.close();
            }
            Platform.exit();
        }
    }

    private void savePreviousSize() {
        prevX = stage.getX();
        prevY = stage.getY();
        prevWidth = stage.getWidth();
        prevHeight = stage.getHeight();
    }

    private void toggleMaximize() {
        if (stage != null) {
            if (!isMaximized) {
                savePreviousSize();
                stage.setMaximized(true);
                isMaximized = true;
            } else {
                stage.setMaximized(false);
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);
                stage.setX(prevX);
                stage.setY(prevY);
                isMaximized = false;
            }
        }
    }

    public void toggleFullScreen() {
        if (stage != null) {
            VBox rootVBox = (VBox) stage.getScene().lookup("#rootVBox");
            if (!isFullScreen) {
                stage.setFullScreen(true);

                if (rootVBox != null) {
                    rootVBox.getChildren().remove(this);
                }

                isFullScreen = true;
            } else {
                stage.setFullScreen(false);

                if (rootVBox != null && !rootVBox.getChildren().contains(this)) {
                    rootVBox.getChildren().add(0, this); // Add to the top
                }

                isFullScreen = false;
            }
        }
    }

    private void updateStyles() {
        String css = createCssStyles();
        this.getStylesheets().clear();
        this.getStylesheets().add("data:text/css," + css.replace("\n", "%0A"));
    }

    private String createCssStyles() {
        return
                ".title-bar-button {\n" +
                        "   -fx-text-fill: white;\n" +
                        "   -fx-background-color: transparent;\n" +
                        "   -fx-background-radius: 0;\n" +
                        "   -fx-border-width: 0;\n" +
                        "}\n" +
                        ".title-bar-button:hover {\n" +
                        "   -fx-background-color: " + hoverHighlightColor + ";\n" +
                        "}\n" +
                        ".title-bar-button:pressed {\n" +
                        "   -fx-background-color: " + clickHighlightColor + ";\n" +
                        "}\n";
    }

    private String toRgbaString(Color color) {
        return String.format("rgba(%d, %d, %d, %.2f)",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255),
                color.getOpacity());
    }

    public void addTransparentResizeRegions(Pane rootPane) {
        Pane topResizePane = new Pane();
        Pane bottomResizePane = new Pane();
        Pane leftResizePane = new Pane();
        Pane rightResizePane = new Pane();
        Pane topLeftResizePane = new Pane();
        Pane topRightResizePane = new Pane();
        Pane bottomLeftResizePane = new Pane();
        Pane bottomRightResizePane = new Pane();

        topResizePane.setCursor(Cursor.N_RESIZE);
        bottomResizePane.setCursor(Cursor.S_RESIZE);
        leftResizePane.setCursor(Cursor.W_RESIZE);
        rightResizePane.setCursor(Cursor.E_RESIZE);
        topLeftResizePane.setCursor(Cursor.NW_RESIZE);
        topRightResizePane.setCursor(Cursor.NE_RESIZE);
        bottomLeftResizePane.setCursor(Cursor.SW_RESIZE);
        bottomRightResizePane.setCursor(Cursor.SE_RESIZE);

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

        addResizeControl(topResizePane, Cursor.N_RESIZE);
        addResizeControl(bottomResizePane, Cursor.S_RESIZE);
        addResizeControl(leftResizePane, Cursor.W_RESIZE);
        addResizeControl(rightResizePane, Cursor.E_RESIZE);
        addResizeControl(topLeftResizePane, Cursor.NW_RESIZE);
        addResizeControl(topRightResizePane, Cursor.NE_RESIZE);
        addResizeControl(bottomLeftResizePane, Cursor.SW_RESIZE);
        addResizeControl(bottomRightResizePane, Cursor.SE_RESIZE);

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

        double newWidth = stage.getWidth();
        double newHeight = stage.getHeight();
        double newX = stage.getX();
        double newY = stage.getY();

        double minWidth = 450;  // Minimum width of the window
        double minHeight = 200; // Minimum height of the window

        if (cursorType == Cursor.E_RESIZE) {
            newWidth = Math.max(minWidth, startWidth + deltaX);
        } else if (cursorType == Cursor.W_RESIZE) {
            if (startWidth - deltaX >= minWidth) {
                newWidth = startWidth - deltaX;
                newX = startX + deltaX; // Adjust position only if size is above minimum
            } else {
                newWidth = minWidth;
            }
        } else if (cursorType == Cursor.N_RESIZE) {
            if (startHeight - deltaY >= minHeight) {
                newHeight = startHeight - deltaY;
                newY = startY + deltaY; // Adjust position only if size is above minimum
            } else {
                newHeight = minHeight;
            }
        } else if (cursorType == Cursor.S_RESIZE) {
            newHeight = Math.max(minHeight, startHeight + deltaY);
        } else if (cursorType == Cursor.NE_RESIZE) {
            newWidth = Math.max(minWidth, startWidth + deltaX);
            if (startHeight - deltaY >= minHeight) {
                newHeight = startHeight - deltaY;
                newY = startY + deltaY;
            } else {
                newHeight = minHeight;
            }
        } else if (cursorType == Cursor.NW_RESIZE) {
            if (startWidth - deltaX >= minWidth) {
                newWidth = startWidth - deltaX;
                newX = startX + deltaX;
            } else {
                newWidth = minWidth;
            }
            if (startHeight - deltaY >= minHeight) {
                newHeight = startHeight - deltaY;
                newY = startY + deltaY;
            } else {
                newHeight = minHeight;
            }
        } else if (cursorType == Cursor.SE_RESIZE) {
            newWidth = Math.max(minWidth, startWidth + deltaX);
            newHeight = Math.max(minHeight, startHeight + deltaY);
        } else if (cursorType == Cursor.SW_RESIZE) {
            if (startWidth - deltaX >= minWidth) {
                newWidth = startWidth - deltaX;
                newX = startX + deltaX;
            } else {
                newWidth = minWidth;
            }
            newHeight = Math.max(minHeight, startHeight + deltaY);
        }

        // Apply the new size and position
        stage.setWidth(newWidth);
        stage.setHeight(newHeight);
        stage.setX(newX);
        stage.setY(newY);
    }


    private JFXButton createButton(String text, boolean isCloseButton) {
        JFXButton button = new JFXButton(text);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px;");

        if (isCloseButton) {
            // Add hover and click effects for the close button
            button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: rgba(255, 0, 0, 0.5); -fx-text-fill: white; -fx-font-size: 14px;"));
            button.setOnMousePressed(e -> button.setStyle("-fx-background-color: rgba(255, 0, 0, 0.8); -fx-text-fill: white; -fx-font-size: 14px;"));
            button.setOnMouseExited(e -> button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px;"));
        } else {
            // Add hover effect for standard buttons
            button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-font-size: 14px;"));
            button.setOnMouseExited(e -> button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px;"));
        }

        button.setCursor(Cursor.HAND);
        return button;
    }

    private JFXButton createButton(String text) {
        return createButton(text, false);
    }
}
