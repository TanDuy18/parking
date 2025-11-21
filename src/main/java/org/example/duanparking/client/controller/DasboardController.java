package org.example.duanparking.client.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import nu.pattern.OpenCV;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.server.dao.ParkingSlotEntity;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


public class DasboardController implements Initializable {
    @FXML private ImageView cameraView;
    @FXML private Button acceptBtn;
    @FXML private Button outBtn;
    @FXML private Button inBtn;
    @FXML private GridPane parkingGrid;

    @FXML private AnchorPane outPane;
    @FXML private AnchorPane inPane;

    @FXML private TextField plateField1;
    @FXML private TextField placeField1;
    @FXML private TextField arrivalTimeField1;
    @FXML private TextField brandField1;
    @FXML private TextField ownerField1;
    @FXML private ComboBox<String> InforField1;

    @FXML private TextField transaction_id_Field0; 
    @FXML private TextField InforField0;
    @FXML private TextField arrivalTimeField0;
    @FXML private TextField leaveTimeField0;
    @FXML private TextField placeField0;
    @FXML private TextField plateField0;
    @FXML private TextField priceField0;
    @FXML private TextField brandField0;
    @FXML private TextField ownerField0;

    private ParkingInterface parkingInterface;
    private ClientCallback clientCallback;
    private static RmiClientManager manager;
    private VideoCapture capture;
    private boolean cameraActive = true;
    private DetectPlate detectPlate;
    private int frameCount = 0;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private PauseTransition debounce = new PauseTransition(Duration.millis(500));
    private PauseTransition debounceOut = new PauseTransition(Duration.millis(500));

    int checkInOut = 0;

    ParkingGridManager gridManager;


    private Image matToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".bmp", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
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
        boolean filled = !plateField1.getText().trim().isEmpty() && !brandField1.getText().trim().isEmpty() && !placeField1.getText().trim().isEmpty() && InforField1.getSelectionModel().getSelectedItem() != null;

        if (!filled) {arrivalTimeField1.clear();return;}

        if (!arrivalTimeField1.getText().trim().isEmpty()) return;

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
        if (plate.isEmpty()) return;

        new Thread(() -> {
            try {
                ParkingSlotDTO data = parkingInterface.getVehicleInfoForOut(plate);

                if (data == null) {
                    Platform.runLater(() -> openNotificationScreen("Không tìm thấy xe trong bãi!"));
                    return;
                }

                Platform.runLater(() -> {
                    transaction_id_Field0.setText(String.valueOf(data.getTransaction_id()));
                    ownerField0.setText(data.getOwner());
                    brandField0.setText(data.getBrand());
                    placeField0.setText(data.getSpotId());
                    InforField0.setText(data.getVehicleType());
                    arrivalTimeField0.setText(data.getEntryTime());
                    System.out.println(data.getEntryTime());
                    leaveTimeField0.setText(data.getExitTime());
                    priceField0.setText(formatVND(data.getFee()));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        firstRun();
        OpenCV.loadLocally();
        capture = new VideoCapture(0);
        try {
            manager = new RmiClientManager();
            manager.connect();

            parkingInterface = manager.getParkingInterface();
            clientCallback = manager.getClientCallBack();

            List<ParkingSlotDTO> slots = parkingInterface.getAllSlots();
            for (ParkingSlotDTO slot : slots) {
                System.out.println(slot.getCol() +" "+ slot.getRow());
            }
            System.out.println(slots);
            gridManager = new ParkingGridManager(parkingGrid);
            gridManager.updateGrid(slots);
            //gridManager.updateGrid(slots);

            manager.setGridManager(gridManager);

            parkingInterface.registerClient(clientCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        Thread captureThread = new Thread(() -> {
            Mat frame = new Mat();
            while (cameraActive && capture.isOpened()) {
                if (capture.grab()) {
                    capture.retrieve(frame);
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);

                    if (!frame.empty()) {
                        Mat resized = new Mat();

                        Imgproc.resize(frame, resized, new Size(640, 480), 0, 0, Imgproc.INTER_AREA);
                        Imgproc.cvtColor(resized, resized, Imgproc.COLOR_BGR2RGB);

                        // Imgproc.flip(resized, resized, 1);

                        frameCount++;
                        if (frameCount % 10 == 0) {

                        }
                        Image fxImage = matToImage(resized);
                        Platform.runLater(() -> cameraView.setImage(fxImage));
                        resized.release();
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.start();

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
    }
    @FXML
    void acceptHandle(ActionEvent event) {
        LocalDateTime current;
        switch (checkInOut) {
            case 1:
                current = LocalDateTime.now();
                String formatted = current.format(formatter);

                String plateName1 = plateField1.getText().trim();
                String owner1 = ownerField1.getText();
                String place = placeField1.getText();
                String brand = brandField1.getText();
                String infor = InforField1.getSelectionModel().getSelectedItem();


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
                        } else {
                            try {
                                parkingInterface.updateSlotStatus(place, "OCCUPIED", plateName1, owner1, formatted, brand, infor);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
            case 0:
                new Thread(() -> {
                    try {
                        ParkingSlotDTO out = new ParkingSlotDTO(
                                Integer.parseInt(transaction_id_Field0.getText()),
                                ownerField0.getText(), brandField0.getText(), plateField0.getText(),
                                placeField0.getText(), InforField0.getText(), arrivalTimeField0.getText(), 
                                leaveTimeField0.getText(), unformatVND(priceField0.getText())
                        );
                        System.out.println(arrivalTimeField0.getText() + " " + leaveTimeField0.getText() + " " + unformatVND(priceField0.getText()));
                        parkingInterface.takeVehicleOut(out);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
        }
    }
}
