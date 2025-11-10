package org.example.duanparking.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.example.duanparking.client.controller.ParkingGridManager;
import org.example.duanparking.common.dto.ParkingSlotDTO;

public interface ClientCallback extends Remote {
    /*
    * Server gọi client
    */
    void onSlotUpdated(ParkingSlotDTO slots) throws RemoteException;
    void setGridManager(ParkingGridManager gridManager) throws RemoteException;
    void ping() throws RemoteException;
    void syncSlots(List<ParkingSlotDTO> slots) throws RemoteException;
}
