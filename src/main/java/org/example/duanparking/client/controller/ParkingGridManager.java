package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.server.dao.ParkingSlotEntity;
import org.example.duanparking.model.SlotStatus;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.awt.Color.getColor;

public class ParkingGridManager {
    private GridPane parkingGrid;
    private List<ParkingSlotDTO> slots;

    public ParkingGridManager(GridPane parkingGrid) {
        this.parkingGrid = parkingGrid;
    }

    public void updateGrid(List<ParkingSlotDTO> slots) {
        this.slots = (slots != null) ? new ArrayList<>(slots) : new ArrayList<>();
        try {
            Platform.runLater(this::buildGrid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildGrid() {
        parkingGrid.getChildren().clear();
        parkingGrid.getRowConstraints().clear();
        parkingGrid.getColumnConstraints().clear();

        if (slots.isEmpty()) return;

        // ✅ Tính max hàng/cột
        int maxRows = slots.stream().mapToInt(ParkingSlotDTO::getRow).max().orElse(0) + 1;
        int maxCols = slots.stream().mapToInt(ParkingSlotDTO::getCol).max().orElse(0) + 1;

        // ✅ Cấu hình kích thước ô cố định
        for (int i = 0; i < maxCols; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPrefWidth(100);
            col.setMinWidth(100);
            col.setMaxWidth(100);
            parkingGrid.getColumnConstraints().add(col);
        }

        for (int i = 0; i < maxRows; i++) {
            RowConstraints row = new RowConstraints();
            row.setPrefHeight(100);
            row.setMinHeight(100);
            row.setMaxHeight(100);
            parkingGrid.getRowConstraints().add(row);
        }

        // ✅ Map vị trí -> slot
        Map<String, ParkingSlotDTO> slotMap = new HashMap<>();
        for (ParkingSlotDTO slot : slots) {
            slotMap.put(slot.getRow() + "," + slot.getCol(), slot);
        }

        // ✅ Tạo đủ tất cả các ô, kể cả trống
        for (int r = 0; r < maxRows; r++) {
            for (int c = 0; c < maxCols; c++) {
                ParkingSlotDTO slot = slotMap.get(r + "," + c);

                AnchorPane pane = new AnchorPane();
                pane.setPrefSize(100, 100);
                pane.setId(slot != null ? "slot" + slot.getSpotId() : "empty" + r + "_" + c);
                pane.setStyle("-fx-border-color: black; -fx-background-color: "
                        + (slot == null ? "#cccccc" : "green") + ";");

                if (slot != null) {
                    Label label = new Label("Slot " + slot.getSpotId());
                    label.setLayoutX(10);
                    label.setLayoutY(10);
                    pane.getChildren().add(label);
                }

                parkingGrid.add(pane, c, r);
            }
        }

        updateSlotUI();
    }

    public void updateSlotUI() {
        if (slots == null) return;
        for (ParkingSlotDTO slot : slots) {
            Node node = parkingGrid.lookup("#slot" + slot.getSpotId());
            if (node instanceof AnchorPane pane) {
                String color = getColor(slot.getStatus(), slot.getAreaType());
                pane.setStyle("-fx-background-color: " + color + "; -fx-border-color: black;");
            }
        }
    }

    // Chọn màu theo trạng thái
    private String getColor(String status, String areaType) {
         if (status.equalsIgnoreCase("OCCUPIED")) {
            return "#ff4444";
            }

        // Nếu free/reserved → màu theo loại slot
        String type = areaType;

        return switch (type) {
            case "PREMIUM" -> "#3388ff";  // xanh dương
            case "VIP" -> "#aa44ff";      // tím
            case "EV" -> "#44ff44";       // xanh neon
            case "MOTOR" -> "#ffaa00";    // cam
            default -> "#44aa44";         // STANDARD
        };
    }


    public void updateSingleSlot(ParkingSlotDTO slot) throws RemoteException {
        Platform.runLater(() -> {
            Node node = parkingGrid.lookup("#slot" + slot.getSpotId());
            if (node instanceof AnchorPane slotPane) {
                String color;

                if ("STANDARD".equalsIgnoreCase(slot.getAreaType())) {
                    // màu cho STANDARD
                    color = switch (slot.getStatus().toUpperCase()) {
                        case "OCCUPIED" -> "#ff4444"; // đỏ
                        case "RESERVED" -> "#ffaa00"; // vàng cam
                        default    -> "#44aa44"; // xanh lá
                    };
                } else if ("PREMIUM".equalsIgnoreCase(slot.getAreaType())) {

                    color = switch (slot.getStatus().toUpperCase()) {
                        case "OCCUPIED" -> "#aa0000";
                        case "RESERVED" -> "#ff8800";
                        default   -> "#4488ff";
                    };
                } else {
                    color = "#cccccc"; // mặc định
                }

                slotPane.setStyle("-fx-background-color: " + color + "; -fx-border-color: black;");
            }
        });
    }


    public String getStringSlot(){
        return null;
    }


}
