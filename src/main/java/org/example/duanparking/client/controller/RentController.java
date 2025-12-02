package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.duanparking.common.TimeSpinner;
import org.example.duanparking.common.remote.ParkingInterface;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class RentController implements Initializable {
    @FXML private TimeSpinner endTimeSpinner;
    @FXML private TimeSpinner startTimeSpinner;
    @FXML private Button returnBtn;
    @FXML private ComboBox<String> buoi;
    @FXML private RadioButton rentByBuoi;
    @FXML private RadioButton rentByKhac;
    @FXML private AnchorPane rentByBuoiPane;
    @FXML private AnchorPane rentByKhacPane;
    private GridPane parkingGrid;
    private ParkingGridManager parkingGridManager;
    @FXML private ScrollPane gridWrapper;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private ParkingInterface parkingInterface;



    public ParkingGridManager getParkingGridManager() {
        return parkingGridManager;
    }

    private void runGridPane() {
        parkingGrid = new GridPane();
        parkingGrid.setHgap(5);
        parkingGrid.setVgap(5);

        parkingGrid.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        gridWrapper.setContent(parkingGrid);


        parkingGridManager = new ParkingGridManager(parkingGrid);
    }
    private void loadParkingData() {
        try {
            ParkingInterface service = RmiClientManager.getInstance().getParkingInterface();
            var slots = service.getAllSlots();

            parkingGridManager.updateGrid(slots);

            // đăng ký callback để server push sự kiện
            var cb = RmiClientManager.getInstance().getClientCallBack();
            service.registerClient(cb);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            runGridPane();
            loadParkingData();
        });
        buoi.getItems().addAll("MORNING", "AFTERNOON", "EVENING", "ALL DAY");
        buoi.valueProperty().addListener((obs, oldVal, newVal) -> getTime());

        startTimeSpinner.getValueFactory().setValue(LocalTime.of(0, 0));
        endTimeSpinner.getValueFactory().setValue(LocalTime.of(0, 0));



        ToggleGroup group = new ToggleGroup();
        rentByBuoi.setToggleGroup(group);
        rentByKhac.setToggleGroup(group);

        rentByBuoi.setOnAction(event -> {
            rentByBuoiPane.setVisible(true);rentByBuoiPane.setDisable(false);rentByKhacPane.setDisable(true);rentByKhacPane.setVisible(false);
        });

        rentByKhac.setOnAction(event -> {
            rentByKhacPane.setVisible(true);rentByKhacPane.setDisable(false);rentByBuoiPane.setDisable(true);rentByBuoiPane.setVisible(false);
        });
        rentByBuoiPane.setVisible(false);rentByBuoiPane.setDisable(true);
        rentByKhacPane.setVisible(false);rentByKhacPane.setDisable(true);

    }



    private LocalDate getMoreDay(LocalDate date) {
        return date.plusDays(7);
    }
    private void getTime() {
        String Buoi =  buoi.getValue();
        switch (Buoi) {
            case "MORNING" -> {
                startTimeSpinner.getValueFactory().setValue(LocalTime.of(7, 0));
                endTimeSpinner.getValueFactory().setValue(LocalTime.of(12, 0));
            }
            case "AFTERNOON" -> {
                startTimeSpinner.getValueFactory().setValue(LocalTime.of(12, 1));
                endTimeSpinner.getValueFactory().setValue(LocalTime.of(17, 50));
            }
            case "EVENING" -> {
                startTimeSpinner.getValueFactory().setValue(LocalTime.of(17, 51));
                endTimeSpinner.getValueFactory().setValue(LocalTime.of(6, 59));
            }
        }
    }

    public void openCalender(ActionEvent event) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/duanparking/server-screen.fxml"));
            Parent root = loader.load();


            Scene newScene = new Scene(root);


            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();


            currentStage.setScene(newScene);
            currentStage.setTitle("Rent Parking");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}