package org.example.duanparking.client.controller;

import com.github.sarxos.webcam.Webcam;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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

import org.example.duanparking.common.dto.ParkingHistoryDTO;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.dto.VehicleDTO;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;


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
    private TextField brandField1;
    @FXML
    private TextField ownerField1;
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
    private TextField brandField0;
    @FXML
    private TextField ownerField0;
    @FXML
    private Label inLabel;
    @FXML
    private Label outLabel;

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
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    private String formatVND(double money) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(money);
    }

    private double unformatVND(String vndText) {
        try {
            // Bỏ dấu cách, bỏ ký tự ₫ nếu có
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
        boolean filled = !plateField1.getText().trim().isEmpty() && !brandField1.getText().trim().isEmpty()
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
        inLabel.setText(" ");
        outLabel.setText(" ");
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
                    transaction_id_Field0.setText(String.valueOf(data.getHistory().getTransactionId()));
                    ownerField0.setText(data.getVehicle().getOwner());
                    brandField0.setText(data.getVehicle().getBrand());
                    placeField0.setText(data.getSpotId());
                    InforField0.setText(data.getVehicle().getVehicleType());
                    arrivalTimeField0.setText(data.getHistory().getEntryTime().format(formatter));
                    leaveTimeField0.setText(data.getHistory().getExitTime().format(formatter));
                    priceField0.setText(formatVND(data.getHistory().getFee()));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void autoFillInInfo() {
        String plate = plateField1.getText().trim();
        if (plate.isEmpty())
            return;

        new Thread(() -> {
            try {
                ParkingSlotDTO data = parkingInterface.getVehicleInfoForIn(plate);

                if (data == null) {
                    System.out.println(" ");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        firstRun();
        startWebcam();
        try {
            manager = RmiClientManager.getInstance();
            manager.connect();

            parkingInterface = manager.getParkingInterface();
            clientCallback = manager.getClientCallBack();

            List<ParkingSlotDTO> slots = parkingInterface.getAllSlots();
            gridManager = new ParkingGridManager(parkingGrid);
            gridManager.updateGrid(slots);
            // gridManager.updateGrid(slots);

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
        brandField1.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        placeField1.textProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());
        InforField1.valueProperty().addListener((obs, oldVal, newVal) -> debounce.playFromStart());

        debounceOut.setOnFinished(e -> autoFillOutInfo());
        plateField0.textProperty().addListener((obs, oldVal, newVal) -> debounceOut.playFromStart());

        debounceIn.setOnFinished(e -> autoFillInInfo());
        plateField1.textProperty().addListener((obs, oldVal, newVal) -> debounceOut.playFromStart());

    }

    @FXML
    void acceptHandle(ActionEvent event) {
        LocalDateTime current;
        switch (checkInOut) {
            case 1:
                current = LocalDateTime.now();

                String plateName1 = plateField1.getText().trim();
                String owner1 = ownerField1.getText();
                String place = placeField1.getText();
                String brand = brandField1.getText();
                String vehicleInfor = InforField1.getSelectionModel().getSelectedItem();

                if (plateName1.isEmpty() || place.isEmpty()) {
                    Platform.runLater(() -> openNotificationScreen("Vui lòng nhập đủ"));
                    return;
                }

                new Thread(() -> {
                    try {
                        boolean exists = parkingInterface.checkIdIn(plateName1);
                        boolean exists2 = parkingInterface.checkPlaceIn(place);
                        if (exists2) {
                            Platform.runLater(() -> openNotificationScreen("Vị trí này đã được thuê rồi"));
                            return;
                        }
                        if (exists) {
                            Platform.runLater(() -> openNotificationScreen("Biển số đã trong bãi đỗ!"));
                            return;
                        }
                        try {
                            ParkingSlotDTO slot = new ParkingSlotDTO();
                            VehicleDTO vehicle = new VehicleDTO();
                            ParkingHistoryDTO history = new ParkingHistoryDTO();

                            slot.setSpotId(place);
                            slot.setStatus("OCCUPIED");

                            history.setEntryTime(current);

                            vehicle.setBrand(brand);
                            vehicle.setOwner(owner1);
                            vehicle.setPlateNumber(plateName1);
                            vehicle.setVehicleType(vehicleInfor);
                            slot.setVehicle(vehicle);
                            slot.setHistory(history);
                            int i = parkingInterface.updateSlotStatus(slot);
                            switch (i) {
                                case 0:
                                    Platform.runLater(() -> {
                                        inLabel.setText("Xe biển số "+ plateField1.getText() +" đã lấy vị trí "+ placeField1.getText());
                                        placeField1.setText(" "); arrivalTimeField1.setText(" "); plateField1.setText(" "); brandField1.setText(" "); ownerField1.setText(" ");
                                    });
                                    break;
                                case 1:
                                    Platform.runLater(() -> openNotificationScreen("Slot đang được thuê"));
                                    break;
                                case 2:
                                    Platform.runLater(() -> openNotificationScreen("Có người thuê slot này rồi"));
                                    break;
                                case 3:
                                    Platform.runLater(() -> openNotificationScreen("Xe đã có trong bãi"));
                                    break;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
            case 0:
                new Thread(() -> {
                    try {
                        boolean check = parkingInterface.takeVehicleOut(outData);

                        if (check) {
                            Platform.runLater(() -> {
                                inLabel.setText("Xe có biển số " + plateField0.getText() + "ở vị trí "
                                        + placeField0.getText() + " đã ra khỏi bãi");
                                transaction_id_Field0.setText(" ");
                                ownerField0.setText(" ");
                                brandField0.setText(" ");
                                placeField0.setText(" ");
                                InforField0.setText(" ");
                                arrivalTimeField0.setText(" ");
                                leaveTimeField0.setText(" ");
                                priceField0.setText(" ");
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
        }
    }

    @FXML
    private void handleRentButton(ActionEvent event) {
        try {
            stopWebcam();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/duanparking/rent-screen.fxml"));
            Parent root = loader.load();

            Scene newScene = new Scene(root);

            RentController newRentController = loader.getController();

            RmiClientManager.getInstance().setGridManager(newRentController.getParkingGridManager());

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            currentStage.setScene(newScene);
            currentStage.setTitle("Rent Parking");

        } catch (Exception e) {
            e.printStackTrace();
            openNotificationScreen("Lỗi load giao diện Rent: " + e.getMessage()); // Dùng method alert của mày
        }
    }

}
