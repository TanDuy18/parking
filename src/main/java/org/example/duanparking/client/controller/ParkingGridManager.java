package org.example.duanparking.client.controller;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import org.example.duanparking.model.ParkingSlot;
import org.example.duanparking.model.SlotStatus;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ParkingGridManager {
    private GridPane parkingGrid;
    private ArrayList<ParkingSlot> slots;

    public ParkingGridManager(GridPane parkingGrid, ArrayList<ParkingSlot> slots) {
        this.parkingGrid = parkingGrid;
        this.slots = slots;
    }

    public void parkingSlotGrid() throws RemoteException {
        parkingGrid.getChildren().clear(); // Xóa các slot cũ
        parkingGrid.getRowConstraints().clear();
        parkingGrid.getColumnConstraints().clear();

        // Tính số hàng và cột tối đa từ slots
        int maxRows = 0, maxCols = 0;
        for (ParkingSlot slot : slots) {
            maxRows = Math.max(maxRows, slot.getRowIndex() + 1);
            maxCols = Math.max(maxCols, slot.getColumnIndex() + 1);
        }

        // Thiết lập kích thước cột/hàng
        for (int i = 0; i < maxCols; i++) {
            parkingGrid.getColumnConstraints().add(new ColumnConstraints(100));
        }
        for (int i = 0; i < maxRows; i++) {
            parkingGrid.getRowConstraints().add(new RowConstraints(100));
        }


        // Thêm slots vào GridPane
        for (ParkingSlot slot : slots) {
            AnchorPane slotPane = new AnchorPane();
            slotPane.setId("slot" + slot.getSpotId());
            slotPane.setPrefSize(100, 100);
            slotPane.setStyle("-fx-background-color: green; -fx-border-color: black; -fx-background-insets: 0;");

            // Thêm Label hiển thị ID slot
            Label label = new Label("Slot " + slot.getSpotId());
            label.setLayoutX(10);
            label.setLayoutY(10);
            slotPane.getChildren().add(label);


            // Thêm vào GridPane theo rowIndex, colIndex
            parkingGrid.add(slotPane, slot.getColumnIndex(), slot.getRowIndex());
        }
        updateSlotUI();
    }

    public void updateSlotUI() throws RemoteException {
        for (ParkingSlot slot : slots) {
            boolean isBooked = false;
            switch (SlotStatus.valueOf(slot.getStatus().toUpperCase())) {
                case OCCUPIED -> isBooked = true;
            }
            Node parkingSlot = parkingGrid.lookup("#slot" + slot.getSpotId());
            if (parkingSlot != null) {
                AnchorPane button = (AnchorPane) parkingSlot;
                button.setStyle(isBooked ? "-fx-background-color: red; -fx-border-color: black; -fx-background-insets: 0; -fx-padding: 0;"
                        : "-fx-background-color: green; -fx-border-color: black; -fx-background-insets: 0; -fx-padding: 0;");
            }
        }
    }
    public void updateSingleSlot(String slotId, String status) {
        Node node = parkingGrid.lookup("#slot" + slotId);
        if (node instanceof AnchorPane slotPane) {
            String color = switch (status.toUpperCase()) {
                case "OCCUPIED" -> "red";
                case "RESERVED" -> "yellow";
                case "FREE" -> "green";
                default -> "lightgray";
            };
            slotPane.setStyle("-fx-background-color: " + color + "; -fx-border-color: black;");
        }
    }

    public String getStringSlot(){
        return null;
    }
}
