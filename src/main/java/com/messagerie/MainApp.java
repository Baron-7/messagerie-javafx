package com.messagerie;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Point d'entrée de l'application JavaFX
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // On charge l'écran de connexion au démarrage
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 420, 460);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("Messagerie - Connexion");
        stage.setScene(scene);
        stage.setMinWidth(420);
        stage.setMinHeight(460);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
