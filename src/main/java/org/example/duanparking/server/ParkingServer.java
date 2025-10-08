package org.example.duanparking.server;

import org.example.duanparking.common.ParkingInterface;
import org.example.duanparking.common.ParkingResponse;
import org.example.duanparking.common.VehicleData;
import org.example.duanparking.model.DBManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ParkingServer implements ParkingInterface {
    private final int port;

    public ParkingServer(int port) {
        this.port = port;
    }

    @Override
    public ParkingResponse enterVehicle(VehicleData data) throws java.rmi.RemoteException {
        System.out.println("ParkingServer port " + port + " nhận enter: " + data.getPlate());
        String arrival = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(data.getTimestamp()), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (DBManager.saveEntry(data.getPlate(), arrival, data.getOwner())) {
            return new ParkingResponse("Approved", port % 10, "Lưu DB OK");
        }
        return new ParkingResponse("Denied", -1, "Lỗi DB");
    }

    @Override
    public void notifyEnter(VehicleData data) throws java.rmi.RemoteException {
        System.out.println("ParkingServer port " + port + " nhận sync enter từ trụ khác: " + data.getPlate());
        String arrival = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(data.getTimestamp()), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        DBManager.saveEntry(data.getPlate(), arrival, data.getOwner());  // Sync DB
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        try {
            ParkingServer obj = new ParkingServer(port);
            ParkingInterface stub = (ParkingInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("ParkingService", stub);
            System.out.println("ParkingServer chạy trên port " + port + "!");

            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}