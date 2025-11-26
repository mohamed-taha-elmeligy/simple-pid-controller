package com.emts.controlpid;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);


        scene.getStylesheets().add(String.valueOf(getClass().getResource("/com/emts/controlpid/style.css")));
        stage.setMaximized(true);
        stage.setTitle("PID Time Response");
        stage.setScene(scene);

        stage.setWidth(1400);
        stage.setHeight(900);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}