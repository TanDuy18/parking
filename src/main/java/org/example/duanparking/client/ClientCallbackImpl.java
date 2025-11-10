package org.example.duanparking.client;

import javafx.application.Platform;
import org.example.duanparking.client.controller.ParkingGridManager;
import org.example.duanparking.common.dto.ParkingSlotDTO;
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
    public void onSlotUpdated(ParkingSlotDTO slot) throws RemoteException {
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
}
