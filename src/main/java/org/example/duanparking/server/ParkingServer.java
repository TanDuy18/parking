package org.example.duanparking.server;


import org.example.duanparking.common.remote.SyncService;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class ParkingServer  {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        String serverName = args.length > 1 ? args[1] : "SERVER_A";
        String myIp = args.length > 2 ? args[2] : "192.168.1.8";
        String otherIp = args.length > 3 ? args[3] : "192.168.1.11";
        try {
            // System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostName());
            System.setProperty("java.rmi.server.hostname", "192.168.1.8");
            System.setProperty("sun.rmi.dgc.client.gcInterval", "15000"); // 15s
            System.setProperty("sun.rmi.dgc.server.gcInterval", "15000");

            String name = "ParkingService";
            ParkingImpl obj = new ParkingImpl(serverName); // Đã extend UnicastedRemote rồi
            SyncServiceImpl syncObj = new SyncServiceImpl(obj, serverName);

            try {
                LocateRegistry.createRegistry(port);
                System.out.println("Tạo RMI registry mới trên port " + port);
            } catch (Exception ex) {
                System.out.println("Registry đã tồn tại trên port " + port + " (hoặc tạo thất bại): " + ex.getMessage());
            }

            Naming.rebind("rmi://" + myIp + ":" + port + "/" + name, obj);
            Naming.rebind("rmi://" + myIp + ":" + port + "/SyncService", syncObj);
            System.out.println("ParkingServer chạy trên " + myIp + ":" + port);
            System.out.println("ParkingServer chạy trên port " + port + "!");

            final int MAX_TRIES = 10;
            for (int i = 1; i <= MAX_TRIES; i++) {
                try {
                    String remoteUrl = "rmi://" + otherIp + ":" + port + "/SyncService";
                    System.out.println("Thử kết nối tới " + remoteUrl + " (attempt " + i + ")");
                    SyncService other = (SyncService) Naming.lookup(remoteUrl);
                    obj.addSyncTarget(other);
                    System.out.println("Đã kết nối tới server " + otherIp);
                    break;
                } catch (Exception ex) {
                    System.err.println("Không kết nối được tới " + otherIp + " (attempt " + i + "): " + ex.getMessage());
                    Thread.sleep(2000);
                    if (i == MAX_TRIES) System.err.println("Hết lần thử, sẽ tiếp tục chạy và chờ manual reconnect");
                }
            }
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