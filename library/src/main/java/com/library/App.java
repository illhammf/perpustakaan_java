package com.library;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/library/view/Login.fxml"));
        Scene scene = new Scene(loader.load());

        // >>> TAMBAH INI
        scene.getStylesheets().add(getClass().getResource("/com/library/view/app.css").toExternalForm());

        stage.setTitle("Sistem Informasi Perpustakaan Kampus");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
