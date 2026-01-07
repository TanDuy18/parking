package org.example.duanparking.client;

import javafx.application.Platform;
import org.example.duanparking.client.controller.ParkingGridManager;
import org.example.duanparking.common.SlotViewModel;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.dto.SlotStatusDTO;
import org.example.duanparking.common.dto.rent.DayRent;
import org.example.duanparking.common.dto.rent.ScheduleDTO;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {

    private ParkingGridManager parkingGrid;  
    public ClientCallbackImpl() throws RemoteException {
        super();
    }


    @Override
    public void syncSlots(List<ParkingSlotDTO> slots) throws RemoteException {
        Platform.runLater(() -> {
            if (parkingGrid != null) {
                parkingGrid.updateGrid(slots);
            }
        });
    }


    @Override
    public void onSlotUpdated(SlotStatusDTO slot) throws RemoteException {
        Platform.runLater(() -> {
            try {
                parkingGrid.updateSingleSlot(slot);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void setGridManager(ParkingGridManager gridManager) throws RemoteException {
        this.parkingGrid = gridManager;
    }

    @Override 
    public void ping() throws RemoteException {};

    @Override
    public void onRentAdded(String place, List<DayRent> days) throws RemoteException {
        Platform.runLater(() -> {
            SlotViewModel vm = parkingGrid.getViewModelMap().get(place);
            if (vm != null) {
                List<ScheduleDTO> newSchedules = new ArrayList<>();
                for (DayRent dr : days) {
                    ScheduleDTO s = new ScheduleDTO();
                    s.setDayOfWeek(dr.getDayOfWeek());
                    s.setStartTime(dr.getStart());
                    s.setEndTime(dr.getEnd());
                    newSchedules.add(s);
                }

                vm.getDailySchedules().setAll(newSchedules);
                vm.updateTime();
            }
        });
    }
}
