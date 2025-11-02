package org.example.duanparking.server;


import org.example.duanparking.common.ParkingInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class ParkingServer  {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        try {
            String name = "ParkingService";
            ParkingImpl obj = new ParkingImpl(); // Đã extend UnicastedRemote rồi
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(name, obj);
            System.out.println("ParkingServer chạy trên port " + port + "!");

            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}