package com.example.wikicraft;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

public class CustomTitleBar extends HBox {
    private double xOffset = 0;
    private double yOffset = 0;
    private Stage stage;

    private double prevX, prevY, prevWidth, prevHeight;
    private boolean isMaximized = false;
    private boolean isFullScreen = false;

    // Scaling factor for customizing title bar size
    private static double scaling = 1.25;

    private String hoverHighlightColor = "rgba(36, 184, 191, 0.3)";
    private String clickHighlightColor = "rgba(36, 184, 191, 0.6)";

    private boolean isDragging = false;

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
        Button pencilButton = new Button("\uD83D\uDD89");
        Button optionsButton = new Button("\u2699");
        Button searchButton = new Button("\uD83D\uDD0D");

        for (Button btn : new Button[]{pencilButton, optionsButton, searchButton}) {
            btn.getStyleClass().add("title-bar-button");
            btn.setStyle("-fx-font-size: " + (12 * scaling) + "px;");
            btn.setOnAction(e -> {
                // Define actions for each button in the future
            });
        }

        HBox centerBox = new HBox(pencilButton, optionsButton, searchButton);
        centerBox.setSpacing(10 * scaling);

        // Right side: Minimize, Maximize, Close buttons
        Button minimizeButton = new Button("\u2015");
        Button maximizeButton = new Button("\u2610");
        Button closeButton = new Button("\u2716");

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

    private void enableWindowDragging() {
        this.setOnMousePressed(event -> {
            if (stage != null) {
                isDragging = true;
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
                event.consume();
            }
        });

        this.setOnMouseDragged(event -> {
            if (stage != null && isDragging) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);

                checkForSnapping(event);
                event.consume();
            }
        });

        this.setOnMouseReleased(event -> {
            isDragging = false;
            event.consume();
        });
    }

    private void checkForSnapping(MouseEvent event) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        double mouseX = event.getScreenX();
        double mouseY = event.getScreenY();

        int edgeThreshold = 1;

        // Maximize when dragged to the top edge
        if (mouseY <= bounds.getMinY() + edgeThreshold) {
            if (!isMaximized) {
                toggleMaximize();
            }
        }
        // Minimize when dragged to the bottom edge
        else if (mouseY >= bounds.getMaxY() - edgeThreshold) {
            stage.setIconified(true);
        }
        // Snap to left half
        else if (mouseX <= bounds.getMinX() + edgeThreshold) {
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight());
            isMaximized = false;
        }
        // Snap to right half
        else if (mouseX >= bounds.getMaxX() - edgeThreshold) {
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight());
            isMaximized = false;
        }
        // Snap to top-left quarter
        else if (mouseX <= bounds.getMinX() + edgeThreshold && mouseY <= bounds.getMinY() + edgeThreshold) {
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
        }
        // Snap to bottom-left quarter
        else if (mouseX <= bounds.getMinX() + edgeThreshold && mouseY >= bounds.getMaxY() - edgeThreshold) {
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY() + bounds.getHeight() / 2);
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
        }
        // Snap to top-right quarter
        else if (mouseX >= bounds.getMaxX() - edgeThreshold && mouseY <= bounds.getMinY() + edgeThreshold) {
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
        }
        // Snap to bottom-right quarter
        else if (mouseX >= bounds.getMaxX() - edgeThreshold && mouseY >= bounds.getMaxY() - edgeThreshold) {
            stage.setX(bounds.getMinX() + bounds.getWidth() / 2);
            stage.setY(bounds.getMinY() + bounds.getHeight() / 2);
            stage.setWidth(bounds.getWidth() / 2);
            stage.setHeight(bounds.getHeight() / 2);
            isMaximized = false;
        }
    }

    private void toggleMaximize() {
        if (stage != null) {
            if (!isMaximized) {
                // Store current window position and size
                prevX = stage.getX();
                prevY = stage.getY();
                prevWidth = stage.getWidth();
                prevHeight = stage.getHeight();

                // Maximize the window
                Screen screen = Screen.getPrimary();
                Rectangle2D bounds = screen.getVisualBounds();

                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());

                isMaximized = true;
            } else {
                // Restore window size and position
                stage.setX(prevX);
                stage.setY(prevY);
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);

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
}
