package org.example.duanparking.server;

import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.model.DBManager;
import org.example.duanparking.server.dao.ParkingSlotEntity;
import org.example.duanparking.model.SlotStatus;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
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
    public ArrayList<ParkingSlotDTO> getAllSlots() throws RemoteException {
        ArrayList<ParkingSlotDTO> Entities = new ArrayList<>();
        String sql = """
                SELECT 
                    ps.spot_id, ps.status, ps.row_index, ps.col_index,ps.area_type,
                    ph.vehicle_id, ph.entry_time, ph.fee,
                    v.plate_number, v.owner_name
                FROM parkingslot ps
                LEFT JOIN ParkingHistory ph 
                    ON ps.spot_id = ph.spot_id 
                    AND ph.status = 'ACTIVE'
                LEFT JOIN Vehicle v ON ph.vehicle_id = v.vehicle_id
                ORDER BY ps.row_index, ps.col_index
        """;
        try (Connection conn = DBManager.getConnection();) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        ParkingSlotDTO slotDTO = new ParkingSlotDTO();
                        slotDTO.setSpotId(rs.getString("spot_id"));
                        slotDTO.setStatus(rs.getString("status"));
                        slotDTO.setRow(rs.getInt("row_index"));
                        slotDTO.setCol(rs.getInt("col_index"));
                        slotDTO.setAreaType(rs.getString("area_type"));

                        if (rs.getObject("vehicle_id") != null) {
                            slotDTO.setPlateNumber(rs.getString("plate_number"));
                            slotDTO.setOwnerName(rs.getString("owner_name"));
                            Timestamp entryTime = rs.getTimestamp("entry_time");
                            slotDTO.setEntryTime(entryTime != null ? entryTime.toLocalDateTime().toString() : null);
                            slotDTO.setFee(rs.getDouble("fee"));
                        }
                        Entities.add(slotDTO);
                    }
            }catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return Entities;
    }

    @Override
    public void updateSlotStatus(String spotId, String status, String plateName, String owner, String arriveTime , String brand, String infor)
            throws RemoteException {
                int vehicleId;
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement lock = conn.prepareStatement("SELECT spot_id FROM parkingslot WHERE spot_id = ? FOR UPDATE"); ){
                lock.setString(1, spotId);
                ResultSet rs = lock.executeQuery();
                while (rs.next()) {
                    try(PreparedStatement updateSlot = conn.prepareStatement(
                            "UPDATE parkingslot SET status='OCCUPIED' WHERE spot_id=? AND status = 'FREE'");){
                        updateSlot.setString(1, spotId);
                        int updatedRows = updateSlot.executeUpdate();
                        if (updatedRows == 0) {
                            throw new IllegalStateException("Slot đã bị người khác chiếm hoặc không còn FREE");
                        }else {
                            try(PreparedStatement check =  conn.prepareStatement(
                                    "SELECT vehicle_id FROM vehicle WHERE plate_number = ?"
                            );){
                                check.setString(1, plateName);
                                ResultSet rsCheck = check.executeQuery();
                            
                                if (rs.next()) {
                                    vehicleId = rs.getInt("vehicle_id"); // xe đã có
                                } else {
                                    // xe chưa có → insert mới
                                    String insert = "INSERT INTO Vehicle (plate_number, owner_name, vehicle_type, brand) VALUES (?, ?, ?, ?)";
                                    PreparedStatement insertPs = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                                    insertPs.setString(1, plateName);
                                    insertPs.setString(2, owner);
                                    insertPs.setString(3, infor);
                                    insertPs.setString(4, brand);

                                    insertPs.executeUpdate();
                                    ResultSet keys = insertPs.getGeneratedKeys();
                                    keys.next();
                                    vehicleId = keys.getInt(1);
                                }
                            }
                        }
                        try (PreparedStatement insertHistory = conn.prepareStatement(
                                "INSERT INTO ParkingHistory (spot_id, vehicle_id, entry_time, status) VALUES (?, ?, ?, 'ACTIVE')")) {

                            insertHistory.setString(1, spotId);
                            insertHistory.setInt(2, vehicleId);
                            insertHistory.setString(3, arriveTime); 

                            insertHistory.executeUpdate();
                        }
                        conn.commit();

                        for (ClientCallback client : clients) {
                            try {
                                ParkingSlotDTO dto = new ParkingSlotDTO();
                                dto.setSpotId(spotId);
                                dto.setStatus(status);

                                client.onSlotUpdated(dto);
                            } catch (RemoteException e) {
                                System.err.println("Lỗi gửi callback: " + e.getMessage());
                            }
                        }
                    }catch (SQLException e) {
                        e.printStackTrace();
                        conn.rollback();
                    }
                }
            }catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ParkingSlotEntity getUpdatedSlot(String spotId) {
        ParkingSlotEntity slotUser = null;
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

                slotUser = new ParkingSlotEntity(spot_Id, status, row, col);

                return slotUser;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };

    @Override
    public void registerClient(ClientCallback client) throws RemoteException {
        clients.add(client);
        client.syncSlots(getAllSlots());
        System.out.println("Một client đã đăng ký");
    };

    @Override
    public void unreferenced() {
        System.out.println("Client chết đột ngột! Dọn dẹp tự động: " + this);
    }

   @Override
    public boolean checkId(String plateName) throws RemoteException {

        String sql =
            "SELECT ph.entry_time, ph.exit_time, ph.status " +
            "FROM parkinghistory ph " +
            "JOIN vehicle v ON v.vehicle_id = ph.vehicle_id " +
            "WHERE v.plate_number = ? " +
            "ORDER BY ph.entry_time DESC " +
            "LIMIT 1";

        try (Connection conn = DBManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, plateName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String exit_time = rs.getString("exit_time");
                String status = rs.getString("status");

                return exit_time == null || status.equalsIgnoreCase("ACTIVE");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


   @Override
    public boolean checkPlace(String placeName) throws RemoteException {
        String sql = "SELECT status FROM parkingslot WHERE spot_id = ?";

        try (Connection conn = DBManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, placeName);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");

                return status.equals("OCCUPIED") || status.equals("RESERVED");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    @Override
    public void ping() throws RemoteException {

    }

    @Override
    public void syncSlots(List<ParkingSlotDTO> slots) throws RemoteException {

    }

    @Override
    public void onSlotUpdated(ParkingSlotDTO slot) throws RemoteException {

    }

}
