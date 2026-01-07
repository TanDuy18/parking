package org.example.duanparking.server;

import org.example.duanparking.common.dto.*;
import org.example.duanparking.common.dto.rent.*;
import org.example.duanparking.common.remote.ClientCallback;
import org.example.duanparking.common.remote.ParkingInterface;
import org.example.duanparking.common.remote.SyncService;
import org.example.duanparking.model.DBManager;
import org.example.duanparking.model.DisplayMode;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParkingImpl extends UnicastRemoteObject implements ParkingInterface, Unreferenced {
    private Set<ClientCallback> clients = new HashSet<>();
    private String serverName;
    private List<SyncService> syncTargets = new ArrayList<>();

    public ParkingImpl(String serverName) throws RemoteException {
        this.serverName = serverName;
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

    public String getServerName() {
        return serverName;
    }

    public void addSyncTarget(SyncService sync) {
        syncTargets.add(sync);
    }

    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(5);

    private void broadcastVehicleIn(ParkingInEvent event) {
        syncExecutor.submit(() -> {
            System.out.println("[SYNC-IN] Bắt đầu đồng bộ xe vào: " + event.getPlateNumber());

            for (SyncService s : syncTargets) {
                try {
                    s.syncVehicleIn(event);
                    System.out.println("[SYNC-IN] Đã gửi thông tin xe " + event.getPlateNumber() + " tới server đích.");
                } catch (Exception e) {
                    System.err.println("[SYNC-IN] Lỗi khi đồng bộ tới " + s.toString() + ": " + e.getMessage());
                }
            }
        });
    }

    private void broadcastVehicleOut(ParkingOutEvent event) {
        syncExecutor.submit(() -> {
            System.out.println("[SYNC] Bắt đầu đồng bộ xe ra: " + event.getPlateNumber());

            for (SyncService targetServer : syncTargets) {
                try {
                    targetServer.syncVehicleOut(event);
                    System.out.println("[SYNC] Thành công tới: " + targetServer.toString());
                } catch (Exception e) {
                    System.err.println("[SYNC] Lỗi khi gửi tới Server " + targetServer + ": " + e.getMessage());
                }
            }
        });
    }
    private void broadcastVehicleRent(RentEvent event) {
        syncExecutor.submit(() -> {
            System.out.println("[SYNC-RENT] Đang gửi lịch thuê xe: " + event.getPlate() + " cho ô: " + event.getPlace());

            for (SyncService targetServer : syncTargets) {
                try {
                    targetServer.syncRentPlace(event);
                    System.out.println("[SYNC-RENT] Đồng bộ thành công tới server: " + targetServer.toString());
                } catch (Exception e) {
                    System.err.println("[SYNC-RENT] Lỗi gửi tới server " + targetServer + ": " + e.getMessage());
                }
            }
        });
    }
    @Override
    public ArrayList<ParkingSlotDTO> getAllSlots() throws RemoteException {
        Map<String, ParkingSlotDTO> slotMap = new LinkedHashMap<>();
        String sql = """
            SELECT 
                ps.spot_id, ps.row_index, ps.col_index, ps.area_type, ps.status AS original_status, ps.zone,
                ph.transaction_id, ph.vehicle_id AS visitor_v_id, ph.entry_time, ph.fee,
                v_visitor.plate_number AS visitor_plate, 
                v_visitor.owner_name AS visitor_name,
                r.renter_id, r.monthly_rate, r.spot_id AS rent_spot_id, -- Lấy thêm thông tin hợp đồng
                rs.day_of_week, rs.start_time, rs.end_time,
                v_renter.owner_name AS renter_name
            FROM parkingslot ps
            LEFT JOIN ParkingHistory ph ON ps.spot_id = ph.spot_id AND ph.status = 'ACTIVE'
            LEFT JOIN Vehicle v_visitor ON ph.vehicle_id = v_visitor.vehicle_id
            LEFT JOIN renter r ON ps.spot_id = r.spot_id AND r.status = 'ACTIVE' 
                AND CURRENT_DATE BETWEEN r.start_date AND r.end_date
            LEFT JOIN Vehicle v_renter ON r.vehicle_id = v_renter.vehicle_id
            LEFT JOIN RenterSchedule rs ON r.renter_id = rs.renter_id 
                AND rs.day_of_week = UPPER(LEFT(DAYNAME(NOW()), 3))
            ORDER BY ps.row_index, ps.col_index, rs.start_time ASC;
            """;

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("spot_id");
                ParkingSlotDTO slotDTO = slotMap.get(id);

                if (slotDTO == null) {
                    slotDTO = new ParkingSlotDTO();
                    slotDTO.setSpotId(id);
                    slotDTO.setRow(rs.getInt("row_index"));
                    slotDTO.setCol(rs.getInt("col_index"));
                    slotDTO.setAreaType(rs.getString("area_type"));
                    slotDTO.setZone(rs.getString("zone"));

                    // Trạng thái đỗ xe thực tế
                    String status = rs.getString("original_status");
                    if (rs.getObject("transaction_id") != null) {
                        status = "OCCUPIED";
                    }
                    slotDTO.setStatus(status);

                    // --- 1. Ánh xạ Xe đang đỗ (Visitor/Current Occupant) ---
                    if (rs.getObject("visitor_v_id") != null) {
                        VehicleDTO vehicle = new VehicleDTO();
                        vehicle.setVehicleId(rs.getInt("visitor_v_id"));
                        vehicle.setPlateNumber(rs.getString("visitor_plate"));
                        vehicle.setOwner(rs.getString("visitor_name"));
                        slotDTO.setVehicle(vehicle);

                        ParkingHistoryDTO history = new ParkingHistoryDTO();
                        history.setSpotId(id);
                        history.setEntryTime(rs.getTimestamp("entry_time") != null ? rs.getTimestamp("entry_time").toLocalDateTime() : null);
                        history.setFee(rs.getDouble("fee"));
                        slotDTO.setParkingHistory(history);
                    }

                    // --- 2. Ánh xạ Thông tin Hợp đồng (RentDTO) ---
                    int rentId = rs.getInt("renter_id");
                    if (!rs.wasNull()) {
                        RentDTO rent = new RentDTO();
                        rent.setRentID(rentId);
                        rent.setMonthlyRate(rs.getDouble("monthly_rate"));
                        rent.setSpotId(rs.getString("rent_spot_id"));

                        slotDTO.setCurrentRent(rent);
                        slotDTO.setRentID(rentId); // Đồng bộ flag ra ngoài
                    }

                    slotMap.put(id, slotDTO);
                }

                // --- 3. Ánh xạ Lịch trình (Schedules) vào RentDTO ---
                Time sqlStart = rs.getTime("start_time");
                Time sqlEnd = rs.getTime("end_time");

                if (sqlStart != null && sqlEnd != null && slotDTO.getCurrentRent() != null) {
                    ScheduleDTO s = new ScheduleDTO();
                    s.setDayOfWeek(rs.getString("day_of_week"));
                    s.setStartTime(sqlStart.toLocalTime());
                    s.setEndTime(sqlEnd.toLocalTime());
                    s.setRenterName(rs.getString("renter_name"));

                    slotDTO.getCurrentRent().getSchedules().add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(slotMap.values());
    }

    @Override
    public int updateSlotStatus(ParkingSlotDTO slot1) throws RemoteException {
        int vehicleId;
        int currentVersion;
        int newVersion;
        String currentStatus;
        LocalDateTime actualEntryTime;

        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // --- BƯỚC 1: KHÓA VÀ KIỂM TRA SLOT ---
                String lockSql = "SELECT status, version FROM parkingslot WHERE spot_id = ? FOR UPDATE";
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setString(1, slot1.getSpotId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return 3;
                        currentStatus = rs.getString("status");
                        currentVersion = rs.getInt("version");
                        if ("OCCUPIED".equals(currentStatus)) {
                            conn.rollback();
                            return 1;
                        }
                    }
                }

                vehicleId = getOrCreateVehicle(conn, slot1.getVehicle());
                if (isVehicleInside(conn, vehicleId)) {
                    conn.rollback();
                    return 4;
                }

                // --- BƯỚC 2: CẬP NHẬT SLOT ---
                newVersion = currentVersion + 1;
                String updateSlotSql = "UPDATE parkingslot SET status='OCCUPIED', version = ? WHERE spot_id=? AND version = ?";
                try (PreparedStatement updatePs = conn.prepareStatement(updateSlotSql)) {
                    updatePs.setInt(1, newVersion);
                    updatePs.setString(2, slot1.getSpotId());
                    updatePs.setInt(3, currentVersion);
                    if (updatePs.executeUpdate() == 0) {
                        conn.rollback();
                        return 2;
                    }
                }

                // --- BƯỚC 3: CHÈN LỊCH SỬ ---
                actualEntryTime = (slot1.getParkingHistory() != null && slot1.getParkingHistory().getEntryTime() != null)
                        ? slot1.getParkingHistory().getEntryTime()
                        : LocalDateTime.now();

                String insertHistory = "INSERT INTO ParkingHistory (spot_id, vehicle_id, entry_time, status) VALUES (?, ?, ?, 'ACTIVE')";
                try (PreparedStatement historyPs = conn.prepareStatement(insertHistory)) {
                    historyPs.setString(1, slot1.getSpotId());
                    historyPs.setInt(2, vehicleId);
                    historyPs.setTimestamp(3, java.sql.Timestamp.valueOf(actualEntryTime));
                    historyPs.executeUpdate();
                }

                conn.commit(); // CHỐT DỮ LIỆU XUỐNG DB

                // --- BƯỚC 4: BROADCAST ĐỒNG BỘ ---
                ParkingInEvent inEvent = new ParkingInEvent(
                        slot1.getSpotId(),
                        slot1.getVehicle().getPlateNumber(),
                        slot1.getVehicle().getVehicleType(),
                        actualEntryTime,
                        slot1.getVehicle().getOwner(),
                        slot1.getVehicle().getBrand(),
                        newVersion,
                        this.serverName
                );

                broadcastVehicleIn(inEvent);
                notifyClients(slot1.getSpotId(), "OCCUPIED");

                return 0;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 999;
        }
    }

    private Integer getReservedVehicleNow(Connection conn, String spotId) throws SQLException {
        String sql = "SELECT r.vehicle_id FROM renter r " +
                "JOIN RenterSchedule rs ON r.renter_id = rs.renter_id " +
                "WHERE r.spot_id = ? AND r.status = 'ACTIVE' " +
                "AND rs.day_of_week = UPPER(DATE_FORMAT(NOW(), '%a')) " +
                "AND (CURTIME() BETWEEN SUBTIME(rs.start_time, '01:00:00') AND rs.end_time)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, spotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("vehicle_id");
            }
        }
        return null;
    }

    private boolean isVehicleInside(Connection conn, int vehicleId) throws SQLException {
        String sql = "SELECT 1 FROM ParkingHistory WHERE vehicle_id = ? AND status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getOrCreateVehicle(Connection conn, VehicleDTO v) throws SQLException {
        String query = "SELECT vehicle_id FROM Vehicle WHERE plate_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, v.getPlateNumber());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {return rs.getInt("vehicle_id");}
            }
        }
        String insert = "INSERT INTO Vehicle (plate_number, vehicle_type, owner_name, brand) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getPlateNumber());
            ps.setString(2, v.getVehicleType());
            ps.setString(3, v.getOwner());
            ps.setString(4, v.getBrand());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    private void notifyClients(String spotId, String status) {
        new Thread(() -> {
            for (ClientCallback client : clients) {
                try {
                    client.onSlotUpdated(new SlotStatusDTO(spotId, status));
                } catch (RemoteException ignored) {}
            }
        }).start();
    }

    @Override
    public void registerClient(ClientCallback client) throws RemoteException {
        clients.add(client);
        client.syncSlots(getAllSlots());
        System.out.println("Một client đã đăng ký");
    };

    @Override
    public void unregisterClient(ClientCallback client) throws RemoteException {
        clients.remove(client);

    }

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
            // 1. Lấy thông tin xe và lượt đỗ hiện tại
            String selectSql = "SELECT ph.transaction_id, ph.entry_time, ph.spot_id, v.vehicle_id, v.vehicle_type, v.brand, v.owner_name "
                    + "FROM ParkingHistory ph "
                    + "JOIN Vehicle v ON ph.vehicle_id = v.vehicle_id "
                    + "WHERE v.plate_number = ? AND ph.status='ACTIVE' ORDER BY ph.entry_time DESC LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, plateNumber);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) return null; // Trả về null nếu không thấy xe

                int vehicleId = rs.getInt("vehicle_id");
                int transactionId = rs.getInt("transaction_id");
                String spotId = rs.getString("spot_id");
                String vehicleType = rs.getString("vehicle_type");
                LocalDateTime entryTime = rs.getObject("entry_time", LocalDateTime.class);
                LocalDateTime exitTime = LocalDateTime.now();

                // --- BƯỚC MỚI: KIỂM TRA XEM CÓ PHẢI KHÁCH THUÊ KHÔNG ---
                boolean isRenter = false;
                String checkRenterSql = "SELECT 1 FROM renter WHERE vehicle_id = ? AND spot_id = ? AND status = 'ACTIVE'";
                try (PreparedStatement psRenter = conn.prepareStatement(checkRenterSql)) {
                    psRenter.setInt(1, vehicleId);
                    psRenter.setString(2, spotId);
                    if (psRenter.executeQuery().next()) {
                        isRenter = true;
                    }
                }

                if (isRenter) {
                    fee = 0;
                } else {
                    String pricingSql = "SELECT hourly_rate, daily_rate FROM SlotPricing sp " +
                            "JOIN parkingslot ps ON ps.area_type = sp.area_type " +
                            "WHERE ps.spot_id = ? AND sp.vehicle_type = ? " +
                            "ORDER BY effective_from DESC LIMIT 1";

                    try (PreparedStatement psPricing = conn.prepareStatement(pricingSql)) {
                        psPricing.setString(1, spotId);
                        psPricing.setString(2, vehicleType);
                        ResultSet rsPricing = psPricing.executeQuery();

                        if (rsPricing.next()) {
                            double hourlyRate = rsPricing.getDouble("hourly_rate");
                            double dailyRate = rsPricing.getDouble("daily_rate");

                            long minutes = Duration.between(entryTime, exitTime).toMinutes();
                            double hours = Math.ceil(minutes / 60.0);
                            if (hours < 1) hours = 1;

                            if (hours >= 24) {
                                fee = dailyRate * (hours / 24.0);
                            } else {
                                fee = hourlyRate * hours;
                            }
                        }
                    }
                }

                // Gán dữ liệu vào DTO để trả về Client
                VehicleDTO vehicle = new VehicleDTO();
                vehicle.setPlateNumber(plateNumber);
                vehicle.setVehicleType(vehicleType);
                vehicle.setBrand(rs.getString("brand"));
                vehicle.setOwner(rs.getString("owner_name"));

                ParkingHistoryDTO history = new ParkingHistoryDTO();
                history.setTransactionId(transactionId);
                history.setEntryTime(entryTime);
                history.setExitTime(exitTime);
                history.setFee(fee);

                out.setSpotId(spotId);
                out.setVehicle(vehicle);
                out.setParkingHistory(history);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Lỗi lấy thông tin xe ra: " + e.getMessage());
        }
        return out;
    }

    @Override
    public ParkingSlotDTO getVehicleInfo(String plateNumber, DisplayMode mode) throws RemoteException {
        ParkingSlotDTO slotDTO = new ParkingSlotDTO();
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT v.plate_number, v.brand, v.owner_name, v.owner_phone, v.vehicle_type, ")
                .append("r.renter_id, r.spot_id, r.start_date, r.end_date, r.monthly_rate, ")
                .append("rs.day_of_week, rs.start_time, rs.end_time ")
                .append("FROM Vehicle v ")
                .append("LEFT JOIN renter r ON v.vehicle_id = r.vehicle_id ")
                .append("AND r.status = 'ACTIVE' ")
                .append("AND CURDATE() <= r.end_date ")
                .append("LEFT JOIN RenterSchedule rs ON r.renter_id = rs.renter_id ");

        if (mode == DisplayMode.DASHBOARD) {
            sql.append("AND rs.day_of_week = UPPER(DATE_FORMAT(NOW(), '%a')) ");
        }

        sql.append("WHERE v.plate_number = ?");

        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setString(1, plateNumber);
            ResultSet rs = ps.executeQuery();
            boolean isFirstRow = true;

            while (rs.next()) {
                if (isFirstRow) {
                    // 1. Luôn lấy thông tin xe (để hiện tên/hiệu xe lên Dashboard)
                    VehicleDTO v = new VehicleDTO();
                    v.setPlateNumber(rs.getString("plate_number"));
                    v.setOwner(rs.getString("owner_name"));
                    v.setBrand(rs.getString("brand"));
                    v.setVehicleType(rs.getString("vehicle_type"));
                    slotDTO.setVehicle(v);

                    int rentId = rs.getInt("renter_id");
                    if (!rs.wasNull()) {
                        // Lấy giờ bắt đầu và kết thúc từ DB
                        java.sql.Time sqlStart = rs.getTime("start_time");
                        java.sql.Time sqlEnd = rs.getTime("end_time");
                        LocalTime now = LocalTime.now();
                        boolean isTimeValid = true;

                        if (mode == DisplayMode.DASHBOARD && sqlStart != null && sqlEnd != null) {
                            LocalTime startTime = sqlStart.toLocalTime();
                            LocalTime endTime = sqlEnd.toLocalTime();

                            // Logic: Sớm hơn không quá 30 phút HOẶC đang trong giờ đỗ
                            // (now >= startTime - 30p) AND (now <= endTime)
                            LocalTime earlyGraceTime = startTime.minusMinutes(30);

                            isTimeValid = (now.isAfter(earlyGraceTime) || now.equals(earlyGraceTime))
                                    && now.isBefore(endTime);
                        }

                        // 2. Chỉ điền thông tin Rent và SpotID nếu đúng khung giờ hoặc không phải mode Dashboard
                        if (isTimeValid) {
                            RentDTO rent = new RentDTO();
                            rent.setRentID(rentId);
                            rent.setMonthlyRate(rs.getDouble("monthly_rate"));
                            slotDTO.setSpotId(rs.getString("spot_id"));
                            slotDTO.setRentID(rentId);

                            if (rs.getDate("start_date") != null) rent.setStartDate(rs.getDate("start_date").toLocalDate());
                            if (rs.getDate("end_date") != null) rent.setEndDate(rs.getDate("end_date").toLocalDate());

                            slotDTO.setCurrentRent(rent);
                        } else {
                            slotDTO.setCurrentRent(null);
                            slotDTO.setSpotId(null);
                        }
                    }
                    isFirstRow = false;
                }


                if (slotDTO.getRentID() != null && rs.getString("day_of_week") != null) {
                    ScheduleDTO s = new ScheduleDTO();
                    s.setDayOfWeek(rs.getString("day_of_week"));
                    s.setStartTime(rs.getTime("start_time").toLocalTime());
                    s.setEndTime(rs.getTime("end_time").toLocalTime());

                    slotDTO.getCurrentRent().getSchedules().add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return slotDTO.getVehicle() != null ? slotDTO : null;
    }

    @Override
    public boolean takeVehicleOut(ParkingSlotDTO slot) throws RemoteException {
        int vehicleId = slot.getVehicle().getVehicleId();
        if (slot == null || slot.getParkingHistory() == null) return false;

        int transactionId = slot.getParkingHistory().getTransactionId();
        String plateNumber = slot.getVehicle().getPlateNumber();
        double fee = slot.getParkingHistory().getFee();
        String spotId = slot.getSpotId();

        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String updateHistory = "UPDATE ParkingHistory SET exit_time = NOW(), fee = ?, status = 'COMPLETED' " +
                        "WHERE transaction_id = ? AND status = 'ACTIVE'";
                try (PreparedStatement ps = conn.prepareStatement(updateHistory)) {
                    ps.setDouble(1, fee);
                    ps.setInt(2, transactionId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                Integer renterId = null;
                String findRenter = "SELECT renter_id FROM renter WHERE vehicle_id = ? AND status = 'ACTIVE' LIMIT 1";
                try (PreparedStatement psR = conn.prepareStatement(findRenter)) {
                    psR.setInt(1, vehicleId);
                    ResultSet rsR = psR.executeQuery();
                    if (rsR.next()) renterId = rsR.getInt("renter_id");
                }

                String paymentType = (fee > 0) ? "VISITOR" : "RENTAL";
                String paySql = "INSERT INTO Payment(transaction_id, renter_id, amount, payment_type, payment_method) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement psPay = conn.prepareStatement(paySql)) {
                    psPay.setInt(1, transactionId);
                    if (renterId != null) psPay.setInt(2, renterId); else psPay.setNull(2, java.sql.Types.INTEGER);
                    psPay.setDouble(3, fee);
                    psPay.setString(4, paymentType);
                    psPay.setString(5, "CASH");
                    psPay.executeUpdate();
                }
                String updateSlot = "UPDATE parkingslot SET status = 'FREE' WHERE spot_id = ?";
                try (PreparedStatement psSlot = conn.prepareStatement(updateSlot)) {
                    psSlot.setString(1, spotId);
                    psSlot.executeUpdate();
                }

                conn.commit();


                LocalDateTime exitTime = LocalDateTime.now();

                ParkingOutEvent outEvent = new ParkingOutEvent(
                        spotId,
                        plateNumber,
                        transactionId,
                        exitTime,
                        fee,
                        this.serverName
                );

                broadcastVehicleOut(outEvent);
                notifyClients(spotId, "FREE");
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Lỗi: " + e.getMessage());
        }
    }
    @Override
    public void takeVehicleInFromSync(ParkingInEvent event) throws RemoteException {
        System.out.println("[SYNC-IN] Nhận xe vào từ server " + event.getSourceServer());
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            int dbVersion;
            String currentStatus;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT version, status FROM parkingslot WHERE spot_id = ? FOR UPDATE")) {

                ps.setString(1, event.getSpotId());
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    conn.rollback();
                    return;
                }
                dbVersion = rs.getInt("version");
                currentStatus = rs.getString("status");
            }
            if (event.getVersion() <= dbVersion) {
                conn.rollback();
                return;
            }
            VehicleDTO v = new VehicleDTO();
            v.setPlateNumber(event.getPlateNumber());
            v.setVehicleType(event.getVehicleType());
            v.setOwner(event.getOwnerName());
            v.setBrand(event.getBrand());

            int vehicleId = getOrCreateVehicle(conn, v);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE parkingslot SET status='OCCUPIED', version=? WHERE spot_id=?")) {

                ps.setInt(1, event.getVersion());
                ps.setString(2, event.getSpotId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ParkingHistory (spot_id, vehicle_id, entry_time, status) VALUES (?, ?, ?, 'ACTIVE')")) {

                ps.setString(1, event.getSpotId());
                ps.setInt(2, vehicleId);
                ps.setTimestamp(3, Timestamp.valueOf(event.getEntryTime()));
                ps.executeUpdate();
            }

            conn.commit();
            notifyClients(event.getSpotId(), "OCCUPIED");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void takeVehicleOutFromSync(ParkingOutEvent slot) throws RemoteException {
        try (Connection conn = DBManager.getConnection()) {

            conn.setAutoCommit(false);

            String spotId = slot.getSpotId();
            String areaType = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT area_type FROM parkingslot WHERE spot_id = ?")) {
                ps.setString(1, spotId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    areaType = rs.getString("area_type");
                }
            }

            // 3. Kiểm tra History có tồn tại không (vì SYNC IN có thể chưa tới)
            boolean historyExists = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT transaction_id FROM parkinghistory WHERE transaction_id = ?")) {
                ps.setInt(1, slot.getTransactionId());
                ResultSet rs = ps.executeQuery();
                historyExists = rs.next();
            }

            // 4. Nếu history chưa có → tạo bản ghi tối thiểu để giữ integrity
            if (!historyExists) {
                System.out.println("SYNC OUT: Chưa có ParkingHistory → Tạo bản ghi placeholder");

                // cần vehicle_id → query từ bảng Vehicle bằng plateNumber
                Integer vehicleId = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT vehicle_id FROM vehicle WHERE plate_number = ?")) {
                    ps.setString(1, slot.getPlateNumber());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        vehicleId = rs.getInt("vehicle_id");
                    } else {
                        System.out.println("SYNC OUT: Chưa có Vehicle → Tạo mới");

                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO vehicle(plate_number, vehicle_type) VALUES(?, 'CAR')",
                                Statement.RETURN_GENERATED_KEYS)) {
                            ins.setString(1, slot.getPlateNumber());
                            ins.executeUpdate();
                            ResultSet gen = ins.getGeneratedKeys();
                            if (gen.next()) {
                                vehicleId = gen.getInt(1);
                            }
                        }
                    }
                }

                // Tạo ParkingHistory placeholder
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO parkinghistory(transaction_id, spot_id, vehicle_id, entry_time, status, fee) " +
                                "VALUES (?, ?, ?, ?, 'ACTIVE', 0)")) {
                    ps.setInt(1, slot.getTransactionId());
                    ps.setString(2, spotId);
                    ps.setInt(3, vehicleId);
                    ps.setObject(4, slot.getExitTime()); // tạm dùng exitTime làm entry_time
                    ps.executeUpdate();
                }
            }

            // 5. Update ParkingHistory thành COMPLETED
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE parkinghistory SET exit_time=?, fee=?, status='COMPLETED' " +
                            "WHERE transaction_id=?")) {
                ps.setObject(1, slot.getExitTime());
                ps.setDouble(2, slot.getFee());
                ps.setInt(3, slot.getTransactionId());
                ps.executeUpdate();
            }

            // 6. Giải phóng Slot
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE parkingslot SET status='FREE' WHERE spot_id=?")) {
                ps.setString(1, spotId);
                ps.executeUpdate();
            }

            // 7. Insert Payment nếu chưa có
            boolean paymentExists = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT payment_id FROM payment WHERE transaction_id=?")) {
                ps.setInt(1, slot.getTransactionId());
                ResultSet rs = ps.executeQuery();
                paymentExists = rs.next();
            }

            if (!paymentExists) {
                System.out.println("SYNC OUT: Chưa có Payment → INSERT");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO payment(transaction_id, amount, payment_type, payment_method) " +
                                "VALUES (?, ?, 'VISITOR', 'TRANSFER')")) {
                    ps.setInt(1, slot.getTransactionId());
                    ps.setDouble(2, slot.getFee());
                    ps.executeUpdate();
                }
            } else {
                System.out.println("SYNC OUT: Payment đã tồn tại → Bỏ qua");
            }

            conn.commit();

            // 8. Cập nhật UI Clients
            SlotStatusDTO dto = new SlotStatusDTO(spotId, "FREE", areaType);

            for (ClientCallback client : clients) {
                try {
                    client.onSlotUpdated(dto);
                } catch (Exception e) {
                    System.out.println("Client lỗi callback OUT SYNC: " + e.getMessage());
                }
            }

            System.out.println("SYNC OUT thành công → Xe: " + slot.getPlateNumber());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public RentEvent calculateRentPrice(RentEvent event) throws RemoteException {
        String spotId = event.getPlace();
        String vehicleType = event.getVehicleType();
        String areaType = event.getAreaStyle();
        LocalDate from = event.getFromDate();
        LocalDate to = event.getToDate();


        List<DayRent> days = event.getDays();
        try (Connection conn = DBManager.getConnection()) {

            double firstAmount = 0;
            double totalAmount = 0;
            String description = "";
            String detail = "";

            if (days != null && !days.isEmpty()) {
                double hourlyRate = getCurrentHourlyRate(conn, areaType, vehicleType);

                long totalDays = ChronoUnit.DAYS.between(from, to) + 1;
                double totalMonths = Math.ceil(totalDays / 30.0);

                double totalHours = 0;
                LocalDate cursor = from;

                while (!cursor.isAfter(to)) {

                    DayOfWeek dow = cursor.getDayOfWeek();

                    for (DayRent d : days) {
                        DayOfWeek selected = DayOfWeek.valueOf(d.getDayOfWeek().toUpperCase());

                        if (selected == dow) {
                            long minutes = ChronoUnit.MINUTES.between(d.getStart(), d.getEnd());
                            if (minutes <= 0) minutes += 24 * 60;

                            totalHours += minutes / 60.0;
                        }
                    }
                    cursor = cursor.plusDays(1);
                }
                firstAmount = totalHours * hourlyRate;
                totalAmount = calculationAmountSales(totalMonths, firstAmount);
            }

            return new RentEvent(roundToThousand(totalAmount));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RentResult acceptRentValue(RentEvent event) throws RemoteException {
        try(Connection conn = DBManager.getConnection();) {
            conn.setAutoCommit(false);

            try {
                int vehicleId = getOrCreateVehicle(conn, event);

                int renterId = insertRenter(conn, event, vehicleId);

                insertRenterSchedule(conn, renterId, event.getDays());

                conn.commit();
                event.setSourceServer(this.serverName);

                broadcastVehicleRent(event);
                return new RentResult(true, renterId, "Rent created successfully");
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return new RentResult(false, -1, e.getMessage());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getRentalSpotOnDayWithSession(LocalDate date, String session) throws RemoteException {
        List<String> spotStatus = new ArrayList<>();
        String dayOfWeek = date.getDayOfWeek().name().substring(0, 3);
        String sql = """
        SELECT ps.spot_id, rs.start_time, rs.end_time
        FROM renter r
        JOIN RenterSchedule rs ON r.renter_id = rs.renter_id
        JOIN parkingslot ps ON r.spot_id = ps.spot_id
        WHERE ? BETWEEN r.start_date AND r.end_date
          AND r.status = 'ACTIVE'
          AND rs.day_of_week = ?
        """;

        try(Connection conn = DBManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, dayOfWeek);

            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String spotId = rs.getString("spot_id");
                    LocalTime startTime = rs.getTime("start_time").toLocalTime();

                    String actualSession;
                    if (startTime.isBefore(LocalTime.of(12, 0))) {
                        actualSession = "MORNING";
                    } else if (startTime.isBefore(LocalTime.of(18, 0))) {
                        actualSession = "AFTERNOON";
                    } else {
                        actualSession = "EVENING";
                    }

                    if (session.equals(actualSession)) {
                        spotStatus.add(spotId + ":" + actualSession);
                        System.out.println(spotId + ":" + actualSession);
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return spotStatus;
    }
    @Override
    public void getVehicleRentFromSync(RentEvent slot) throws RemoteException {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Tận dụng các hàm private ông đã viết để lưu vào DB địa phương
                int vehicleId = getOrCreateVehicle(conn, slot);
                int renterId = insertRenter(conn, slot, vehicleId);
                insertRenterSchedule(conn, renterId, slot.getDays());

                conn.commit();
                System.out.println("[RECEIVE-SYNC] Lưu thành công ô: " + slot.getPlace());

                // 2. QUAN TRỌNG: Cập nhật giao diện Dashboard của server hiện tại
                // Để các máy Client đang kết nối tới Server B cũng thấy ô đó chuyển sang màu Cam
                new Thread(() -> {
                    for (ClientCallback client : clients) {
                        try {
                            client.onRentAdded(slot.getPlace(), slot.getDays());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double getCurrentHourlyRate(Connection conn, String areaType, String vehicleType) throws SQLException {
        String sql = "SELECT hourly_rate FROM SlotPricing WHERE area_type = ? AND vehicle_type = ? " +
                "AND effective_from <= NOW() AND (effective_to IS NULL OR effective_to >= NOW()) " +
                "ORDER BY effective_from DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, areaType);
            ps.setString(2, vehicleType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    private double getCurrentDailyRate(Connection conn, String areaType, String vehicleType) throws SQLException {
        String sql = "SELECT daily_rate FROM SlotPricing WHERE area_type = ? AND vehicle_type = ? " +
                "AND effective_from <= NOW() AND (effective_to IS NULL OR effective_to >= NOW()) " +
                "ORDER BY effective_from DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, areaType);
            ps.setString(2, vehicleType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    private double calculationAmountSales(double months, double totalAmount) {

        if (months < 3) {
            return totalAmount;
        } else if (months < 6) {
            return totalAmount * 0.94;
        } else if (months < 12) {
            return totalAmount * 0.86;
        } else if (months == 12.0) {
            return totalAmount * 0.80;
        } else {
            double extraMonths = months - 12;
            double baseMonthly = totalAmount / months;
            double amount12 = (baseMonthly * 12) * 0.80;
            double amountExtra = (baseMonthly * 0.97) * extraMonths;

            return amount12 + amountExtra;
        }
    }
    private double roundToThousand(double value) {
        return Math.round(value / 1000.0) * 1000;
    }

    private int getOrCreateVehicle(Connection conn, RentEvent e) throws SQLException {
        String plate = e.getPlate();
        if (plate == null || plate.trim().isEmpty()) {
            throw new SQLException("Plate number is empty");
        }
        plate = plate.trim();

        String checkSql = "SELECT vehicle_id, owner_name, owner_phone, brand FROM Vehicle WHERE plate_number = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, plate);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("vehicle_id");
                    
                    String old_name = rs.getString("owner_name"); 
                    String old_phone = rs.getString("owner_phone");
                    String brand = rs.getString("brand");

                    if(old_name == null || old_phone == null || brand == null) {
                      String updateSql = "UPDATE Vehicle SET owner_name = ?, owner_phone = ?, brand = ? WHERE vehicle_id = ?";
                              try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                  updatePs.setString(1, e.getOwner());
                                  updatePs.setString(2, e.getPhone());
                                  updatePs.setString(3, e.getBrand());
                                  updatePs.setInt(4, existingId);
                                  updatePs.executeUpdate();
                              }
                    }
                    return existingId;
                }
            }
        }
        String insertSql = "INSERT INTO Vehicle (plate_number, owner_name, owner_phone, vehicle_type, brand) VALUES (?,?,?,?,?)";
        try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertPs.setString(1, plate);
            insertPs.setString(2, e.getOwner());
            insertPs.setString(3, e.getPhone());
            insertPs.setString(4, e.getVehicleType());
            insertPs.setString(5, e.getBrand());

            insertPs.executeUpdate();

            try (ResultSet keys = insertPs.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                } else {
                    return selectVehicleIdByPlate(conn, plate);
                }
            }
        } catch (SQLException ex) {
            String sqlState = ex.getSQLState();
            int errorCode = ex instanceof java.sql.SQLIntegrityConstraintViolationException ? ((java.sql.SQLIntegrityConstraintViolationException) ex).getErrorCode() : -1;
            if (errorCode == 1062 || (sqlState != null && sqlState.startsWith("23"))) {
                return selectVehicleIdByPlate(conn, plate);
            }
            throw ex;
        }
    }
    private int selectVehicleIdByPlate(Connection conn, String plate) throws SQLException {
        String q = "SELECT vehicle_id FROM Vehicle WHERE plate_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to obtain vehicle_id for plate: " + plate);
    }


    private int insertRenter(Connection conn, RentEvent e, int vehicleId) throws SQLException {
        String sql = """
        INSERT INTO renter (vehicle_id, spot_id, rent_pricing_id, start_date, end_date, 
        monthly_rate, deposit)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """;

        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, vehicleId);
        ps.setString(2, e.getPlace());
        if (e.getDays() != null) {
            ps.setObject(3, null);
        }
        ps.setDate(4, Date.valueOf(e.getFromDate()));
        ps.setDate(5, Date.valueOf(e.getToDate()));
        ps.setDouble(6, e.getTotalAmount());
        ps.setDouble(7, 0);               // không có đặt cọc

        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        keys.next();
        return keys.getInt(1);
    }

    private void insertRenterSchedule(Connection conn, int renterId, List<DayRent> days) throws SQLException {
        String sql = "INSERT INTO RenterSchedule (renter_id, day_of_week, start_time, end_time) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DayRent d : days) {
                ps.setInt(1, renterId);
                ps.setString(2, mapDay(d.getDayOfWeek()));
                ps.setTime(3, Time.valueOf(d.getStart()));
                ps.setTime(4, Time.valueOf(d.getEnd()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    private String mapDay(String day) {
        return switch (day.toUpperCase()) {
            case "MONDAY", "MON" -> "MON";
            case "TUESDAY", "TUE" -> "TUE";
            case "WEDNESDAY", "WED" -> "WED";
            case "THURSDAY", "THU" -> "THU";
            case "FRIDAY", "FRI" -> "FRI";
            case "SATURDAY", "SAT" -> "SAT";
            case "SUNDAY", "SUN" -> "SUN";
            default -> throw new IllegalArgumentException("Invalid day: " + day);
        };
    }

}
