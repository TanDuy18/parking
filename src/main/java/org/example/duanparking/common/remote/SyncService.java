package org.example.duanparking.common.remote;

import org.example.duanparking.common.dto.ParkingInEvent;
import org.example.duanparking.common.dto.ParkingSlotDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SyncService extends Remote {
    void syncVehicleOut(ParkingSlotDTO slot) throws RemoteException;

    void syncVehicleIn(ParkingInEvent slot) throws RemoteException;
}
