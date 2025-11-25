package org.example.duanparking.server;

import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.common.remote.SyncService;
import org.example.duanparking.model.DBManager;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.sql.*;
import java.time.Duration;
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
    private List<String> otherServers = List.of(
            "rmi://192.168.19.128:1099/SyncService"
    );

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

    private void broadcast(ParkingSlotDTO slot) {
        for(String url: otherServers) {
            try {
               SyncService remote = (SyncService) Naming.lookup(url); 
                remote.syncVehicleIn(slot); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            } catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return Entities;
    }

    @Override
    public void updateSlotStatus(String spotId, String status, String plateName, String owner,
            String arriveTime, String brand, String infor) throws RemoteException {

        int vehicleId;

        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Khóa slot
                try (PreparedStatement lock = conn.prepareStatement(
                        "SELECT spot_id FROM parkingslot WHERE spot_id = ? FOR UPDATE")) {
                    lock.setString(1, spotId);
                    ResultSet rs = lock.executeQuery();
                    if (!rs.next()) {
                        throw new IllegalStateException("Không tìm thấy slot.");
                    }
                }

                // 2. Update slot từ FREE → OCCUPIED
                String updateSlotSql = "UPDATE parkingslot SET status=? WHERE spot_id=? AND status='FREE'";
                try (PreparedStatement updateSlot = conn.prepareStatement(updateSlotSql)) {
                    updateSlot.setString(1, status);
                    updateSlot.setString(2, spotId);

                    int updated = updateSlot.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalStateException("Slot đã bị người khác chiếm hoặc không còn FREE.");
                    }
                }

                // 3. Kiểm tra xe có tồn tại chưa
                String checkSql = "SELECT vehicle_id FROM vehicle WHERE plate_number = ?";
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, plateName);
                    ResultSet rsCheck = check.executeQuery();

                    if (rsCheck.next()) {
                        vehicleId = rsCheck.getInt("vehicle_id");
                    } else {
                        // Insert xe mới
                        String insert = "INSERT INTO Vehicle (plate_number, owner_name, vehicle_type, brand) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement insertPs = conn.prepareStatement(insert,
                                Statement.RETURN_GENERATED_KEYS)) {

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

                // 4. Insert lịch sử parking
                String insertHistorySql = "INSERT INTO ParkingHistory (spot_id, vehicle_id, entry_time, status) " +
                        "VALUES (?, ?, ?, 'ACTIVE')";
                try (PreparedStatement insertHistory = conn.prepareStatement(insertHistorySql)) {
                    insertHistory.setString(1, spotId);
                    insertHistory.setInt(2, vehicleId);
                    insertHistory.setString(3, arriveTime);

                    insertHistory.executeUpdate();
                }

                conn.commit();

                new Thread(() -> {
                    for (ClientCallback client : clients) {
                        try {
                            ParkingSlotDTO dto = new ParkingSlotDTO();
                            dto.setSpotId(spotId);
                            dto.setStatus(status);
                            broadcast(dto);
                            client.onSlotUpdated(dto);
                        } catch (RemoteException ex) {
                            System.err.println("Lỗi callback: " + ex.getMessage());
                        }
                    }
                }).start();

            } catch (Exception e) {
                conn.rollback(); // rollback toàn bộ nếu có lỗi
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
    public boolean checkIdIn(String plateName) throws RemoteException {

        String sql = "SELECT ph.entry_time, ph.exit_time, ph.status " +
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
    public boolean checkPlaceIn(String placeName) throws RemoteException {
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
    public ParkingSlotDTO getVehicleInfoForOut(String plateNumber) throws RemoteException {
        double fee = 0;
        ParkingSlotDTO out = new ParkingSlotDTO();
        try (Connection conn = DBManager.getConnection()) {

            String selectSql = "SELECT ph.transaction_id, ph.entry_time, ph.spot_id, v.vehicle_type, v.brand, v.owner_name "
                    +
                    "FROM ParkingHistory ph " +
                    "JOIN Vehicle v ON ph.vehicle_id = v.vehicle_id " +
                    "WHERE v.plate_number = ? AND ph.status='ACTIVE' ORDER BY ph.entry_time DESC LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, plateNumber);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    throw new IllegalStateException("Xe không có trong bãi hoặc đã OUT.");
                }

                int transactionId = rs.getInt("transaction_id");
                String spotId = rs.getString("spot_id");
                String vehicleType = rs.getString("vehicle_type");
                String brand = rs.getString("brand");
                String ownerName = rs.getString("owner_name");
                LocalDateTime entryTime = rs.getObject("entry_time", LocalDateTime.class);
                System.out.println(entryTime);
                LocalDateTime exitTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String leaveTime = exitTime.format(formatter);

                String pricingSql = "SELECT hourly_rate, daily_rate FROM SlotPricing sp " +
                        "JOIN parkingslot ps ON ps.area_type = sp.area_type " +
                        "WHERE ps.spot_id = ? AND sp.vehicle_type = ? " +
                        "ORDER BY effective_from DESC LIMIT 1";

                try (PreparedStatement psPricing = conn.prepareStatement(pricingSql)) {
                    psPricing.setString(1, spotId);
                    psPricing.setString(2, vehicleType);
                    ResultSet rsPricing = psPricing.executeQuery();

                    if (!rsPricing.next()) {
                        throw new IllegalStateException("Không tìm thấy bảng giá cho xe này");
                    }

                    double hourlyRate = rsPricing.getDouble("hourly_rate");
                    double dailyRate = rsPricing.getDouble("daily_rate");

                    long minutes = Duration.between(entryTime, exitTime).toMinutes();
                    long hours = minutes / 60;
                    if (hours == 0)
                        hours = 1; // tối thiểu 1 giờ

                    if (hours >= 24) {
                        fee = dailyRate * ((double) hours / 24);
                    } else {
                        fee = hourlyRate * hours;
                    }
                    out.setPlateNumber(plateNumber);
                    out.setEntryTime(entryTime.format(formatter));
                    out.setExitTime(leaveTime);
                    out.setFee(fee);
                    out.setSpotId(spotId);
                    out.setBrand(brand);
                    out.setVehicleType(vehicleType);
                    out.setOwnerName(ownerName);
                    out.setTransaction_id(transactionId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Lỗi OUT xe: " + e.getMessage());
        }
        return out;
    }

    @Override
    public ParkingSlotDTO getVehicleInfoForIn(String plateNumber) throws RemoteException {
        return null;
    }

    @Override
    public boolean takeVehicleOut(ParkingSlotDTO slot) throws RemoteException {
        Connection conn = null;

        try {
            conn = DBManager.getConnection();
            conn.setAutoCommit(false);

            String lockSql = "SELECT spot_id FROM parkingslot WHERE spot_id=? FOR UPDATE";
            try (PreparedStatement lock = conn.prepareStatement(lockSql)) {
                lock.setString(1, slot.getSpotId());
                ResultSet rs = lock.executeQuery();
                if (!rs.next())
                    throw new IllegalStateException("Slot không tồn tại!");
            }

            // 2️⃣ Update ParkingHistory → OUT
            String updateHistory = """
                        UPDATE parkinghistory SET exit_time=?, fee=?, status='COMPLETED'
                        WHERE transaction_id=? AND status='ACTIVE'
                    """;

            try (PreparedStatement ps = conn.prepareStatement(updateHistory)) {
                ps.setString(1, slot.getExitTime());
                ps.setDouble(2, slot.getFee());
                ps.setInt(3, slot.getTransaction_id());

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    throw new IllegalStateException("Không tìm thấy giao dịch ACTIVE để OUT!");
                }
            }

            String paySql = """
                        INSERT INTO Payment(transaction_id, amount, payment_type, payment_method)
                        VALUES (?, ?, 'VISITOR', ?)
                    """;

            try (PreparedStatement ps = conn.prepareStatement(paySql)) {
                ps.setInt(1, slot.getTransaction_id()); // transaction ID từ ParkingHistory
                ps.setDouble(2, slot.getFee()); // số tiền tính được
                ps.setString(3, "CASH"); // hoặc CARD/TRANSFER
                ps.executeUpdate();
            }

            String sqlUpdateSlot = "UPDATE parkingslot SET status='FREE' WHERE spot_id=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateSlot)) {
                ps.setString(1, slot.getSpotId());
                ps.executeUpdate();
            }

            conn.commit();

            ParkingSlotDTO dto = new ParkingSlotDTO();
            dto.setSpotId(slot.getSpotId());
            dto.setStatus("FREE");
            dto.setAreaType("PREMIUM");

            for (ClientCallback client : clients) {
                try {
                    client.onSlotUpdated(dto);
                } catch (Exception ex) {
                    System.out.println("Client lỗi khi callback OUT: " + ex.getMessage());
                }
            }

            return true;

        } catch (Exception e) {

            // rollback nếu lỗi
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            throw new RemoteException("Lỗi OUT xe: " + e.getMessage());

        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
