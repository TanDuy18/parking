package org.example.duanparking.client.controller;

import com.github.sarxos.webcam.Webcam;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.controlsfx.control.PrefixSelectionChoiceBox;
import org.example.duanparking.common.dto.ParkingHistoryDTO;
import org.example.duanparking.common.dto.ParkingSlotDTO;

import org.example.duanparking.common.dto.VehicleDTO;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.model.DisplayMode;


import java.awt.*;
import java.awt.image.BufferedImage;

import java.net.URL;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class DasboardController implements Initializable {
    @FXML
    private ImageView cameraView;
    @FXML
    private Button acceptBtn;
    @FXML
    private Button outBtn;
    @FXML
    private Button inBtn;
    @FXML
    private Button rentBtn;
    @FXML
    private GridPane parkingGrid;

    @FXML
    private AnchorPane outPane;
    @FXML
    private AnchorPane inPane;

    @FXML
    private TextField plateField1;
    @FXML
    private TextField placeField1;
    @FXML
    private TextField arrivalTimeField1;

    @FXML
    private ComboBox<String> InforField1;

    @FXML
    private TextField transaction_id_Field0;
    @FXML
    private TextField InforField0;
    @FXML
    private TextField arrivalTimeField0;
    @FXML
    private TextField leaveTimeField0;
    @FXML
    private TextField placeField0;
    @FXML
    private TextField plateField0;
    @FXML
    private TextField priceField0;


    @FXML
    private PrefixSelectionChoiceBox<String> serverRmi;

    private ParkingInterface parkingInterface;
    private ParkingSlotDTO outData;
    private ClientCallback clientCallback;
    private static RmiClientManager manager;
    private Webcam webcam;
    private boolean cameraActive = true;


    private DetectPlate detectPlate;
    private int frameCount = 0;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private PauseTransition debounce = new PauseTransition(Duration.millis(500));
    private PauseTransition debounceOut = new PauseTransition(Duration.millis(500));
    private PauseTransition debounceIn = new PauseTransition(Duration.millis(500));

    int checkInOut = 0;

    ParkingGridManager gridManager;

    private void startWebcam() {
        Thread thread = new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    webcam.setViewSize(new Dimension(640, 480));
                    webcam.open();

                    while (cameraActive && webcam.isOpen()) {
                        // 3. Lấy ảnh từ Webcam (BufferedImage)
                        BufferedImage bImage = webcam.getImage();

                        if (bImage != null) {

                            Image fxImage = SwingFXUtils.toFXImage(bImage, null);

                            Platform.runLater(() -> cameraView.setImage(fxImage));
                        }
                        Thread.sleep(33);
                    }
                } else {
                    System.err.println("Không tìm thấy Webcam nào!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true); // Đóng thread khi tắt app
        thread.start();
    }

    private void openNotificationScreen(String name) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setContentText(name);

            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopWebcam() {
        cameraActive = false;
        if (webcam != null) {
            new Thread(() -> {
                try {
                    webcam.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

   public void refreshTime() {
        Timeline clock = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
        if (gridManager != null) {
            gridManager.refreshAllSlotsTime();
            System.out.println("Hệ thống vừa cập nhật thời gian thực cho các ô!");
        }
        }));

        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
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

    private void checkAndSetArrivalTime() {
        boolean filled = !plateField1.getText().trim().isEmpty()
                && !placeField1.getText().trim().isEmpty() && InforField1.getSelectionModel().getSelectedItem() != null;

        if (!filled) {
            arrivalTimeField1.clear();
            return;
        }

        if (!arrivalTimeField1.getText().trim().isEmpty())
            return;

        LocalDateTime time = LocalDateTime.now();
        arrivalTimeField1.setText(time.format(dateTimeFormatter));
    }

    private void firstRun() {
        inPane.setVisible(true);
        outPane.setVisible(false);
        checkInOut = 1;
        outBtn.setDisable(false);
        inBtn.setDisable(true);

    }

    private void autoFillOutInfo() {
        String plate = plateField0.getText().trim();
        if (plate.isEmpty())
            return;

        new Thread(() -> {
            try {
                ParkingSlotDTO data = parkingInterface.getVehicleInfoForOut(plate);
                outData = data;
                if (data == null) {
                    Platform.runLater(() -> openNotificationScreen("Không tìm thấy xe trong bãi!"));
                    return;
                }

                Platform.runLater(() -> {
                    transaction_id_Field0.setText(String.valueOf(data.getParkingHistory().getTransactionId()));
                    placeField0.setText(data.getSpotId());
                    InforField0.setText(data.getVehicle().getVehicleType());
                    arrivalTimeField0.setText(data.getParkingHistory().getEntryTime().format(formatter));
                    leaveTimeField0.setText(data.getParkingHistory().getExitTime().format(formatter));
                    priceField0.setText(formatVND(data.getParkingHistory().getFee()));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void autoFillInInfo() {
        String plate = plateField1.getText().trim();
        if (plate.length() < 4) return; // Chỉ tìm khi biển số đủ dài

        new Thread(() -> {
            try {
                ParkingSlotDTO data = parkingInterface.getVehicleInfo(plate, DisplayMode.DASHBOARD);

                Platform.runLater(() -> {
                    if (data != null && data.getVehicle() != null) {
                        // 1. Tự động chọn loại xe
                        InforField1.setValue(data.getVehicle().getVehicleType());
                        if (data.getSpotId() != null) {
                            placeField1.setText(data.getSpotId());
                            if (gridManager != null) {
                                gridManager.highlightSlotById(data.getSpotId());
                            }
                            if (data.getCurrentRent() != null) {
                                if (data.getCurrentRent().getSchedules() == null || data.getCurrentRent().getSchedules().isEmpty()) {
                                    System.out.println("Cảnh báo: Xe " + plate + " không có lịch hôm nay.");
                                    plateField1.setStyle("-fx-border-color: orange;");
                                } else {
                                    plateField1.setStyle("-fx-border-color: green;");
                                }
                            }
                        }
                    } else {
                        // Xe vãng lai mới hoặc không tìm thấy: reset để nhập tay
                        placeField1.clear();
                        plateField1.setStyle(null);
                    }

                    // Sau khi auto-fill xong, cập nhật giờ đến nếu form đã đủ thông tin
                    checkAndSetArrivalTime();
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }).start();
    }



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        firstRun();
        refreshTime(); 
        startWebcam();
        try {
            manager = RmiClientManager.getInstance();
            manager.connect();

            parkingInterface = manager.getParkingInterface();
            clientCallback = manager.getClientCallBack();

            List<ParkingSlotDTO> slots = parkingInterface.getAllSlots();
            gridManager = new ParkingGridManager(parkingGrid);
            gridManager.setCurrentMode(DisplayMode.DASHBOARD);
            gridManager.updateGrid(slots);


            manager.setGridManager(gridManager);

            if (!manager.isRegistered()) {
                parkingInterface.registerClient(clientCallback);
                manager.setRegistered(true);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        InforField1.getItems().addAll("CAR", "MOTORBIKE", "TRUCK", "BICYCLE");
        inBtn.setOnAction(event -> {
            inBtn.setDisable(true);
            outBtn.setDisable(false);
            inPane.setVisible(true);
            outPane.setVisible(false);
            checkInOut = 1;
        });
        /*
         * 1 là in và 0 là out
         */
        outBtn.setOnAction(event -> {
            outBtn.setDisable(true);
            inBtn.setDisable(false);
            inPane.setVisible(false);
            outPane.setVisible(true);
            checkInOut = 0;
        });

        debounce.setOnFinished(e -> checkAndSetArrivalTime());
        plateField1.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        placeField1.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        InforField1.valueProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());

        debounceOut.setOnFinished(e -> autoFillOutInfo());
        plateField0.textProperty().addListener((obs, oldVal, newVal) -> {
            clearOutFields();
            debounceOut.playFromStart();});

        debounceIn.setOnFinished(e -> autoFillInInfo());
        plateField1.textProperty().addListener((obs, oldVal, newVal) -> {clearInFields();debounceIn.playFromStart();});


        gridManager.setClickHandler(((slot, pane) -> {
           if(inPane.isVisible()) {
               placeField1.setText(slot.getSpotId());
               placeField0.clear();
           }else{
               placeField0.setText(slot.getSpotId());
               placeField1.clear();
           }

        }));
    }

    @FXML
    void acceptHandle(ActionEvent event) {
        switch (checkInOut) {
            case 1: // XE VÀO
                LocalDateTime current = LocalDateTime.now();
                String plateName1 = plateField1.getText().trim();
                String place = placeField1.getText().trim();
                String vehicleInfor = InforField1.getSelectionModel().getSelectedItem();

                // 1. Kiểm tra nhanh ở Client
                if (plateName1.isEmpty() || place.isEmpty() || vehicleInfor == null) {
                    openNotificationScreen("Vui lòng nhập đầy đủ thông tin (Biển số, Vị trí, Loại xe)");
                    return;
                }

                new Thread(() -> {
                    try {
                        // 2. Tạo cấu trúc DTO đầy đủ (Quan trọng nhất)
                        ParkingSlotDTO slot = new ParkingSlotDTO();
                        VehicleDTO vehicle = new VehicleDTO();
                        ParkingHistoryDTO history = new ParkingHistoryDTO();

                        // Gán thông tin xe
                        vehicle.setPlateNumber(plateName1);
                        vehicle.setVehicleType(vehicleInfor);
                        // Nếu có thêm Brand/Owner thì set ở đây (ví dụ lấy từ autoFill)

                        // Gán lịch sử vào
                        history.setEntryTime(current);

                        // Gán mọi thứ vào Slot
                        slot.setSpotId(place);
                        slot.setStatus("OCCUPIED");
                        slot.setVehicle(vehicle);         // SỬA LỖI NPE TẠI ĐÂY
                        slot.setParkingHistory(history); // SỬA LỖI NPE TẠI ĐÂY

                        // 3. Gọi Server (Server sẽ tự check exists, check Renter, check Occupied)
                        int result = parkingInterface.updateSlotStatus(slot);

                        Platform.runLater(() -> {
                            switch (result) {
                                case 0:
                                    openNotificationScreen("Cho xe vào thành công!");
                                    clearInFields(); // Hàm xóa trắng đã viết ở trên
                                    plateField1.clear();
                                    break;
                                case 1:
                                    openNotificationScreen("Lỗi: Vị trí này hiện đã có xe khác đỗ!");
                                    break;
                                case 3:
                                    openNotificationScreen("Lỗi: Không tìm thấy mã vị trí này trong hệ thống!");
                                    break;
                                case 4:
                                    openNotificationScreen("Lỗi: Xe này hiện đã có trong bãi rồi!");
                                    break;
                                case 6:
                                    openNotificationScreen("Lỗi: Đây là vị trí ưu tiên đã được khách khác đặt thuê!");
                                    break;
                                case 2:
                                    openNotificationScreen("Hệ thống đang bận (tranh chấp), vui lòng thử lại!");
                                    break;
                                default:
                                    openNotificationScreen("Lỗi hệ thống không xác định (Mã: " + result + ")");
                                    break;
                            }
                        });
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> openNotificationScreen("Lỗi kết nối Server!"));
                    }
                }).start();
                break;

            case 0: // XE RA
                if (outData == null) {
                    openNotificationScreen("Vui lòng nhập biển số và tìm xe trước khi bấm Chấp nhận!");
                    return;
                }
                new Thread(() -> {
                    try {
                        boolean check = parkingInterface.takeVehicleOut(outData);
                        Platform.runLater(() -> {
                            if (check) {
                                openNotificationScreen("Xe ra thành công!");
                                clearOutFields();
                                plateField0.clear();
                                outData = null;
                            } else {
                                openNotificationScreen("Lỗi khi thực hiện xe ra!");
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
        }
    }

    @FXML
    private void handleRentButton(ActionEvent event) {
        stopWebcam();
           Platform.runLater(() -> {
               try {
               FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/duanparking/rent-screen.fxml"));
               Parent root = loader.load();

               Scene newScene = new Scene(root);

               Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

               currentStage.setScene(newScene);
               currentStage.setTitle("Rent Parking");
               } catch (Exception e) {
                   e.printStackTrace();
                   openNotificationScreen("Lỗi load giao diện Rent: " + e.getMessage()); // Dùng method alert của mày
               }
           });
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        new Thread(() -> {   
            try {    
                List<ParkingSlotDTO> slots = parkingInterface.getAllSlots();
                gridManager = new ParkingGridManager(parkingGrid);
                gridManager.updateGrid(slots);
                gridManager.setCurrentMode(DisplayMode.RENT_MANAGEMENT);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }).start(); 
    }

    private void clearInFields() {
        placeField1.clear();
        arrivalTimeField1.clear();
        InforField1.setValue(null);
        plateField1.setStyle(null); // Xóa màu viền cũ (xanh/cam)
        if (gridManager != null) {
            gridManager.highlightSlotById(null); // Bỏ highlight trên bản đồ
        }
    }

    private void clearOutFields() {
        transaction_id_Field0.clear();
        InforField0.clear();
        arrivalTimeField0.clear();
        leaveTimeField0.clear();
        placeField0.clear();
        priceField0.clear();
        if (gridManager != null) {
            gridManager.highlightSlotById(null); // Bỏ highlight trên bản đồ
        }
    }

}
