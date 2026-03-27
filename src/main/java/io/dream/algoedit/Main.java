package io.dream.algoedit;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("app-layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Load stylesheet
        String css = Main.class.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("algoEdit");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.show();
    }
}
