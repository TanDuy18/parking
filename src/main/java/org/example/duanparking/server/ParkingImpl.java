package org.example.duanparking.server;

import org.example.duanparking.common.ClientCallback;
import org.example.duanparking.common.ParkingInterface;
import org.example.duanparking.model.DBManager;
import org.example.duanparking.model.ParkingSlot;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ParkingImpl extends UnicastRemoteObject implements ParkingInterface {
    private Set<ClientCallback> clients = new HashSet<>();

    public ParkingImpl() throws java.rmi.RemoteException {
        super();
    }

    @Override
    public ArrayList<ParkingSlot> getSlot() throws RemoteException {
        ArrayList<ParkingSlot> parkingSlots = new ArrayList<>();
        try (Connection conn = DBManager.getConnection()) {
            String sql = "SELECT spot_id, status,row_index,col_index FROM parkingslot";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String spotId = rs.getString("spot_id");
                String status = rs.getString("status");
                int rowIndex = rs.getInt("row_index");
                int colIndex = rs.getInt("col_index");

                ParkingSlot slot = new ParkingSlot(spotId, status, rowIndex, colIndex);
                System.out.println(slot);
                parkingSlots.add(slot);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return parkingSlots;
    }

    @Override
    public void updateSlotStatus(String spotId, String status, String plateName, String owner,String arriveTime) throws RemoteException {
        try {
            Connection conn = DBManager.getConnection();
            conn.setAutoCommit(true);
            String sql = "UPDATE parkingslot SET status = ? where spot_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, spotId);
            int rowAffected = ps.executeUpdate();

            if (rowAffected == 0) {
                throw new SQLException("Ko tìm thấy spot_id");
            } else {
                if ("OCCUPIED".equalsIgnoreCase(status)) {

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(arriveTime, formatter);

                    String insertTransactionSql = "INSERT INTO parkingtransaction(spot_id, plate_number, entry_time, owner_name) VALUES(?,?,?,?)";

                    PreparedStatement psTransaction = conn.prepareStatement(insertTransactionSql);
                    psTransaction.setString(1, spotId);
                    psTransaction.setString(2, plateName);
                    psTransaction.setTimestamp(3, Timestamp.valueOf(dateTime));
                    psTransaction.setString(4, owner);

                    psTransaction.executeUpdate();
                    System.out.println("Có một vị trí vào");
                }
                System.out.println("Cập nhật status slot " + spotId + "thành công");

                for (ClientCallback client : clients) {
                    try {
                        client.onSlotUpdated(spotId, status);
                    } catch (RemoteException e) {
                        System.err.println("Lỗi gửi callback");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    @Override
    public ParkingSlot getUpdatedSlot(String spotId) {
        ParkingSlot slotUser;
        try (Connection conn = DBManager.getConnection()) {
            String sql = "SELECT spot_id, status, row_index, col_index FROM parkingslot where spot_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
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
            // TODO: handle exception
        }
        return null;
    };

    @Override
    public void registerClient(ClientCallback client) {
        clients.add(client);
        System.out.println("Một client đã đăng ký");
    };

    @Override
    public void unregisterClient(ClientCallback client) {
        clients.remove(client);
        System.out.println("Client đã hủy đăng ký");
    };
}
