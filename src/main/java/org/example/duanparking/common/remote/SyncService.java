package org.example.duanparking.common.remote;

import org.example.duanparking.common.dto.ParkingSlotDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SyncService extends Remote {
    void syncVehicleIn(ParkingSlotDTO slot) throws RemoteException;
    void syncVehicleOut(ParkingSlotDTO slot) throws RemoteException; 
}
