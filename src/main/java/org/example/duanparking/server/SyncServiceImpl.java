package org.example.duanparking.server;

import org.example.duanparking.common.dto.ParkingInEvent;
import org.example.duanparking.common.dto.ParkingOutEvent;
import org.example.duanparking.common.dto.rent.RentEvent;
import org.example.duanparking.common.remote.SyncService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private final ParkingImpl parkingImpl;
    private final String thisServer;

    public SyncServiceImpl(ParkingImpl parkingImpl, String thisServer) throws RemoteException {
        this.parkingImpl = parkingImpl;
        this.thisServer = thisServer;
    }

    @Override
    public void syncVehicleIn(ParkingInEvent slot) throws RemoteException {
        if (slot == null) return;
        if (thisServer.equals(slot.getSourceServer())) {
            return;
        }
        System.out.println("Nhận sync từ server khác");
        parkingImpl.takeVehicleInFromSync(slot);
    }

    @Override
    public void syncRentPlace(RentEvent rent) throws RemoteException {
        if (rent == null) return;
        if (thisServer.equals(rent.getSourceServer())) {
            return;
        }
        parkingImpl.getVehicleRentFromSync(rent);
    }

    @Override
    public void syncVehicleOut(ParkingOutEvent slot) throws RemoteException {
        if (slot == null) return;
        if (thisServer.equals(slot.getSourceServer())) {
            return;
        }
        try {
            parkingImpl.takeVehicleOutFromSync(slot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
