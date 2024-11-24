package com.example.wikicraft;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("GUI.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1100, 700);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);

        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Add border and expand window
                root.setStyle("-fx-border-width: 1px; -fx-border-color: cyan;");
                stage.setWidth(stage.getWidth() + 2);
                stage.setHeight(stage.getHeight() + 2);
                stage.setX(stage.getX() - 1);
                stage.setY(stage.getY() - 1);
            } else {
                // Remove border and shrink window
                root.setStyle("-fx-border-width: 0px;");
                stage.setWidth(stage.getWidth() - 2);
                stage.setHeight(stage.getHeight() - 2);
                stage.setX(stage.getX() + 1);
                stage.setY(stage.getY() + 1);
            }
        });

        stage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) -> {
            if (isFullScreen) {
                // Ensure border is removed in full-screen mode
                root.setStyle("-fx-border-width: 0px;");
            } else {
                // Restore border when exiting full-screen mode
                if (stage.isFocused()) {
                    root.setStyle("-fx-border-width: 1px; -fx-border-color: cyan;");
                }
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}