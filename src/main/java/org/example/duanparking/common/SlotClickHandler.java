package org.example.duanparking.common;

import javafx.scene.layout.AnchorPane;
import org.example.duanparking.common.dto.ParkingSlotDTO;

@FunctionalInterface
public interface SlotClickHandler {
    void onSlotClicked(ParkingSlotDTO slot, AnchorPane pane);
}
