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
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.duanparking.common.TimeSpinner;
import org.example.duanparking.common.dto.DayRent;
import org.example.duanparking.common.dto.RentDTO;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.model.ShiftType;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RentController implements Initializable {
    // Thông tin xe
    @FXML private TextField brandField;
    @FXML private TextField ownerField;
    @FXML private TextField phoneFiled;
    @FXML private TextField plateField;
    @FXML private ComboBox<String> vehicleTypeField;
    @FXML private TextField placeField;

    // Radio buttons
    @FXML private RadioButton rentByBuoi;
    @FXML private AnchorPane rentByBuoiPane;
    @FXML private RadioButton rentByKhac;
    @FXML private AnchorPane rentByKhacPane;

    // Grid
    @FXML private ScrollPane gridWrapper;

    // === CHẾ ĐỘ THUÊ THEO BUỔI ===
    @FXML private DatePicker fromDatePicker2;
    @FXML private DatePicker toDatePicker2;
    @FXML private TextField moneyField2;

    // Các ngày trong tuần
    @FXML private CheckBox monday;
    @FXML private CheckBox tuesday;
    @FXML private CheckBox wednesday;
    @FXML private CheckBox thursday;
    @FXML private CheckBox friday;
    @FXML private CheckBox saturday;
    @FXML private CheckBox sunday;

    // Shift cho các ngày
    @FXML private ComboBox<String> mondayShift;
    @FXML private ComboBox<String> tuesdayShift;
    @FXML private ComboBox<String> wednesdayShift;
    @FXML private ComboBox<String> thursdayShift;
    @FXML private ComboBox<String> fridayShift;
    @FXML private ComboBox<String> saturdayShift;
    @FXML private ComboBox<String> sundayShift;

    // Time spinner
    @FXML private TimeSpinner monFrom;
    @FXML private TimeSpinner monTo;
    @FXML private TimeSpinner tueFrom;
    @FXML private TimeSpinner tueTo;
    @FXML private TimeSpinner wedFrom;
    @FXML private TimeSpinner wedTo;
    @FXML private TimeSpinner thuFrom;
    @FXML private TimeSpinner thuTo;
    @FXML private TimeSpinner friFrom;
    @FXML private TimeSpinner friTo;
    @FXML private TimeSpinner satFrom;
    @FXML private TimeSpinner satTo;
    @FXML private TimeSpinner sunFrom;
    @FXML private TimeSpinner sunTo;

    private GridPane parkingGrid;
    private ParkingGridManager parkingGridManager;

    private List<CheckBox> dayChecks;
    private List<ComboBox<String>> shiftBoxes;
    private List<TimeSpinner> fromTimes;
    private List<TimeSpinner> toTimes;



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(() -> {
            runGridPane();
            loadParkingData();
            setupUI();
            setupListeners();
        });
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
            var cb = RmiClientManager.getInstance().getClientCallBack();
            service.registerClient(cb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        // Chỉ hiển thị chế độ thuê theo buổi
        rentByBuoi.setSelected(true);
        rentByBuoiPane.setVisible(true);
        rentByBuoiPane.setDisable(false);
        rentByKhacPane.setVisible(false);
        rentByKhacPane.setDisable(true);

        // Vô hiệu hóa radio button "Khác"
        rentByKhac.setDisable(true);

        // Khởi tạo vehicle type
        vehicleTypeField.getItems().addAll("CAR", "MOTORBIKE", "TRUCK", "BICYCLE");

        // Khởi tạo danh sách các control
        dayChecks = Arrays.asList(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
        shiftBoxes = Arrays.asList(mondayShift, tuesdayShift, wednesdayShift, thursdayShift,
                fridayShift, saturdayShift, sundayShift);
        fromTimes = Arrays.asList(monFrom, tueFrom, wedFrom, thuFrom, friFrom, satFrom, sunFrom);
        toTimes = Arrays.asList(monTo, tueTo, wedTo, thuTo, friTo, satTo, sunTo);

        // Khởi tạo shift cho các ngày
        for (ComboBox<String> cb : shiftBoxes) {
            cb.getItems().addAll("MORNING", "AFTERNOON", "EVENING", "FULL_DAY");
        }

        // Thiết lập listener cho từng ngày
        for (int i = 0; i < dayChecks.size(); i++) {
            CheckBox day = dayChecks.get(i);
            ComboBox<String> shift = shiftBoxes.get(i);
            TimeSpinner from = fromTimes.get(i);
            TimeSpinner to = toTimes.get(i);

            // Khi chọn shift, tự động set thời gian
            shift.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    ShiftType shiftType = ShiftType.valueOf(newV);
                    if (shiftType != null) {
                        from.getValueFactory().setValue(shiftType.start);
                        to.getValueFactory().setValue(shiftType.end);
                        calculateMoney();
                    }
                }
            });

            // Khi tick/untick ngày
            day.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    shift.setDisable(false);
                    from.setDisable(false);
                    to.setDisable(false);
                    if (shift.getValue() == null) {
                        shift.setValue("MORNING");
                    }
                } else {
                    shift.setDisable(true);
                    from.setDisable(true);
                    to.setDisable(true);
                }
                calculateMoney();
            });

            // Mặc định disable
            shift.setDisable(true);
            from.setDisable(true);
            to.setDisable(true);
        }
    }

    private void setupListeners() {
        // Listener cho thông tin cơ bản
        vehicleTypeField.valueProperty().addListener((obs, oldV, newV) -> calculateMoney());
        placeField.textProperty().addListener((obs, oldV, newV) -> calculateMoney());

        // Listener cho ngày tháng
        fromDatePicker2.valueProperty().addListener((obs, oldV, newV) -> {
            validateDate();
            calculateMoney();
        });
        toDatePicker2.valueProperty().addListener((obs, oldV, newV) -> {
            validateDate();
            calculateMoney();
        });

        // Listener cho thời gian
        for (TimeSpinner from : fromTimes) {
            from.valueProperty().addListener((obs, oldV, newV) -> calculateMoney());
        }
        for (TimeSpinner to : toTimes) {
            to.valueProperty().addListener((obs, oldV, newV) -> calculateMoney());
        }
    }

    private void validateDate() {
        LocalDate fromDate = fromDatePicker2.getValue();
        LocalDate toDate = toDatePicker2.getValue();

        if (fromDate == null || toDate == null) return;

        if (toDate.isBefore(fromDate)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Ngày kết thúc phải sau ngày bắt đầu!");
            alert.showAndWait();
            toDatePicker2.setValue(fromDate);
        }
    }

    private void calculateMoney() {
        new Thread(() -> {
            try {
                String vehicleType = vehicleTypeField.getValue();
                String place = placeField.getText();

                // Kiểm tra thông tin bắt buộc
                if (vehicleType == null || place == null || place.isEmpty() ||
                        fromDatePicker2.getValue() == null || toDatePicker2.getValue() == null) {
                    Platform.runLater(() -> moneyField2.setText("0 VND"));
                    return;
                }

                // Lấy giá từ server
                ParkingInterface service = RmiClientManager.getInstance().getParkingInterface();
                double hourlyRate = service.getHourlyRate(place, vehicleType);

                // Tính tổng tiền
                double totalMoney = 0;
                LocalDate startDate = fromDatePicker2.getValue();
                LocalDate endDate = toDatePicker2.getValue();

                // Tính số ngày trong khoảng thời gian
                long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

                // Map các ngày trong tuần sang số (1=Monday, 7=Sunday)
                Map<String, Integer> dayMap = new HashMap<>();
                dayMap.put("MONDAY", 1);
                dayMap.put("TUESDAY", 2);
                dayMap.put("WEDNESDAY", 3);
                dayMap.put("THURSDAY", 4);
                dayMap.put("FRIDAY", 5);
                dayMap.put("SATURDAY", 6);
                dayMap.put("SUNDAY", 7);

                // Tính tiền cho từng ngày được chọn
                for (int i = 0; i < dayChecks.size(); i++) {
                    if (dayChecks.get(i).isSelected()) {
                        String dayName = getDayName(i);
                        Integer dayNumber = dayMap.get(dayName);

                        if (dayNumber != null) {
                            LocalTime startTime = fromTimes.get(i).getValue();
                            LocalTime endTime = toTimes.get(i).getValue();

                            if (startTime != null && endTime != null) {

                                double hours = calculateHours(startTime, endTime);

                                long count = countWeekdays(startDate, endDate, dayNumber);

                                totalMoney += count * hours * hourlyRate;
                            }
                        }
                    }
                }

                // Cập nhật UI
                double finalTotal = totalMoney;
                Platform.runLater(() -> {
                    moneyField2.setText(String.format("%,.0f VND", finalTotal));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> moneyField2.setText("Lỗi tính toán"));
            }
        }).start();
    }

    // Hàm lấy tên ngày từ index
    private String getDayName(int index) {
        switch(index) {
            case 0: return "MONDAY";
            case 1: return "TUESDAY";
            case 2: return "WEDNESDAY";
            case 3: return "THURSDAY";
            case 4: return "FRIDAY";
            case 5: return "SATURDAY";
            case 6: return "SUNDAY";
            default: return "";
        }
    }

    // Hàm tính số giờ giữa 2 thời điểm
    private double calculateHours(LocalTime start, LocalTime end) {
        if (start == null || end == null) return 0;

        long minutes = java.time.Duration.between(start, end).toMinutes();
        double hours = minutes / 60.0;

        // Xử lý trường hợp qua đêm
        if (hours < 0) {
            hours += 24.0;
        }

        return hours;
    }

    // Hàm đếm số lần ngày xuất hiện trong khoảng
    private long countWeekdays(LocalDate startDate, LocalDate endDate, int targetDay) {
        long count = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            if (date.getDayOfWeek().getValue() == targetDay) {
                count++;
            }
            date = date.plusDays(1);
        }

        return count;
    }
    @FXML
    public void acceptRent(ActionEvent event) {
        // Kiểm tra thông tin bắt buộc
        if (plateField.getText().isEmpty()) {
            showAlert("Vui lòng nhập biển số xe!");
            return;
        }
        if (vehicleTypeField.getValue() == null) {
            showAlert("Vui lòng chọn loại xe!");
            return;
        }
        if (placeField.getText().isEmpty()) {
            showAlert("Vui lòng nhập vị trí!");
            return;
        }
        if (fromDatePicker2.getValue() == null || toDatePicker2.getValue() == null) {
            showAlert("Vui lòng chọn khoảng thời gian!");
            return;
        }

        // Kiểm tra có chọn ngày nào không
        boolean hasDaySelected = false;
        for (CheckBox day : dayChecks) {
            if (day.isSelected()) {
                hasDaySelected = true;
                break;
            }
        }

        if (!hasDaySelected) {
            showAlert("Vui lòng chọn ít nhất một ngày trong tuần!");
            return;
        }

        // Thực hiện thuê
        new Thread(() -> {
            try {
                RentDTO rent = new RentDTO();
                rent.setPlate(plateField.getText());
                rent.setBrand(brandField.getText());
                rent.setVehicleType(vehicleTypeField.getValue());
                rent.setPhone(phoneFiled.getText());
                rent.setOwner(ownerField.getText());
                rent.setPlace(placeField.getText());
                rent.setFromDate(fromDatePicker2.getValue());
                rent.setToDate(toDatePicker2.getValue());

                // Thêm các ngày được chọn
                List<DayRent> days = new ArrayList<>();
                addDayIfSelected(monday, monFrom, monTo, "MONDAY", days);
                addDayIfSelected(tuesday, tueFrom, tueTo, "TUESDAY", days);
                addDayIfSelected(wednesday, wedFrom, wedTo, "WEDNESDAY", days);
                addDayIfSelected(thursday, thuFrom, thuTo, "THURSDAY", days);
                addDayIfSelected(friday, friFrom, friTo, "FRIDAY", days);
                addDayIfSelected(saturday, satFrom, satTo, "SATURDAY", days);
                addDayIfSelected(sunday, sunFrom, sunTo, "SUNDAY", days);

                rent.setDays(days);

                // Gọi service
                ParkingInterface service = RmiClientManager.getInstance().getParkingInterface();
//                boolean success = service.rentPlace(rent);
//
//                Platform.runLater(() -> {
//                    if (success) {
//                        showAlert("Thuê chỗ đỗ thành công!");
//                        clearForm();
//                        // Refresh grid
//                        loadParkingData();
//                    } else {
//                        showAlert("Thuê chỗ đỗ thất bại! Vị trí có thể đã được thuê.");
//                    }
//                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    private void addDayIfSelected(CheckBox cb, TimeSpinner from, TimeSpinner to, String dayName, List<DayRent> days) {
        if (cb.isSelected() && from.getValue() != null && to.getValue() != null) {
            days.add(new DayRent(dayName, from.getValue(), to.getValue()));
        }
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void clearForm() {
        plateField.clear();
        brandField.clear();
        phoneFiled.clear();
        ownerField.clear();
        placeField.clear();
        vehicleTypeField.setValue(null);
        fromDatePicker2.setValue(null);
        toDatePicker2.setValue(null);
        moneyField2.setText("0");

        // Reset các ngày
        for (CheckBox day : dayChecks) {
            day.setSelected(false);
        }
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
}