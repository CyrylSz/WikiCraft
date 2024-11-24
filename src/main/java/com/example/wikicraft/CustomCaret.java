package com.example.wikicraft;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import javafx.util.Duration;

public class CustomCaret {
    private String shape;
    private Color color;
    private double width;
    private double height;
    private boolean bold;
    private Node caretNode;
    private boolean isBlinking;
    private Timeline blinkTimeline;
    private CaretPositionListener listener;

    public interface CaretPositionListener {
        void updateCaretPosition(double x, double y);
    }

    public CustomCaret(String shape, Color color, double width, double height, boolean bold, boolean isBlinking, CaretPositionListener listener) {
        this.shape = shape.toLowerCase();
        this.color = color;
        this.width = width;
        this.height = height;
        this.bold = bold;
        this.isBlinking = isBlinking;
        this.listener = listener;
        createCaretNode();
        if (isBlinking) {
            setupBlinking();
        }
    }

    private void createCaretNode() {
        if ("block".equals(shape)) {
            Rectangle rect = new Rectangle(width, height, color);
            if (bold) {
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(1.5);
            }
            caretNode = rect;
        } else {
            Rectangle line = new Rectangle(width, height, color);
            if (bold) {
                line.setStroke(Color.BLACK);
                line.setStrokeWidth(1.5);
            }
            caretNode = line;
        }
    }

    public Node getCaretNode() {
        return caretNode;
    }

    public void setupCaretTracking(WebEngine engine) {
        CaretBridge bridge = new CaretBridge();
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("caretBridge", bridge);

        String script = ""
                + "document.addEventListener('selectionchange', function() {"
                + "    var sel = window.getSelection();"
                + "    if (sel.rangeCount > 0) {"
                + "        var range = sel.getRangeAt(0).cloneRange();"
                + "        if (range.getClientRects().length > 0) {"
                + "            var rect = range.getClientRects()[0];"
                + "            caretBridge.updateCaretPosition(rect.left, rect.top + rect.height);"
                + "        }"
                + "    }"
                + "});";
        engine.executeScript(script);
    }

    private void setupBlinking() {
        blinkTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), event -> caretNode.setVisible(false)),
                new KeyFrame(Duration.seconds(1.0), event -> caretNode.setVisible(true))
        );
        blinkTimeline.setCycleCount(Timeline.INDEFINITE);
        blinkTimeline.play();
    }

    public void stopBlinking() {
        if (blinkTimeline != null) {
            blinkTimeline.stop();
            caretNode.setVisible(true);
        }
    }

    public void startBlinking() {
        if (blinkTimeline != null) {
            blinkTimeline.play();
        }
    }

    private class CaretBridge {
        public void updateCaretPosition(double x, double y) {
            Platform.runLater(() -> {
                listener.updateCaretPosition(x, y);
            });
        }
    }
}
