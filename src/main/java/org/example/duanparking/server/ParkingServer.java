package org.example.duanparking.server;


import org.example.duanparking.common.remote.SyncService;

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class ParkingServer  {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        String serverName = args.length > 1 ? args[1] : "SERVER_A";
        try {
            System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostName());
            //System.setProperty("java.rmi.server.hostname", "172.20.10.3");
            System.setProperty("sun.rmi.dgc.client.gcInterval", "15000"); // 15s
            System.setProperty("sun.rmi.dgc.server.gcInterval", "15000");

            String name = "ParkingService";
            ParkingImpl obj = new ParkingImpl(serverName); // Đã extend UnicastedRemote rồi
            SyncServiceImpl syncObj = new SyncServiceImpl(obj, serverName);
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(port);
                registry.list();
                System.out.println("RMI registry đã tồn tại trên port " + port);
            } catch (Exception e) {
                System.out.println("Tạo RMI registry mới trên port " + port);
                registry = LocateRegistry.createRegistry(port);
            }

            registry.rebind(name, obj);
            registry.rebind("SyncService", syncObj);
            System.out.println("ParkingServer chạy trên port " + port + "!");

//            SyncService other = (SyncService) LocateRegistry.getRegistry("172.20.10.2", 1099)
//                    .lookup("SyncService");
//            obj.addSyncTarget(other);


            System.out.println("Đã kết nối tới serverB");

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