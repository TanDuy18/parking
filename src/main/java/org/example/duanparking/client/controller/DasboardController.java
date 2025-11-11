package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    @FXML private TextField ticketField1;
    @FXML private TextField ownerField1;
    @FXML private TextField InforField1;

    @FXML private TextField InforField0;
    @FXML private TextField arrivalTimeField0;
    @FXML private TextField leaveTimeField0;
    @FXML private TextField placeField0;
    @FXML private TextField plateField0;
    @FXML private TextField priceField0;
    @FXML private TextField ticketField0;
    @FXML private TextField ownerField0;

    private ParkingInterface parkingInterface;
    private ClientCallback clientCallback;
    private static RmiClientManager manager;
    private VideoCapture capture;
    private boolean cameraActive = true;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
            Mat frame = new Mat(); // Tạo ma trận để lưu frame chứa dữ liệu cũ
            while (cameraActive && capture.isOpened()) {
                if (capture.grab()) { // Dùng grab và retrieve
                    capture.retrieve(frame);
                    Core.flip(frame, frame, 1);
                    if (!frame.empty()) {
                        Mat resized = new Mat();
                        Imgproc.resize(frame, resized, new Size(590, 339), 0, 0, Imgproc.INTER_AREA);
                        Image fxImage = matToImage(resized);
                        Platform.runLater(() -> cameraView.setImage(fxImage));
                        resized.release(); // Giải phóng memory
                    }
                }
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        captureThread.setDaemon(true); //
        captureThread.start();

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
        acceptBtn.setOnAction(event -> {
            LocalDateTime current;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            switch (checkInOut) {
                case 1:
                    current = LocalDateTime.now();
                    String formatted = current.format(formatter);

                    arrivalTimeField1.setText(current.format(dateTimeFormatter));

                    String plateName1 = plateField1.getText();
                    String owner1 = ownerField1.getText();
                    String rentPlace1 = placeField1.getText();
                    String ticket = ticketField1.getText();
                    System.out.println(plateName1 + " " + formatted + " " + rentPlace1);

                    // parkingInterface.updateSlotStatus(rentPlace1, "OCCUPIED", plateName1,
                    // owner1,formatted);

                    if (plateName1 == null || plateName1.isEmpty() || rentPlace1 == null || rentPlace1.isEmpty()) {
                        Platform.runLater(() -> openNotificationScreen("Vui lòng nhập đủ"));
                        return;
                    }

                    new Thread(() -> {
                        try {
                            boolean exists = parkingInterface.checkId(plateName1);
                            boolean exists2 = parkingInterface.checkPlace(rentPlace1);
                            if(exists2) {
                                Platform.runLater(() -> openNotificationScreen("Vị trí này đã được thuê rồi"));
                                return; 
                            }
                            if (exists) {
                                Platform.runLater(() -> openNotificationScreen("Biển số đã trong bãi đỗ!"));
                            } else {
                                    try {
                                        parkingInterface.updateSlotStatus(rentPlace1, "OCCUPIED", plateName1, owner1, formatted);
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
                    current = LocalDateTime.now();

                    String formatted0 = current.format(formatter);
                    leaveTimeField0.setText(current.format(dateTimeFormatter));

                    String plateName0 = plateField0.getText();
                    String owner0 = ownerField0.getText();
                    String rentPlace0 = placeField0.getText();
                    System.out.println(plateName0 + " " + formatted0 + " " + rentPlace0);
            }
        });
    }
}
