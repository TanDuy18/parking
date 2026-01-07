package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import org.example.duanparking.common.SlotClickHandler;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.dto.SlotStatusDTO;
import org.example.duanparking.common.SlotViewModel;
import org.example.duanparking.common.dto.rent.ScheduleDTO;
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

    private BooleanProperty isHighlighted = new SimpleBooleanProperty(false);

    public StringBinding borderBinding() {
        return Bindings.createStringBinding(() -> {
            if (isHighlighted.get()) {
                return "-fx-border-color: #FFD700; -fx-border-width: 4; -fx-border-radius: 5;";
            } else {
                return "-fx-border-color: #cccccc; -fx-border-width: 1;";
            }
        }, isHighlighted);
    }

    public void setHighlighted(boolean value) {
        this.isHighlighted.set(value);
    }


    public Map<String, SlotViewModel> getViewModelMap() {
        return viewModelMap;
    }
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

    // Trong class ParkingGridManager
    public void highlightSlotById(String spotId) {
        Platform.runLater(() -> {
            SlotViewModel vm = viewModelMap.get(spotId);
            if (vm != null) {
                handleHighlight(vm); // Sử dụng hàm logic highlight bạn đã viết
                System.out.println("Đã highlight ô: " + spotId);
            } else {
                // Nếu không tìm thấy hoặc biển số mới, bỏ highlight cái cũ
                if (selectedViewModel != null) {
                    selectedViewModel.setHighlighted(false);
                    selectedViewModel = null;
                }
            }
        });
    }

    public void refreshAllSlotsTime() {
        viewModelMap.values().forEach(SlotViewModel::updateTime);
    }

    public void buildTimeline(List<ParkingSlotDTO> slots, String selectedZone) {
        parkingGrid.getChildren().clear();
        parkingGrid.getRowConstraints().clear();
        parkingGrid.getColumnConstraints().clear();

        if (slots == null || slots.isEmpty()) {
            parkingGrid.add(new Label("Không có dữ liệu cho khu vực " + selectedZone), 0, 0);
            return;
        }

        double hourWidth = 60.0;
        double idColumnWidth = 100.0; // Độ rộng cột ID

        // THÊM: Thiết lập ColumnConstraints cho cột ID (Cột 0)
        ColumnConstraints idCol = new ColumnConstraints(idColumnWidth);
        parkingGrid.getColumnConstraints().add(idCol);

        // THÊM: Thiết lập ColumnConstraints cho 24 cột giờ (Cột 1-24)
        for (int h = 0; h < 24; h++) {
            ColumnConstraints hourCol = new ColumnConstraints(hourWidth);
            parkingGrid.getColumnConstraints().add(hourCol);

            Label hourLabel = new Label(String.format("%02d:00", h));
            hourLabel.setMinWidth(hourWidth);
            hourLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5; -fx-alignment: center;");
            parkingGrid.add(hourLabel, h + 1, 0);
        }

        int rowIndex = 1; // Bắt đầu từ dòng 1 (dòng 0 là tiêu đề giờ)
        for (ParkingSlotDTO slot : slots) {
            // Cột 0: Tên ô đỗ xe
            Label slotLabel = new Label(slot.getSpotId());
            slotLabel.setMinWidth(idColumnWidth);
            slotLabel.setStyle("-fx-padding: 10; -fx-font-weight: bold; -fx-border-color: #eee;");
            parkingGrid.add(slotLabel, 0, rowIndex);

            // Vùng chứa các thanh thời gian
            AnchorPane timelineContainer = new AnchorPane();
            timelineContainer.setPrefWidth(24 * hourWidth);
            timelineContainer.setPrefHeight(40);
            timelineContainer.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-width: 0.5;");

            // KIỂM TRA DỮ LIỆU: Debug xem có schedule không
            if (slot.getSchedules() != null) {
                for (ScheduleDTO schedule : slot.getSchedules()) {
                    Region bar = createScheduleBar(schedule, hourWidth);
                    timelineContainer.getChildren().add(bar);
                }
            }

            // Quan trọng: timelineContainer phải được add vào cột 1 và kéo dài (colspan) 24 cột
            parkingGrid.add(timelineContainer, 1, rowIndex, 24, 1);
            rowIndex++;
        }
    }
    private Region createScheduleBar(ScheduleDTO schedule, double hourWidth) {
        Region bar = new Region();

        double startInMinutes = schedule.getStartTime().getHour() * 60 + schedule.getStartTime().getMinute();
        double endInMinutes;

        if (schedule.isOvernight()) {
            endInMinutes = 24 * 60;
        } else {
            endInMinutes = schedule.getEndTime().getHour() * 60 + schedule.getEndTime().getMinute();
        }

        double xOffset = (startInMinutes / 60.0) * hourWidth;
        double width = ((endInMinutes - startInMinutes) / 60.0) * hourWidth;

        if (width <= 0) width = 5; // Đảm bảo luôn thấy một vạch nhỏ nếu thời gian quá ngắn

        bar.setPrefWidth(width);
        bar.setMinWidth(Region.USE_PREF_SIZE); // Ép Region giữ đúng kích thước
        bar.setPrefHeight(30);
        bar.setLayoutX(xOffset);
        bar.setLayoutY(5);

        // Style màu cam nổi bật
        bar.setStyle("-fx-background-color: #FFA500; -fx-background-radius: 4; -fx-border-color: #CC7A00; -fx-border-width: 1;");

        Tooltip tooltip = new Tooltip("Khách: " + schedule.getRenterName() + "\n" +
                "Giờ: " + schedule.getStartTime() + " - " + schedule.getEndTime());
        Tooltip.install(bar, tooltip);

        return bar;
    }
}
