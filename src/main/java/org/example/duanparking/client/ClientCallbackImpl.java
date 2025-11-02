package org.example.duanparking.client;

import javafx.application.Platform;
import org.example.duanparking.client.controller.ParkingGridManager;
import org.example.duanparking.common.ClientCallback;
import org.example.duanparking.common.ParkingInterface;
import org.example.duanparking.model.ParkingSlot;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    ArrayList<ParkingSlot> slots = new ArrayList<>();
    private ParkingInterface parkingInterface;
    private ParkingGridManager parkingGrid;  
    public ClientCallbackImpl(ParkingInterface parkingInterface, ParkingGridManager parkingGridManager) throws RemoteException {
        this.parkingInterface = parkingInterface;
        this.parkingGrid = parkingGridManager;
        initializeSlot();
    }

    public void initializeSlot() throws RemoteException {
        slots = parkingInterface.getSlot();
    }

    @Override
    public ArrayList<ParkingSlot> getSlots() throws RemoteException {
        return slots;
    }

    @Override
    public void onSlotUpdated(String slotId, String status) throws RemoteException {
        for (ParkingSlot slot : slots) {
            if(slot.getSpotId().equals(slotId)) {
                slot.setStatus(status);
                break;
            }
        }

        Platform.runLater(() -> {
           parkingGrid.updateSingleSlot(slotId, status);
        });
    }
}
