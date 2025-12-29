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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.example.duanparking.common.TimeSpinner;
import org.example.duanparking.common.dto.*;
import org.example.duanparking.common.dto.rent.DayRent;
import org.example.duanparking.common.dto.rent.RentEvent;
import org.example.duanparking.common.dto.rent.RentResult;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.model.DisplayMode;

import java.net.URL;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class RentController implements Initializable {
    @FXML
    private TextField brandField;
    @FXML
    private ComboBox<?> buoi2;
    @FXML
    private TimeSpinner endTimeSpinner1;
    @FXML
    private CheckBox friday;
    @FXML
    private DatePicker fromDatePicker1;
    @FXML
    private Label fromValue1;
    @FXML
    private GridPane gridPane;
    @FXML
    private CheckBox monday;
    @FXML
    private TextField moneyField1;
    @FXML
    private TextField moneyField2;
    @FXML
    private TextField ownerField;
    @FXML
    private AnchorPane pane_2;
    @FXML
    private AnchorPane pane_3;
    @FXML
    private AnchorPane pane_4;
    @FXML
    private AnchorPane pane_5;
    @FXML
    private AnchorPane pane_6;
    @FXML
    private AnchorPane pane_7;
    @FXML
    private AnchorPane pane_8;
    @FXML
    private TextField phoneFiled;
    @FXML
    private TextField placeField;
    @FXML
    private TextField plateField;
    @FXML
    private RadioButton rentByBuoi;
    @FXML
    private AnchorPane rentByBuoiPane;
    @FXML
    private RadioButton rentByKhac;
    @FXML
    private AnchorPane rentByKhacPane;
    @FXML
    private CheckBox saturday;
    @FXML
    private TimeSpinner startTimeSpinner1;
    @FXML
    private CheckBox sunday;
    @FXML
    private CheckBox thursday;
    @FXML
    private DatePicker toDatePicker1;
    @FXML
    private Label toValue1;
    @FXML
    private CheckBox tuesday;
    @FXML
    private ComboBox<String> vehicleTypeField;
    @FXML
    private CheckBox wednesday;

    @FXML
    private TimeSpinner time2_1;
    @FXML
    private TimeSpinner time2_2;
    @FXML
    private TimeSpinner time3_1;
    @FXML
    private TimeSpinner time3_2;
    @FXML
    private TimeSpinner time4_1;
    @FXML
    private TimeSpinner time4_2;
    @FXML
    private TimeSpinner time5_1;
    @FXML
    private TimeSpinner time5_2;
    @FXML
    private TimeSpinner time6_1;
    @FXML
    private TimeSpinner time6_2;
    @FXML
    private TimeSpinner time7_1;
    @FXML
    private TimeSpinner time7_2;
    @FXML
    private TimeSpinner time8_1;
    @FXML
    private TimeSpinner time8_2;

    @FXML private DatePicker date3dPicker;
    @FXML private Button changeDateBtn;
    @FXML private Button changeTimeline;
    @FXML private ComboBox<String> buoiBox;
    private ScheduledExecutorService scheduler;
    private GridPane parkingGrid;
    private ParkingInterface service; 
    private ParkingSlotDTO selectedSlot;
    private AnchorPane lastHighlightedPane;
    private ParkingGridManager parkingGridManager;
    private List<CheckBox> dayCheckBoxes;
    LocalDate today = LocalDate.now();
    private final Map<CheckBox, TimeSpinner> startTimeMap = new HashMap<>();
    private final Map<CheckBox, TimeSpinner> endTimeMap = new HashMap<>();
    private final Map<CheckBox, AnchorPane> dayToPaneMap = new HashMap<>();

    private final double totalRentFee = 0;

    private List<ParkingSlotDTO> slots;

    DateTimeFormatter fmt = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd/MM/yyyy")
            .toFormatter(Locale.ENGLISH);

    private LocalDate parseDate(String s) {
        if (s == null || s.equals("--/--/----"))
            return null;
        return LocalDate.parse(s, fmt);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            LoadUI();
            loadParkingData();
            initDayCheckboxes();
            setupAutoCalculate();

            LocalDate start = parseDate(fromValue1.getText());
            LocalDate end = parseDate(toValue1.getText());
            updateDayCheckboxState(start, end);
        });
    }

    private void initDayCheckboxes() {
        dayCheckBoxes = Arrays.asList(
                monday, tuesday, wednesday, thursday, friday, saturday, sunday);

        startTimeMap.put(monday, time2_1);
        startTimeMap.put(tuesday, time3_1);
        startTimeMap.put(wednesday, time4_1);
        startTimeMap.put(thursday, time5_1);
        startTimeMap.put(friday, time6_1);
        startTimeMap.put(saturday, time7_1);
        startTimeMap.put(sunday, time8_1);

        endTimeMap.put(monday, time2_2);
        endTimeMap.put(tuesday, time3_2);
        endTimeMap.put(wednesday, time4_2);
        endTimeMap.put(thursday, time5_2);
        endTimeMap.put(friday, time6_2);
        endTimeMap.put(saturday, time7_2);
        endTimeMap.put(sunday, time8_2);

        dayToPaneMap.put(monday, pane_2);
        dayToPaneMap.put(tuesday, pane_3);
        dayToPaneMap.put(wednesday, pane_4);
        dayToPaneMap.put(thursday, pane_5);
        dayToPaneMap.put(friday, pane_6);
        dayToPaneMap.put(saturday, pane_7);
        dayToPaneMap.put(sunday, pane_8);

        dayToPaneMap.values().forEach(pane -> pane.setVisible(false));

        for (CheckBox cb : dayCheckBoxes) {
            cb.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                AnchorPane pane = dayToPaneMap.get(cb);
                if (pane != null) {
                    pane.setVisible(isNowSelected);
                }

                TimeSpinner start = startTimeMap.get(cb);
                TimeSpinner end = endTimeMap.get(cb);
                if (isNowSelected && !wasSelected) {
                    LocalTime now = LocalTime.now();

                    LocalTime suggestedStart = now.withMinute(0).withSecond(0);

                    if (start != null) {
                        start.getValueFactory().setValue(suggestedStart);
                    }

                    if (end != null) {
                        LocalTime suggestedEnd;
                        if (now.isBefore(LocalTime.NOON)) {
                            suggestedEnd = LocalTime.of(12, 0);
                        } else {
                            suggestedEnd = LocalTime.of(18, 0);
                        }
                        end.getValueFactory().setValue(suggestedEnd);
                    }
                }

                if (!isNowSelected) {
                    if (start != null)
                        start.getValueFactory().setValue(LocalTime.of(7, 0));
                    if (end != null)
                        end.getValueFactory().setValue(LocalTime.of(18, 0));
                }
            });
        }
    }

    private List<DayRent> getSelectedDayList() {
        List<DayRent> days = new ArrayList<>(); 

        for(CheckBox cb : dayCheckBoxes) {
            if(cb.isSelected()) {
                TimeSpinner startTime = startTimeMap.get(cb); 
                TimeSpinner endTime = endTimeMap.get(cb); 

                LocalTime start = startTime.getValue();
                LocalTime end = endTime.getValue();   

                String dayOfWeek = switch (cb.getId()) {
                    case "tuesday" -> "TUESDAY";
                    case "wednesday" -> "WEDNESDAY";
                    case "thursday" -> "THURSDAY"; 
                    case "friday" -> "FRIDAY";
                    case "saturday" -> "SATURDAY" ;
                    case "sunday" -> "SUNDAY"; 
                    default -> "MONDAY" ;
                }; 
                days.add(new DayRent(dayOfWeek, start, end)); 
            }
        }
        return days;
    }

    private void loadParkingData() {
        try {
            service = RmiClientManager.getInstance().getParkingInterface();
            parkingGridManager = new ParkingGridManager(gridPane);
            slots = service.getAllSlots();
            parkingGridManager.setCurrentMode(DisplayMode.RENT_MANAGEMENT);
            parkingGridManager.updateGrid(slots);


            parkingGridManager.setClickHandler((slot, pane) -> {
                if (!"FREE".equalsIgnoreCase(slot.getStatus())) {
                   new Thread(() -> {


                    
                    }).start(); 
                }
                System.out.println(slot.getAreaType());
                selectedSlot = slot;
                lastHighlightedPane = pane;
                placeField.setText(slot.getSpotId());
                calculateTotalFee();

                System.out.println("Đã chọn chỗ thuê: " + slot.getSpotId());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateTotalFee() {
        if (selectedSlot == null || vehicleTypeField.getValue() == null) {
            moneyField2.setText("0 ₫");
            return;
        }

        String areaType = selectedSlot.getAreaType();
        String vehicleType = vehicleTypeField.getValue();
        String fromStr = fromValue1.getText();
        String toStr = toValue1.getText();

        boolean invalidFrom = fromStr == null || fromStr.equals("--/--/----");
        boolean invalidTo   = toStr == null || toStr.equals("--/--/----");


        if (invalidTo) {
            toStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        LocalDate from, to;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            from = LocalDate.parse(fromStr, fmt);
            to = LocalDate.parse(toStr, fmt);
        } catch (Exception e) {
            showAlert("Lỗi định dạng");
            return;
        }

        if (from.isAfter(to)) {
            showAlert("Ngày bắt đầu > ngày kết thức");
            return;
        }


        try {
            RentEvent eventToCalc = new RentEvent(vehicleType, placeField.getText(), areaType, from, to, getSelectedDayList(), 0.0);
            RentEvent result = service.calculateRentPrice(eventToCalc);

           Platform.runLater(() -> {
               moneyField2.setText(formatVND(result.getTotalAmount()));
           });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupAutoCalculate() {
        // Khi tick checkbox
        for (CheckBox cb : dayCheckBoxes) {
            cb.selectedProperty().addListener(o -> calculateTotalFee());
            startTimeMap.get(cb).valueProperty().addListener(o -> calculateTotalFee());
            endTimeMap.get(cb).valueProperty().addListener(o -> calculateTotalFee());
        }

        vehicleTypeField.valueProperty().addListener(o -> calculateTotalFee());
    }


    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private String formatVND(double money) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(money);
    }

    private double unformatVND(String vndText) {
        try {
            vndText = vndText.replace("₫", "").replace(" ", "").replace(".", "");

            NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
            Number number = format.parse(vndText);

            return number.doubleValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @FXML
    public void acceptRent(ActionEvent event) {
        String license = plateField.getText().trim();
        String brand = brandField.getText().trim();
        String vehicleType = vehicleTypeField.getValue();
        String phoneNumber = phoneFiled.getText().trim();
        String ownerName = ownerField.getText().trim();
        String place = placeField.getText().trim();
        double price = unformatVND(moneyField2.getText().trim());
        String area_style = selectedSlot.getAreaType().trim();

        LocalDate fromDate = parseDate(fromValue1.getText());
        LocalDate toDate = parseDate(toValue1.getText());

        if(toDate == null ) {
            toDate = today; 
        }

        if(license.isEmpty() || brand.isEmpty() || vehicleType.isEmpty() || phoneNumber.isEmpty() || ownerName.isEmpty() || place.isEmpty()){
            showAlert("Please fill all the form");
            return;
        }


        if (!phoneNumber.matches("\\d+")) {
            showAlert("Số điện thoại lỗi định dạng");
            return;
        }


        LocalDate finalToDate = toDate;
        new Thread(() -> {

               try{
                   RentEvent rentPlace = new RentEvent(license, ownerName, phoneNumber, brand, place, vehicleType, area_style,fromDate, finalToDate, getSelectedDayList(), price);
                   RentResult result = service.acceptRentValue(rentPlace);
                   System.out.println(result.getRentId() + " " + result.getTotalAmount() );
                   System.out.println(result);
               }catch (RemoteException e){
                   e.printStackTrace();
               }
           }).start();

    }

    @FXML
    public void returnDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/duanparking/server-screen.fxml"));
            Parent root = loader.load();
            Scene newScene = new Scene(root);
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(newScene);
            currentStage.setTitle("Dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void LoadUI() {
        fromValue1.setText(LocalDate.now().format(fmt));
        vehicleTypeField.getItems().addAll("CAR", "MOTORBIKE", "TRUCK", "BICYCLE");
        toValue1.setText("--/--/----");
        date3dPicker.setValue(today);
        changeTimeline.setOnAction(event -> {

        });

        changeDateBtn.setOnAction(event -> {
            handleChangeDateTime();
        });

        buoiBox.getItems().addAll("MORNING", "AFTERNOON", "EVENING");
        buoiBox.setValue(buoiBox.getItems().get(0));
        date3dPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-opacity: 0.4;");
                }
            }
        });
    }

    @FXML
    public void openCalender() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/duanparking/calender-screen.fxml"));
            Parent root = loader.load();

            CalenderScreenController calCtrl = loader.getController();
            calCtrl.setRentController(this);
            calCtrl.setInitialDates(parseDate(fromValue1.getText()),
                    parseDate(toValue1.getText()));

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateRentDates(LocalDate start, LocalDate end) {
        Platform.runLater(() -> {
            fromValue1.setText(start != null ? start.format(fmt) : "--/--/----");
            toValue1.setText(end != null ? end.format(fmt) : "--/--/----");

            updateDayCheckboxState(start, end);
        });
    }

    private void updateDayCheckboxState(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            for (CheckBox cb : dayCheckBoxes) {
                cb.setDisable(false);
            }
            return;
        }
        if (start != null && end == null) {
            end = start;
        }


        Map<CheckBox, java.time.DayOfWeek> map = Map.of(
                monday, java.time.DayOfWeek.MONDAY,
                tuesday, java.time.DayOfWeek.TUESDAY,
                wednesday, java.time.DayOfWeek.WEDNESDAY,
                thursday, java.time.DayOfWeek.THURSDAY,
                friday, java.time.DayOfWeek.FRIDAY,
                saturday, java.time.DayOfWeek.SATURDAY,
                sunday, java.time.DayOfWeek.SUNDAY
        );

        for (CheckBox cb : map.keySet()) {
            java.time.DayOfWeek dow = map.get(cb);
            boolean available = rangeContainsDay(start, end, dow);

            cb.setDisable(!available);
            if (!available) {
                cb.setSelected(false);
                AnchorPane pane = dayToPaneMap.get(cb);
                if (pane != null) pane.setVisible(false);
            }
        }
    }
    @FXML
    void handleRefresh(ActionEvent event) {
        Platform.runLater(() -> {
            try {


                
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        });
    }

    private boolean rangeContainsDay(LocalDate start, LocalDate end, java.time.DayOfWeek target) {
        LocalDate date = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek() == target) return true;
            date = date.plusDays(1);
        }
        return false;
    }

    private void handleChangeDateTime() {
        LocalDate selectedDate = date3dPicker.getValue();
        String selectedSession = buoiBox.getValue();

        if (selectedDate == null) {
            showAlert("Vui lòng chọn ngày!");
            return;
        }
        if (selectedSession == null) {
            showAlert("Vui lòng chọn buổi!");
            return;
        }
        Platform.runLater(() -> {
            try {
                updateGridForSession(selectedDate, selectedSession);
            } catch (Exception e) {
                showAlert("Lỗi cập nhật!");
                e.printStackTrace();
            }
        });
    }
    private void updateGridForSession(LocalDate date, String session) {
        try {
            List<String> rentedSpots = service.getRentalSpotOnDayWithSession(date, session);

            Set<String> rentedSpotIds = rentedSpots.stream()
                    .map(s -> s.split(":")[0])
                    .collect(Collectors.toSet());

            for (Node node : gridPane.getChildren()) {
                if (node instanceof AnchorPane pane && pane.getId() != null && pane.getId().startsWith("slot")) {

                    String spotId = pane.getId().substring(4); // slotA01 -> A01
                    String color;

                    if (rentedSpotIds.contains(spotId)) {
                        // 🔥 Slot đang thuê → màu tím
                        color = "#b266ff"; // tím nhạt
                    } else {
                        // Slot không thuê → lấy màu theo areaType
                        color = "#44aa44"; // default NORMAL green

                        for (ParkingSlotDTO s : slots) {
                            if (s.getSpotId().equals(spotId)) {

                                if ("FREE".equalsIgnoreCase(s.getStatus())) {
                                    color = switch (s.getAreaType()) {
                                        case "PREMIUM" -> "#3388ff"; // xanh premium
                                        case "MOTOR" -> "#ffaa00";
                                        case "EV" -> "#44ff44";
                                        default -> "#44aa44";
                                    };
                                } else {
                                    color = "#ff4444"; // occupied, reserved, rented...
                                }
                                break;
                            }
                        }
                    }

                    pane.setStyle(
                            "-fx-background-color: " + color + ";" +
                                    "-fx-border-color: black;" +
                                    "-fx-border-width: 1;"
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
