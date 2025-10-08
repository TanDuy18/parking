package org.example.duanparking.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.example.duanparking.common.ParkingInterface;
import org.example.duanparking.common.ParkingResponse;
import org.example.duanparking.common.VehicleData;
import org.example.duanparking.model.DBManager;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DasboardController implements Initializable {
    @FXML private ImageView cameraView;
    @FXML private TextField arrivalTimeField;
    @FXML private TextField ownerField;
    @FXML private TextField plateField;
    @FXML private TextField ticketField;
    @FXML private TextField priceField;

    @FXML private Button acceptBtn;
    @FXML private Button exitBtn;
    @FXML private Button enterBtn;

    private ParkingInterface parkingInterface;

    private VideoCapture capture;
    private boolean cameraActive = true;

    private Image matToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".bmp", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

//    private Image matToImage(Mat frame) { // Cần được giải thích kĩ hơn
//        int width = frame.width(), height = frame.height(), channels = frame.channels();
//        byte[] source = new byte[width * height * channels];
//        frame.get(0, 0, source);
//        WritableImage image = new WritableImage(width, height);
//        PixelWriter pw = image.getPixelWriter();
//        pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteRgbInstance(), source, 0, width * channels);
//        return image;
//    }



    private void callRMIEnter(String plate, String owner) {
        if (parkingInterface == null) return;
        VehicleData data = new VehicleData(plate, System.currentTimeMillis(), owner);
        try {
            ParkingResponse response = parkingInterface.enterVehicle(data);
            Platform.runLater(() -> {
                plateField.setText(plate);
                arrivalTimeField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            });
            parkingInterface.notifyEnter(data);  // Sync notify
        } catch (Exception e) {
           e.printStackTrace();
        }
    }

    public void onEnterVehicle() {
        String plate = plateField.getText().trim();
        String owner = ownerField.getText().trim();

        String arrival = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (DBManager.saveEntry(plate, arrival, owner)) {  // Lưu local DB
            System.out.println("Lưu local DB OK: " + plate);
        }
        callRMIEnter(plate, owner);  // Gọi RMI sync
    }




    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nu.pattern.OpenCV.loadLocally();
        capture = new VideoCapture(0);
        initRMI();

        Thread captureThread = new Thread(() -> {
            Mat frame = new Mat(); // Tạo ma trận để lưu frame chứa dữ liệu cũ
            while (cameraActive && capture.isOpened()) {
                if(capture.grab()) {  // Dùng grab và retrieve
                    capture.retrieve(frame);
                    if(!frame.empty()) {
                        Mat resized = new Mat();
                        Imgproc.resize(frame,resized, new Size(826, 700));
                        Image fxImage = matToImage(resized);
                        Platform.runLater(() -> cameraView.setImage(fxImage));
                        resized.release(); // Giải phóng memory
                    }
                }
                try{
                    Thread.sleep(30);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        captureThread.setDaemon(true); //
        captureThread.start();


        exitBtn.setOnAction( event -> {});
        enterBtn.setOnAction( event -> {});
        acceptBtn.setOnAction( event -> {onEnterVehicle();});
    }
    private void initRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);  // Port server
            parkingInterface = (ParkingInterface) registry.lookup("ParkingService");
            System.out.println("Kết nối RMI OK!");
        } catch (Exception e) {
            System.err.println("Lỗi RMI: " + e.getMessage());
        }
    }
}
