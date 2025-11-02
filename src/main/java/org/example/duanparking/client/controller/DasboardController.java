package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import nu.pattern.OpenCV;
import org.example.duanparking.client.ClientCallbackImpl;
import org.example.duanparking.common.ParkingInterface;
import org.example.duanparking.model.ParkingSlot;
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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @FXML private TextField arrivalTimeField0;
    @FXML private TextField leaveTimeField0;
    @FXML private TextField placeField0;
    @FXML private TextField plateField0;
    @FXML private TextField priceField0;
    @FXML private TextField ticketField0;
    @FXML private TextField ownerField0;

    private ClientCallbackImpl clientCallback;
    private ParkingInterface parkingInterface;
    private VideoCapture capture;
    private boolean cameraActive = true; 
    ArrayList<ParkingSlot> slots = new ArrayList<>();

    int checkInOut = 0;
 
    ParkingGridManager gridManager;   

    private void initRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            parkingInterface = (ParkingInterface) registry.lookup("ParkingService");
            System.out.println("Kết nối RMI OK!");
        } catch (Exception e) {
            System.err.println("Lỗi RMI: " + e.getMessage());
        }
    }

    private Image matToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".bmp", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private void openNotificationScreen() {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText("Look, an Information Dialog");
            alert.setContentText("I have a great message for you!");

            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        OpenCV.loadLocally();
        capture = new VideoCapture(0);
        initRMI();
        try {
            slots = parkingInterface.getSlot();

            gridManager = new ParkingGridManager(parkingGrid, slots);
            gridManager.parkingSlotGrid();

            clientCallback = new ClientCallbackImpl(parkingInterface,gridManager) ;

            parkingInterface.registerClient(clientCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        Thread captureThread = new Thread(() -> {
            Mat frame = new Mat(); // Tạo ma trận để lưu frame chứa dữ liệu cũ
            while (cameraActive && capture.isOpened()) {
                if(capture.grab()) {  // Dùng grab và retrieve
                    capture.retrieve(frame);
                    Core.flip(frame, frame, 1);
                    if(!frame.empty()) {
                        Mat resized = new Mat();
                        Imgproc.resize(frame, resized, new Size(590, 339), 0, 0, Imgproc.INTER_AREA);
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


        inBtn.setOnAction( event -> {inBtn.setDisable(true);outBtn.setDisable(false);inPane.setVisible(true);outPane.setVisible(false);checkInOut = 1;});
        /*
        *    1 là in và 0 là out
        */
        outBtn.setOnAction( event -> {outBtn.setDisable(true);inBtn.setDisable(false);inPane.setVisible(false);outPane.setVisible(true);checkInOut = 0;});
        acceptBtn.setOnAction( event -> {
            LocalDateTime current;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
           switch (checkInOut) {
               case 1:
                   try {
                       current = LocalDateTime.now();
                       String formatted = current.format(formatter);

                       arrivalTimeField1.setText(current.format(dateTimeFormatter));

                       String plateName1 = plateField1.getText();
                       String owner1 =  ownerField1.getText();
                       String rentPlace1 = placeField1.getText();
                       String ticket = ticketField1.getText(); 
                       System.out.println(plateName1+" "+formatted+" "+rentPlace1);

                      // parkingInterface.updateSlotStatus(rentPlace1, "OCCUPIED", plateName1, owner1,formatted);
                        
                       if(plateName1 == null || plateName1.isEmpty() || rentPlace1 == null || rentPlace1.isEmpty() || ticket == null || ticket.isEmpty()){
                            openNotificationScreen();
                        } else {
                           parkingInterface.updateSlotStatus(rentPlace1, "OCCUPIED", plateName1, owner1,formatted);
                        }  

                   } catch (RemoteException e) {
                       throw new RuntimeException(e);
                   }
                   break;
               case 0:
                   current = LocalDateTime.now();

                   String formatted0 = current.format(formatter);
                   leaveTimeField0.setText(current.format(dateTimeFormatter));

                   String plateName0 = plateField0.getText();
                   String owner0 =  ownerField0.getText();
                   String rentPlace0 = placeField0.getText();
                   System.out.println(plateName0+" "+formatted0+" "+rentPlace0);
           }
        });
    }
}
