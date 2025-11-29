package org.example.duanparking.server;

import org.example.duanparking.common.dto.ParkingHistoryDTO;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.dto.SlotStatusDTO;
import org.example.duanparking.common.dto.VehicleDTO;
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
                        VehicleDTO vehicle = new VehicleDTO();
                        vehicle.setVehicleId(rs.getInt("vehicle_id"));
                        vehicle.setPlateNumber(rs.getString("plate_number"));
                        vehicle.setOwner(rs.getString("owner_name"));

                        slotDTO.setVehicle(vehicle);

                        ParkingHistoryDTO history = new ParkingHistoryDTO();
                        history.setSpotId(rs.getString("spot_id"));
                        history.setVehicleId(rs.getInt("vehicle_id"));

                        LocalDateTime entryTime = rs.getObject("entry_time", LocalDateTime.class);
                        history.setEntryTime(entryTime);

                        history.setFee(rs.getDouble("fee"));

                        slotDTO.setHistory(history);

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
    public int updateSlotStatus(ParkingSlotDTO slot1) throws RemoteException {

        int vehicleId;

        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            String currentStatus ;
            int currentVersion;
            try {
                try (PreparedStatement lock = conn.prepareStatement("SELECT status,version FROM parkingslot WHERE spot_id = ?")) {
                    lock.setString(1, slot1.getSpotId());
                    try(ResultSet rs = lock.executeQuery();) {
                        if (!rs.next()) {
                            return 3;
                        }
                        currentStatus = rs.getString("status");
                        currentVersion = rs.getInt("version");
                    }
                }

                if ("OCCUPIED".equals(currentStatus)) {
                    conn.rollback();
                    return 1;
                }

                String updateSlotSql = "UPDATE parkingslot SET status=?, version = version + 1 WHERE spot_id=? AND version = ?";
                try (PreparedStatement updateSlot = conn.prepareStatement(updateSlotSql)) {
                    updateSlot.setString(1, slot1.getStatus());
                    updateSlot.setString(2, slot1.getSpotId());
                    updateSlot.setInt(3, currentVersion);

                    int updated = updateSlot.executeUpdate();
                    if (updated == 0) {
                        return 2; // race condition: không update được
                    }
                }

                String checkSql = "SELECT vehicle_id FROM vehicle WHERE plate_number = ?";
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, slot1.getVehicle().getPlateNumber());
                    try(ResultSet rsCheck = check.executeQuery();) {
                        if (rsCheck.next()) {
                            vehicleId = rsCheck.getInt("vehicle_id");
                        } else {
                            // Insert xe mới
                            String insert = "INSERT INTO Vehicle (plate_number, owner_name, vehicle_type, brand) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement insertPs = conn.prepareStatement(insert,
                                    Statement.RETURN_GENERATED_KEYS)) {

                                insertPs.setString(1, slot1.getVehicle().getPlateNumber());
                                insertPs.setString(2, slot1.getVehicle().getOwner());
                                insertPs.setString(3, slot1.getVehicle().getVehicleType());
                                insertPs.setString(4, slot1.getVehicle().getBrand());

                                insertPs.executeUpdate();
                                ResultSet keys = insertPs.getGeneratedKeys();
                                keys.next();
                                vehicleId = keys.getInt(1);
                            } catch (SQLException e) {
                                if (e.getErrorCode() == 1062 /* MySQL duplicate key code*/) {
                                    try (PreparedStatement checkAgain = conn.prepareStatement(checkSql)) {
                                        checkAgain.setString(1, slot1.getVehicle().getPlateNumber());
                                        ResultSet rsCheckAgain = checkAgain.executeQuery();
                                        if (rsCheckAgain.next()) {
                                            vehicleId = rsCheckAgain.getInt("vehicle_id");
                                        } else {
                                            throw e;
                                        }
                                    }
                                } else {
                                    throw e;
                                }
                            }
                        }
                    }
                }

                String checkActive = "SELECT 1 FROM ParkingHistory WHERE vehicle_id = ? AND status='ACTIVE'";
                try(PreparedStatement ps = conn.prepareStatement(checkActive)) {
                    ps.setInt(1, vehicleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return 4; // đã đậu xe
                        }
                    }
                }

                String insertHistorySql = "INSERT INTO ParkingHistory (spot_id, vehicle_id, entry_time, status) " +
                        "VALUES (?, ?, ?, 'ACTIVE')";
                try (PreparedStatement insertHistory = conn.prepareStatement(insertHistorySql)) {
                    insertHistory.setString(1, slot1.getSpotId());
                    insertHistory.setInt(2, vehicleId);
                    insertHistory.setObject(3, slot1.getHistory().getEntryTime());

                    insertHistory.executeUpdate();
                }catch (SQLException e) {
                    if(e.getErrorCode() == 1062) {
                        return 5;
                    }
                }
                conn.commit();

                new Thread(() -> {
                    for (ClientCallback client : clients) {
                        try {
                            SlotStatusDTO slot = new SlotStatusDTO(slot1.getSpotId(), slot1.getStatus());
                            // broadcast(dto);
                            client.onSlotUpdated(slot);
                        } catch (RemoteException ex) {
                            System.err.println("Lỗi callback: " + ex.getMessage());
                        }
                    }
                }).start();
                return 0;

            } catch (Exception e) {
                conn.rollback(); // rollback toàn bộ nếu có lỗi
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 999;
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
                    VehicleDTO vehicle = new VehicleDTO();
                    vehicle.setPlateNumber(plateNumber);
                    vehicle.setBrand(brand);
                    vehicle.setOwner(ownerName);
                    vehicle.setVehicleType(vehicleType);

                    ParkingHistoryDTO history = new ParkingHistoryDTO();
                    history.setTransactionId(transactionId);
                    history.setEntryTime(entryTime);
                    history.setExitTime(exitTime);
                    history.setFee(fee);

                    out.setSpotId(spotId);

                    out.setVehicle(vehicle);
                    out.setHistory(history);
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

           String getAreaType = "SELECT area_type FROM parkingslot WHERE spot_id = ?";

           try(PreparedStatement getInfor = conn.prepareStatement(getAreaType)) {
               getInfor.setString(1, slot.getSpotId()); 

               ResultSet rs = getInfor.executeQuery();
               if (rs.next()) {
                   slot.setAreaType(rs.getString("area_type"));
               }
            } 
            String updateHistory = """
                        UPDATE parkinghistory SET exit_time=?, fee=?, status='COMPLETED'
                        WHERE transaction_id=? AND status='ACTIVE'
                    """;

            try (PreparedStatement ps = conn.prepareStatement(updateHistory)) {
                ps.setObject(1, slot.getHistory().getExitTime());
                ps.setDouble(2, slot.getHistory().getFee());
                ps.setInt(3, slot.getHistory().getTransactionId());

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
                ps.setInt(1, slot.getHistory().getTransactionId()); // transaction ID từ ParkingHistory
                ps.setDouble(2, slot.getHistory().getFee()); // số tiền tính được
                ps.setString(3, "TRANSFER");
                ps.executeUpdate();
            }

            String sqlUpdateSlot = "UPDATE parkingslot SET status='FREE' WHERE spot_id=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateSlot)) {
                ps.setString(1, slot.getSpotId());
                ps.executeUpdate();
            }

            SlotStatusDTO dto = new SlotStatusDTO(slot.getSpotId(),"FREE",slot.getAreaType());


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

    @Override
    public void updateSlotFromSync(ParkingSlotDTO slot) {
//        try (Connection conn = DBManager.getConnection()) {
//            conn.setAutoCommit(false);
//
//
//
//            String sqlUpdate = "UPDATE parkingslot SET status=? WHERE spot_id=?";
//            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
//                ps.setString(1, slot.getStatus());
//                ps.setString(2, slot.getSpotId());
//                int updated = ps.executeUpdate();
//                System.out.println("SYNC UPDATE SLOT = " + updated);
//            }
//
//            if (slot.getPlateNumber() != null) {
//                int vehicleId;
//
//                // Check vehicle
//                String checkV = "SELECT vehicle_id FROM vehicle WHERE plate_number=?";
//                try (PreparedStatement check = conn.prepareStatement(checkV)) {
//                    check.setString(1, slot.getPlateNumber());
//                    ResultSet rs = check.executeQuery();
//
//                    if (rs.next()) {
//                        vehicleId = rs.getInt(1);
//                    } else {
//                        String insV = "INSERT INTO vehicle(plate_number, owner_name, vehicle_type, brand) VALUES (?,?,?,?)";
//                        try (PreparedStatement ins = conn.prepareStatement(insV, Statement.RETURN_GENERATED_KEYS)) {
//                            ins.setString(1, slot.getPlateNumber());
//                            ins.setString(2, slot.getOwnerName());
//                            ins.setString(3, slot.getVehicleType());
//                            ins.setString(4, slot.getBrand());
//                            ins.executeUpdate();
//                            ResultSet keys = ins.getGeneratedKeys();
//                            keys.next();
//                            vehicleId = keys.getInt(1);
//                        }
//                    }
//                }
//
//                // Check history trước khi insert
//                String checkH = "SELECT 1 FROM ParkingHistory WHERE spot_id=? AND status='ACTIVE'";
//                try (PreparedStatement ch = conn.prepareStatement(checkH)) {
//                    ch.setString(1, slot.getSpotId());
//                    ResultSet rs = ch.executeQuery();
//
//                    if (!rs.next()) {
//                        String insertH = "INSERT INTO ParkingHistory(spot_id, vehicle_id, entry_time, status, fee) VALUES (?, ?, ?, 'ACTIVE', ?)";
//                        try (PreparedStatement ph = conn.prepareStatement(insertH)) {
//                            ph.setString(1, slot.getSpotId());
//                            ph.setInt(2, vehicleId);
//                            ph.setTimestamp(3, Timestamp.valueOf(time));
//                            ph.setDouble(4, slot.getFee());
//                            ph.executeUpdate();
//                        }
//                    }
//                }
//            }
//
//            conn.commit();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }



    @Override
    public void takeVehicleOutFromSync(ParkingSlotDTO slot) {
//        try (Connection conn = DBManager.getConnection()) {
//            conn.setAutoCommit(false);
//
//            // Update ParkingHistory
//            String updateHistory = "UPDATE ParkingHistory SET exit_time=?, fee=?, status='COMPLETED' WHERE transaction_id=?";
//            try (PreparedStatement ps = conn.prepareStatement(updateHistory)) {
//                ps.setString(1, slot.getExitTime());
//                ps.setDouble(2, slot.getFee());
//                ps.setInt(3, slot.getTransaction_id());
//                ps.executeUpdate();
//            }
//
//            // Update slot status
//            try (PreparedStatement ps = conn.prepareStatement(
//                    "UPDATE parkingslot SET status='FREE' WHERE spot_id=?")) {
//                ps.setString(1, slot.getSpotId());
//                ps.executeUpdate();
//            }
//
//            conn.commit();
//
//            // Update GUI client
//            ParkingSlotDTO dto = new ParkingSlotDTO();
//            dto.setSpotId(slot.getSpotId());
//            dto.setStatus("FREE");
//
//            for (ClientCallback client : clients) {
//                try {
//                    client.onSlotUpdated(dto);
//                } catch (Exception e) {
//                    System.out.println("Client lỗi callback OUT sync: " + e.getMessage());
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


}
