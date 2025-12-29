package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import org.example.duanparking.common.SlotClickHandler;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.dto.SlotStatusDTO;
import org.example.duanparking.common.SlotViewModel;
import org.example.duanparking.model.DisplayMode;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingGridManager {
    private GridPane parkingGrid;
    private Map<String, SlotViewModel> viewModelMap = new HashMap<>();
    private boolean isRentPage = false;
    private SlotClickHandler primaryClickHandler = (slot, pane) -> {};
    private SlotClickHandler secondaryClickHandler = (slot, pane) -> {};
    private DisplayMode currentMode;
    private SlotViewModel selectedViewModel = null;


    public ParkingGridManager(GridPane parkingGrid) {
        this.parkingGrid = parkingGrid;
    }

    public void updateGrid(List<ParkingSlotDTO> slots) {
        try {
            Platform.runLater(() -> buildGrid(slots));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setCurrentMode(DisplayMode mode) {
        this.currentMode = mode;
    }

    public void setClickHandler(SlotClickHandler handler) {
        this.primaryClickHandler = (handler != null) ? handler : (slot, pane) -> {
        };
    }

    public void setRightClickHandler(SlotClickHandler handler) {
        this.secondaryClickHandler = (handler != null) ? handler : (slot, pane) -> {
        };
    }

    private void buildGrid(List<ParkingSlotDTO> slots) {
        parkingGrid.getChildren().clear();
        parkingGrid.getRowConstraints().clear();
        parkingGrid.getColumnConstraints().clear();
        viewModelMap.clear();

        if (slots.isEmpty()) return;

        int maxRows = slots.stream().mapToInt(ParkingSlotDTO::getRow).max().orElse(0) + 1;
        int maxCols = slots.stream().mapToInt(ParkingSlotDTO::getCol).max().orElse(0) + 1;

        int lineCol = 5;
        int lineRow = 2;
        for (int c = 0; c < maxCols; c++) {
            ColumnConstraints col = new ColumnConstraints();

            double width = 100;
            if (c == lineCol) {
                width = 40;
                lineCol += 6;
            }
            col.setPrefWidth(width);
            col.setMinWidth(width);
            col.setMaxWidth(width);

            parkingGrid.getColumnConstraints().add(col);
        }

        for (int r = 0; r < maxRows; r++) {
            RowConstraints row = new RowConstraints();
            double width = 100;

            if (r == lineRow) {
                width = 40;
                lineRow += 3;
            }
            row.setPrefHeight(width);
            row.setMinHeight(width);
            row.setMaxHeight(width);
            parkingGrid.getRowConstraints().add(row);
        }

        for (ParkingSlotDTO dto : slots) {
            SlotViewModel vm = new SlotViewModel(dto);
            viewModelMap.put(dto.getSpotId(), vm);

            AnchorPane pane = new AnchorPane();
            pane.setPrefSize(100, 100);

            pane.styleProperty().bind(Bindings.concat(
                    "-fx-background-color: ", vm.colorBinding(this.currentMode), "; ",
                    vm.borderBinding()
            ));

            Label label = new Label();
            label.textProperty().bind(vm.textBinding());
            label.setLayoutX(5);
            label.setLayoutY(5);
            pane.getChildren().add(label);

            pane.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {

                    Platform.runLater(() -> secondaryClickHandler.onSlotClicked(dto, pane));

                } else if (event.getButton() == MouseButton.PRIMARY) {
                    handleHighlight(vm);
                    Platform.runLater(() -> primaryClickHandler.onSlotClicked(dto, pane));
                    System.out.println("Bạn đang click chuột trái");
                }
                event.consume();
            });

            parkingGrid.add(pane, dto.getCol(), dto.getRow());
        }


    }


    public void updateSingleSlot(SlotStatusDTO slot) throws RemoteException {
        Platform.runLater(() -> {
            SlotViewModel vm = viewModelMap.get(slot.getSpotId());
            if (vm != null) {
                vm.setStatus(slot.getStatus());
            }
        });
    }

    private void handleHighlight(SlotViewModel newVm) {
        if (selectedViewModel != null) {
            selectedViewModel.setHighlighted(false);
        }
        newVm.setHighlighted(true);
        selectedViewModel = newVm;
    }

    public void refreshAllSlotsTime() {
        viewModelMap.values().forEach(SlotViewModel::updateTime);
    }
}
