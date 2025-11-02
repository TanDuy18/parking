package org.example.duanparking.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class NotifiController implements Initializable {
    @FXML private Label Notification;
    @FXML private Button Button;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Button.setOnAction(event -> {
            Stage stage = (Stage)  Button.getScene().getWindow();
            stage.close();
        });
    }
}
