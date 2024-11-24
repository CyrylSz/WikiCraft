package com.example.wikicraft;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class Popup extends StackPane {

    private final Rectangle background;
    private final Label label;
    private final PauseTransition delay;

    private double width;
    private double height;

    public Popup(double width, double height) {
        this("Mode", width, height, Color.GRAY, 0.8, Font.font(18));
        this.width = width;
        this.height = height;
    }

    public Popup() {
        this("Mode", 200, 50, Color.GRAY, 0.8, Font.font(18));
    }

    public Popup(String text, double width, double height, Color color, double opacity, Font font) {
        this.width = width;
        this.height = height;

        background = new Rectangle(width, height);
        background.setFill(color);
        background.setOpacity(opacity);

        label = new Label(text);
        label.setFont(font);
        label.setTextFill(Color.WHITE);

        this.getChildren().addAll(background, label);
        this.setAlignment(Pos.CENTER);

        this.setVisible(false);

        delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(event -> this.setVisible(false));
    }

    public void show(String text) {
        Platform.runLater(() -> {
            label.setText(text);
            this.setVisible(true);
            delay.playFromStart();
        });
    }

    public void setPopupWidth(double width) {
        background.setWidth(width);
    }

    public void setPopupHeight(double height) {
        background.setHeight(height);
    }

    public void setPopupColor(Color color) {
        background.setFill(color);
    }

    public void setPopupOpacity(double opacity) {
        background.setOpacity(opacity);
    }

    public void setTextFont(Font font) {
        label.setFont(font);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public double getPopupHeight() {
        return height;
    }

    public double getPopupWidth() {
        return width;
    }
}
