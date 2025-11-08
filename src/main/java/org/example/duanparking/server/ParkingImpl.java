package org.example.duanparking.server;

import org.example.duanparking.common.ClientCallback;
import org.example.duanparking.common.ParkingInterface;
import org.example.duanparking.model.DBManager;
import org.example.duanparking.model.ParkingSlot;
import org.example.duanparking.model.SlotStatus;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ParkingImpl extends UnicastRemoteObject implements ParkingInterface, Unreferenced {
    private Set<ClientCallback> clients = new HashSet<>();

    public ParkingImpl() throws java.rmi.RemoteException {
        super();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            clients.removeIf(client -> {
                try {
                    client.ping();
                    return false;
                } catch (Exception e) {
                    System.out.println("Xóa client chết: " + e.getMessage());
                    return true;
                }
            });
        }, 10, 15, TimeUnit.SECONDS);
    }

    @Override
    public ArrayList<ParkingSlot> getSlot() throws RemoteException {
        ArrayList<ParkingSlot> parkingSlots = new ArrayList<>();
        String sql = "SELECT spot_id, status,row_index,col_index FROM parkingslot";
        try (Connection conn = DBManager.getConnection();) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String spotId = rs.getString("spot_id");
                        String status = rs.getString("status");
                        int rowIndex = rs.getInt("row_index");
                        int colIndex = rs.getInt("col_index");

                        ParkingSlot slot = new ParkingSlot(spotId, status, rowIndex, colIndex);
                        System.out.println(slot);
                        parkingSlots.add(slot);
                    }
                }catch (SQLException e) {
                    e.printStackTrace();
                };
            }catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return parkingSlots;
    }

    @Override
    public void updateSlotStatus(String spotId, String status, String plateName, String owner, String arriveTime)
            throws RemoteException {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "UPDATE parkingslot SET status = ? WHERE spot_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, spotId);
                int rowAffected = ps.executeUpdate();

                if (rowAffected == 0) {
                    throw new SQLException("Không tìm thấy spot_id: " + spotId);
                }

                // Nếu có xe vào (OCCUPIED) thì ghi thêm vào bảng transaction
                if ("OCCUPIED".equalsIgnoreCase(status)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(arriveTime, formatter);

                    String insertTransactionSql = "INSERT INTO parkingtransaction (spot_id, plate_number, entry_time, owner_name) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement psTransaction = conn.prepareStatement(insertTransactionSql)) {
                        psTransaction.setString(1, spotId);
                        psTransaction.setString(2, plateName);
                        psTransaction.setTimestamp(3, Timestamp.valueOf(dateTime));
                        psTransaction.setString(4, owner);
                        psTransaction.executeUpdate();
                    }
                }

                conn.commit();
                System.out.println("Cập nhật trạng thái slot " + spotId + " thành công!");

                // Gửi callback cho các client sau khi commit
                for (ClientCallback client : clients) {
                    try {
                        client.onSlotUpdated(spotId, status);
                    } catch (RemoteException e) {
                        System.err.println("Lỗi gửi callback: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                conn.rollback(); // rollback nếu bất kỳ lỗi nào xảy ra
                throw e; // ném lỗi để hiển thị stacktrace bên ngoài
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ParkingSlot getUpdatedSlot(String spotId) {
        ParkingSlot slotUser = null;
        String sql = "SELECT spot_id, status, row_index, col_index FROM parkingslot where spot_id = ?";
        try (Connection conn = DBManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {

            ps.setString(1, spotId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String spot_Id = rs.getString("spot_id");
                String status = rs.getString("status");
                int row = rs.getInt("row_index");
                int col = rs.getInt("col_index");

                slotUser = new ParkingSlot(spot_Id, status, row, col);

                return slotUser;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };

    @Override
    public void registerClient(ClientCallback client) {
        clients.add(client);
        System.out.println("Một client đã đăng ký");
    };

    @Override
    public void unreferenced() {
        System.out.println("Client chết đột ngột! Dọn dẹp tự động: " + this);
    }

    @Override
    public boolean checkId(String plateName) throws RemoteException {
        String sql = "SELECT plate_number, entry_time, exit_time, status FROM parkingtransaction WHERE plate_number = ? ORDER BY entry_time DESC LIMIT 1";
        try (Connection conn = DBManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setString(1, plateName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String exit_time = rs.getString("exit_time");
                String status = rs.getString("status");

                if (exit_time == null || status.equalsIgnoreCase("ACTIVE")) {
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean checkPlace(String placeName) throws RemoteException {
        try (Connection conn = DBManager.getConnection()) {
            String sql = "SELECT status FROM parkingslot WHERE spot_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, placeName);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                SlotStatus type = SlotStatus.valueOf(status.toUpperCase());
                switch (type) {
                    case OCCUPIED, REVERSED:
                        return true;
                    default:
                        return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void ping() throws RemoteException {

    }

}
