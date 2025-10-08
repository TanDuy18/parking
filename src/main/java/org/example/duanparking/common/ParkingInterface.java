package org.example.duanparking.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ParkingInterface extends Remote {
    ParkingResponse enterVehicle(VehicleData data) throws RemoteException;
    void notifyEnter(VehicleData data) throws RemoteException;
}
