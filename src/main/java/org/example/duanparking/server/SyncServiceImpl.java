package org.example.duanparking.server;

import org.example.duanparking.common.dto.ParkingInEvent;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.remote.SyncService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private final ParkingImpl parkingImpl;
    private final String thisServer;

    public SyncServiceImpl(ParkingImpl parkingImpl, String thisServer) throws RemoteException {
        this.parkingImpl = parkingImpl;
        this.thisServer = System.getProperty("server.name", "SERVER_A");
    }

    @Override
    public void syncVehicleIn(ParkingInEvent slot) throws RemoteException {
        if (slot == null) return;
        if (thisServer.equals(slot.getSourceServer())) {
            // event từ chính nó — bỏ qua
            return;
        }
        // gửi vào ParkingImpl để xử lý (không broadcast lại)
        parkingImpl.takeVehicleInFromSync(slot);

    }

    @Override
    public void syncVehicleOut(ParkingSlotDTO slot) throws RemoteException {
        System.out.println("Nhận OUT từ server khác: " + slot.getSpotId());
        try {
            parkingImpl.takeVehicleOutFromSync(slot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
