package com.puumInc._securePassword;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * @author Muriithi_Mandela
 * @version 1.1.0
 */

public class Main extends Application {

    public static Stage stage;
    private double xOffset, yOffset;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/puumInc/_securePassword/_fxml/sample.fxml"));
        Scene scene = new Scene(root);
        scene.setOnMousePressed(event2 -> {
            xOffset = event2.getSceneX();
            yOffset = event2.getSceneY();
        });
        scene.setOnMouseDragged(event1 -> {
            primaryStage.setX(event1.getScreenX() - xOffset);
            primaryStage.setY(event1.getScreenY() - yOffset);
        });
        primaryStage.setScene(scene);
        primaryStage.setTitle("Password Generator");
        primaryStage.getIcons().addAll(
                new Image(getClass().getResource("/com/puumInc/_securePassword/_image/logo.png").toExternalForm()),
                new Image(getClass().getResource("/com/puumInc/_securePassword/_image/logo@2x.png").toExternalForm()),
                new Image(getClass().getResource("/com/puumInc/_securePassword/_image/logo@3x.png").toExternalForm()),
                new Image(getClass().getResource("/com/puumInc/_securePassword/_image/logo@4x.png").toExternalForm())
        );
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
        Main.stage = primaryStage;

    }
}
