package org.example.duanparking.common.remote;

import org.example.duanparking.common.dto.ParkingInEvent;
import org.example.duanparking.common.dto.ParkingOutEvent;
import org.example.duanparking.common.dto.rent.RentEvent;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SyncService extends Remote {
    void syncVehicleOut(ParkingOutEvent slot) throws RemoteException;

    void syncVehicleIn(ParkingInEvent slot) throws RemoteException;
    void syncRentPlace(RentEvent rent) throws RemoteException;
}
