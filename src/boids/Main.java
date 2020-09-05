package boids;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
    private final int ENVIRONMENT_WIDTH = 1600;
    private final int ENVIRONMENTS_HEIGHT = 1020;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Scene root = FXMLLoader.load(getClass().getResource("resources/main.fxml"));
        root.getStylesheets().add(getClass().getResource("resources/application.css").toExternalForm());

        primaryStage.setScene(root);
        primaryStage.setTitle("Boid simulation v. 0.1");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

}
