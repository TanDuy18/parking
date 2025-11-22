package org.example.duanparking.server;

import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.remote.SyncService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private final ParkingImpl parkingImpl;

    public SyncServiceImpl(ParkingImpl parkingImpl) throws RemoteException {
        this.parkingImpl = parkingImpl;
    }

    @Override
    public void syncVehicleIn(ParkingSlotDTO slot) throws RemoteException {
        System.out.println("Nhận đồng bộ từ server khác: " + slot.getSpotId() + " = " + slot.getStatus());
    }

    @Override
    public void syncVehicleOut(ParkingSlotDTO slot) throws RemoteException {
    }
}
