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
import org.example.duanparking.model.ShiftType;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class RentController implements Initializable {
    @FXML private TextField brandField;
    @FXML private ComboBox<String> buoi1;
    @FXML private ComboBox<String> buoi2;
    @FXML private TimeSpinner endTimeSpinner1;
    @FXML private TimeSpinner endTimeSpinner2;
    @FXML private ScrollPane gridWrapper;
    @FXML private TextField moneyField1;
    @FXML private TextField moneyField2;
    @FXML private TextField ownerField;
    @FXML private TextField phoneFiled;
    @FXML private TextField plateField;
    @FXML private RadioButton rentByBuoi;
    @FXML private AnchorPane rentByBuoiPane;
    @FXML private RadioButton rentByKhac;
    @FXML private AnchorPane rentByKhacPane;
    @FXML private Button returnBtn;
    @FXML private TimeSpinner startTimeSpinner1;

    @FXML private CheckBox sunday;
    @FXML private CheckBox saturday;
    @FXML private CheckBox friday;
    @FXML private CheckBox thursday;
    @FXML private CheckBox wednesday;
    @FXML private CheckBox tuesday;
    @FXML private CheckBox monday;
    @FXML private ComboBox<String> mondayShift;
    @FXML private ComboBox<String> tuesdayShift;
    @FXML private ComboBox<String> wednesdayShift;
    @FXML private ComboBox<String> thursdayShift;
    @FXML private ComboBox<String> fridayShift;
    @FXML private ComboBox<String> saturdayShift;
    @FXML private ComboBox<String> sundayShift;

    @FXML private TimeSpinner friFrom;
    @FXML private TimeSpinner friTo;
    @FXML private TimeSpinner monFrom;
    @FXML private TimeSpinner monTo;
    @FXML private TimeSpinner satFrom;
    @FXML private TimeSpinner satTo;
    @FXML private TimeSpinner sunFrom;
    @FXML private TimeSpinner sunTo;
    @FXML private TimeSpinner thuFrom;
    @FXML private TimeSpinner thuTo;
    @FXML private TimeSpinner tueFrom;
    @FXML private TimeSpinner tueTo;
    @FXML private TimeSpinner wedFrom;
    @FXML private TimeSpinner wedTo;

    @FXML private DatePicker toDatePicker1;
    @FXML private DatePicker toDatePicker2;
    @FXML private DatePicker fromDatePicker1;
    @FXML private DatePicker fromDatePicker2;

    @FXML private ComboBox<String> vehicleType;


    private List<RadioButton> dayButtons;

    private GridPane parkingGrid;
    private ParkingGridManager parkingGridManager;


    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private ParkingInterface parkingInterface;
    private List<CheckBox> dayChecks;
    private List<TimeSpinner> fromTimes;
    private List<TimeSpinner> toTimes;




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
    private ShiftType parseShift(String s) {
        if (s == null) return null;
        return switch (s) {
            case "MORNING"   -> ShiftType.MORNING;
            case "AFTERNOON" -> ShiftType.AFTERNOON;
            case "EVENING"   -> ShiftType.EVENING;
            case "ALL DAY", "FULL_DAY" -> ShiftType.ALL_DAY;
            default -> null;
        };
    }


    private void pickDay() {

        dayChecks = Arrays.asList(
                monday, tuesday, wednesday, thursday, friday, saturday, sunday);

        List<ComboBox<String>> shiftBoxes = Arrays.asList(
                mondayShift, tuesdayShift, wednesdayShift, thursdayShift,
                fridayShift, saturdayShift, sundayShift);

        fromTimes = Arrays.asList(
                monFrom, tueFrom, wedFrom, thuFrom, friFrom, satFrom, sunFrom);

        toTimes = Arrays.asList(
                monTo, tueTo, wedTo, thuTo, friTo, satTo, sunTo);


        // Load shift items
        for (ComboBox<String> cb : shiftBoxes) {
            cb.getItems().setAll("MORNING", "AFTERNOON", "EVENING", "FULL_DAY");
        }


        for (int i = 0; i < dayChecks.size(); i++) {
            CheckBox day = dayChecks.get(i);
            ComboBox<String> shift = shiftBoxes.get(i);
            TimeSpinner from = fromTimes.get(i);
            TimeSpinner to = toTimes.get(i);

            // Khi chọn shift → tự set giờ nhưng KHÔNG disable
            shift.valueProperty().addListener((obs, oldV, newV) -> {
                ShiftType st = parseShift(newV);
                if (st != null) {
                    from.getValueFactory().setValue(st.start);
                    to.getValueFactory().setValue(st.end);
                }
            });

            day.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    shift.setDisable(false);
                    from.setDisable(false);
                    to.setDisable(false);

                    // Nếu chưa chọn combo → mặc định MORNING
                    if (shift.getValue() == null) {
                        shift.setValue("MORNING");
                    }

                } else { // bỏ tick
                    shift.setDisable(true);
                    shift.setValue(null);

                    from.setDisable(true);
                    to.setDisable(true);
                }
            });

            // mặc định disable khi chưa chọn ngày
            shift.setDisable(true);
            from.setDisable(true);
            to.setDisable(true);
        }
    }



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            runGridPane();
            loadParkingData();
        });



        Platform.runLater(this::pickDay);
        buoi1.getItems().addAll("MORNING", "AFTERNOON", "EVENING", "ALL DAY");
        buoi1.valueProperty().addListener((obs, oldVal, newVal) -> getTime1());



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
    private void getTime1() {
        ShiftType st = parseShift(buoi1.getValue());
        if (st == null) return;

        startTimeSpinner1.getValueFactory().setValue(st.start);
        endTimeSpinner1.getValueFactory().setValue(st.end);

        startTimeSpinner1.setDisable(true);
        endTimeSpinner1.setDisable(true);
    }



    public void returnDashboard(ActionEvent event) {
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

    public void acceptRent(ActionEvent event){
        if (rentByBuoi.isSelected()) {
            System.out.println("→ Người dùng đang rent theo BUỔI");
            handleRentByBuoi();
        }
        else if (rentByKhac.isSelected()) {
            System.out.println("→ Người dùng đang rent theo KHÁC");
            handleRentByKhac();
        }
    }

    private void handleRentByBuoi() {

    }

    private void handleRentByKhac() {

    }
}