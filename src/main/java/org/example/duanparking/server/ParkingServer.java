package org.example.duanparking.server;


import org.example.duanparking.common.ParkingInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class ParkingServer  {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        try {
            System.setProperty("java.rmi.server.hostname", "localhost");

            System.setProperty("sun.rmi.dgc.client.gcInterval", "15000"); // 15s
            System.setProperty("sun.rmi.dgc.server.gcInterval", "15000");

            String name = "ParkingService";
            ParkingImpl obj = new ParkingImpl(); // Đã extend UnicastedRemote rồi
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(name, obj);
            System.out.println("ParkingServer chạy trên port " + port + "!");

            keepAlive();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void keepAlive(){
        Thread keepAliveThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                System.out.println("Server đang tắt...");
                Thread.currentThread().interrupt();
            }
        }, "Server-Keep-Alive");
        keepAliveThread.setDaemon(false);
        keepAliveThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nĐang tắt ParkingServer...");
            // Dọn dẹp tài nguyên nếu cần
            System.out.println("Server đã dừng an toàn.");
        }));
    }
}