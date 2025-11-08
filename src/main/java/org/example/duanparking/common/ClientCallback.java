package org.example.duanparking.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.example.duanparking.client.controller.ParkingGridManager;
import org.example.duanparking.model.ParkingSlot;

public interface ClientCallback extends Remote {
    /*
    * Server gọi client
    */

    ArrayList<ParkingSlot> getSlots() throws RemoteException;
    void onSlotUpdated(String slotId, String status) throws RemoteException;
    void setGridManager(ParkingGridManager gridManager) throws RemoteException;
    void ping() throws RemoteException;
}
