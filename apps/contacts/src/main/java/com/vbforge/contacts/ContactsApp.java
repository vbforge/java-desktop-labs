package com.vbforge.contacts;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ContactsApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                ContactsApp.class.getResource("/contacts-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 980, 640);
        scene.getStylesheets().add(
                ContactsApp.class.getResource("/contacts.css").toExternalForm());

        stage.setTitle("Contacts");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}