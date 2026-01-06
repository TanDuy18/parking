-- ========================================
-- 1. PARKINGSLOT - Chỗ đỗ xe
-- ========================================
CREATE TABLE IF NOT EXISTS parkingslot (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL UNIQUE,
    floor INT DEFAULT 1,
    zone VARCHAR(10) DEFAULT '',
    area_type ENUM('STANDARD', 'PREMIUM') DEFAULT 'STANDARD',  -- ✅ NEW: Phân loại vị trí
    status ENUM('FREE', 'RESERVED', 'OCCUPIED', 'RENTED') DEFAULT 'FREE',
    row_index INT DEFAULT 0,
    col_index INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT not null,
    INDEX idx_status_type (status, area_type)
);

-- ========================================
-- 2. SLOTPRICING - Bảng giá theo spot
-- ========================================
CREATE TABLE IF NOT EXISTS SlotPricing (
                                           pricing_id INT PRIMARY KEY AUTO_INCREMENT,
                                           area_type ENUM('STANDARD','PREMIUM') NOT NULL,
    vehicle_type ENUM('CAR','MOTORBIKE','TRUCK') NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL,
    daily_rate DECIMAL(10,2) NOT NULL,
    effective_from DATETIME DEFAULT CURRENT_TIMESTAMP,
    effective_to DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_area_vehicle_from (area_type, vehicle_type, effective_from),
    INDEX idx_area_type (area_type, vehicle_type)
    );

CREATE TABLE IF NOT EXISTS RentPricing (
                                           pricing_id INT PRIMARY KEY AUTO_INCREMENT,
                                           area_type ENUM('STANDARD','PREMIUM') NOT NULL,
    vehicle_type ENUM('CAR','MOTORBIKE','TRUCK') NOT NULL,
    duration_months INT NOT NULL DEFAULT 1,
    price DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_rentpkg (area_type, vehicle_type, duration_months)
    );

-- ========================================
-- 3. VEHICLE - Thông tin xe
-- ========================================
CREATE TABLE IF NOT EXISTS Vehicle (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    owner_name VARCHAR(30),
    owner_phone VARCHAR(30),
    vehicle_type ENUM('CAR', 'MOTORBIKE', 'TRUCK', 'BICYCLE') DEFAULT 'CAR',
    brand VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plate (plate_number)
);

-- ========================================
-- 4. PARKINHISTORY - Lịch sử đỗ xe tạm
-- ========================================
CREATE TABLE IF NOT EXISTS ParkingHistory (
                                              transaction_id INT PRIMARY KEY AUTO_INCREMENT,
                                              spot_id VARCHAR(25) NOT NULL,
    vehicle_id INT NOT NULL,
    entry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time DATETIME NULL,
    status ENUM('ACTIVE','COMPLETED','CANCELLED') DEFAULT 'ACTIVE',
    pricing_id INT NULL,            -- <-- lưu pricing tại thời điểm vào
    fee DECIMAL(10,2) DEFAULT 0,
    paid BOOLEAN DEFAULT FALSE,     -- flag nhanh
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id),
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    FOREIGN KEY (pricing_id) REFERENCES SlotPricing(pricing_id),
    INDEX idx_active_spot (status, spot_id),
    INDEX idx_vehicle_time (vehicle_id, entry_time),
    INDEX idx_dates (entry_time, exit_time),
    INDEX idx_pricing (pricing_id)
    );

-- ========================================
-- 5. RENTER - Hợp đồng thuê dài hạn
-- ========================================
CREATE TABLE IF NOT EXISTS renter (
                                      renter_id INT PRIMARY KEY AUTO_INCREMENT,
                                      vehicle_id INT NOT NULL,
                                      spot_id VARCHAR(25) NOT NULL,
    rent_pricing_id INT NULL,   -- <-- liên kết gói thuê
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('ACTIVE','EXPIRED','CANCELLED') DEFAULT 'ACTIVE',
    monthly_rate DECIMAL(10,2) NOT NULL,
    deposit DECIMAL(10,2) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id),
    FOREIGN KEY (rent_pricing_id) REFERENCES RentPricing(pricing_id),
    UNIQUE KEY uniq_rent_spot (spot_id, start_date, end_date),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_vehicle (vehicle_id)
    );

-- ========================================
-- 6. RENTERSCHEDULE - Lịch thuê từng ngày
-- ========================================
CREATE TABLE IF NOT EXISTS RenterSchedule (
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    renter_id INT NOT NULL,
    day_of_week ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
    start_time TIME ,
    end_time TIME,
    FOREIGN KEY (renter_id) REFERENCES renter(renter_id) ON DELETE CASCADE,
    UNIQUE KEY uniq_day_time (renter_id, day_of_week)
);

-- ========================================
-- 7. PAYMENT - Thanh toán
-- ========================================
CREATE TABLE IF NOT EXISTS Payment (
                                       payment_id INT PRIMARY KEY AUTO_INCREMENT,
                                       transaction_id INT NULL,
                                       renter_id INT NULL,
                                       amount DECIMAL(10,2) NOT NULL,
    payment_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_type ENUM('VISITOR','RENTAL') NOT NULL,
    payment_method ENUM('CASH','CARD','TRANSFER') DEFAULT 'CASH',
    reference VARCHAR(100) DEFAULT NULL,
    created_by VARCHAR(100) DEFAULT NULL,
    FOREIGN KEY (transaction_id) REFERENCES ParkingHistory(transaction_id),
    FOREIGN KEY (renter_id) REFERENCES renter(renter_id),
    CONSTRAINT chk_payment_source CHECK (
(payment_type = 'VISITOR' AND transaction_id IS NOT NULL) OR
(payment_type = 'RENTAL' AND (renter_id IS NOT NULL OR transaction_id IS NOT NULL))
    ),
    INDEX idx_type_time (payment_type, payment_time),
    INDEX idx_transaction (transaction_id),
    INDEX idx_renter (renter_id)
    );

-- ========================================
-- INSERT DỮ LIỆU MẪU
-- ========================================
-- Zone A (STANDARD) – 20 slots
INSERT INTO parkingslot (spot_id, floor, zone, area_type, status, row_index, col_index, version)
VALUES
    ('A1-01', 1, 'A', 'STANDARD', 'FREE', 0, 0, 1),
    ('A1-02', 1, 'A', 'STANDARD', 'FREE', 0, 1, 1),
    ('A1-03', 1, 'A', 'STANDARD', 'FREE', 0, 2, 1),
    ('A1-04', 1, 'A', 'STANDARD', 'FREE', 0, 3, 1),
    ('A1-05', 1, 'A', 'STANDARD', 'FREE', 0, 4, 1),

    ('A1-06', 1, 'A', 'STANDARD', 'FREE', 1, 0, 1),
    ('A1-07', 1, 'A', 'STANDARD', 'FREE', 1, 1, 1),
    ('A1-08', 1, 'A', 'STANDARD', 'FREE', 1, 2, 1),
    ('A1-09', 1, 'A', 'STANDARD', 'FREE', 1, 3, 1),
    ('A1-10', 1, 'A', 'STANDARD', 'FREE', 1, 4, 1);


-- Zone B (PREMIUM) – 10 slots
INSERT INTO parkingslot (spot_id, floor, zone, area_type, status, row_index, col_index, version)
VALUES
    ('B1-01', 1, 'B', 'PREMIUM', 'FREE', 0, 6, 1),
    ('B1-02', 1, 'B', 'PREMIUM', 'FREE', 0, 7, 1),
    ('B1-03', 1, 'B', 'PREMIUM', 'FREE', 0, 8, 1),
    ('B1-04', 1, 'B', 'PREMIUM', 'FREE', 0, 9, 1),
    ('B1-05', 1, 'B', 'PREMIUM', 'FREE', 0, 10, 1),

    ('B1-06', 1, 'B', 'PREMIUM', 'FREE', 1, 6, 1),
    ('B1-07', 1, 'B', 'PREMIUM', 'FREE', 1, 7, 1),
    ('B1-08', 1, 'B', 'PREMIUM', 'FREE', 1, 8, 1),
    ('B1-09', 1, 'B', 'PREMIUM', 'FREE', 1, 9, 1),
    ('B1-10', 1, 'B', 'PREMIUM', 'FREE', 1, 10, 1);


-- Zone C (STANDARD) – 10 slots
INSERT INTO parkingslot (spot_id, floor, zone, area_type, status, row_index, col_index, version)
VALUES
    ('C1-01', 1, 'C', 'STANDARD', 'FREE', 3, 0, 1),
    ('C1-02', 1, 'C', 'STANDARD', 'FREE', 3, 1, 1),
    ('C1-03', 1, 'C', 'STANDARD', 'FREE', 3, 2, 1),
    ('C1-04', 1, 'C', 'STANDARD', 'FREE', 3, 3, 1),
    ('C1-05', 1, 'C', 'STANDARD', 'FREE', 3, 4, 1),

    ('C1-06', 1, 'C', 'STANDARD', 'FREE', 4, 0, 1),
    ('C1-07', 1, 'C', 'STANDARD', 'FREE', 4, 1, 1),
    ('C1-08', 1, 'C', 'STANDARD', 'FREE', 4, 2, 1),
    ('C1-09', 1, 'C', 'STANDARD', 'FREE', 4, 3, 1),
    ('C1-10', 1, 'C', 'STANDARD', 'FREE', 4, 4, 1);


-- Zone D (STANDARD) – 10 slots
INSERT INTO parkingslot (spot_id, floor, zone, area_type, status, row_index, col_index, version)
VALUES
    ('D1-01', 1, 'D', 'STANDARD', 'FREE', 3, 6, 1),
    ('D1-02', 1, 'D', 'STANDARD', 'FREE', 3, 7, 1),
    ('D1-03', 1, 'D', 'STANDARD', 'FREE', 3, 8, 1),
    ('D1-04', 1, 'D', 'STANDARD', 'FREE', 3, 9, 1),
    ('D1-05', 1, 'D', 'STANDARD', 'FREE', 3, 10, 1),

    ('D1-06', 1, 'D', 'STANDARD', 'FREE', 4, 6, 1),
    ('D1-07', 1, 'D', 'STANDARD', 'FREE', 4, 7, 1),
    ('D1-08', 1, 'D', 'STANDARD', 'FREE', 4, 8, 1),
    ('D1-09', 1, 'D', 'STANDARD', 'FREE', 4, 9, 1),
    ('D1-10', 1, 'D', 'STANDARD', 'FREE', 4, 10, 1);



INSERT INTO SlotPricing (area_type, vehicle_type, hourly_rate, daily_rate, effective_from) VALUES
-- ==========================
-- STANDARD ZONE
-- ==========================
('STANDARD', 'CAR',        15000, 120000, '2025-01-01 00:00:00'),
('STANDARD', 'MOTORBIKE',   5000,  40000, '2025-01-01 00:00:00'),
('STANDARD', 'TRUCK',      25000, 200000, '2025-01-01 00:00:00'),
('STANDARD', 'BICYCLE', 2000, 15000, '2025-01-01 00:00:00'),

-- ==========================
-- PREMIUM ZONE
-- ==========================
('PREMIUM', 'CAR',        20000, 150000, '2025-01-01 00:00:00'),
('PREMIUM', 'MOTORBIKE',   7000,  50000, '2025-01-01 00:00:00'),
('PREMIUM', 'TRUCK',      35000, 250000, '2025-01-01 00:00:00'),
('PREMIUM', 'BICYCLE', 3000, 20000, '2025-01-01 00:00:00');


INSERT INTO RentPricing (area_type, vehicle_type, duration_months, price, description) VALUES
-- ==========================
-- STANDARD - CAR
-- ==========================
('STANDARD', 'CAR', 1, 1500000, 'Thuê 1 tháng cho ô tô (Standard)'),
('STANDARD', 'CAR', 3, 4200000, 'Gói 3 tháng ô tô Standard (tiết kiệm 300k)'),
('STANDARD', 'CAR', 6, 7800000, 'Gói 6 tháng ô tô Standard (tiết kiệm 1.2 triệu)'),
('STANDARD', 'CAR', 12, 15000000, 'Gói năm ô tô Standard (tặng 2 tháng)'),


-- ==========================
-- STANDARD - MOTORBIKE
-- ==========================
('STANDARD', 'MOTORBIKE', 1, 120000, 'Thuê 1 tháng xe máy Standard'),
('STANDARD', 'MOTORBIKE', 3, 330000, 'Gói 3 tháng xe máy (giảm 30k)'),
('STANDARD', 'MOTORBIKE', 6, 600000, 'Gói 6 tháng xe máy (giảm 120k)'),
('STANDARD', 'MOTORBIKE', 12, 1100000, 'Gói năm xe máy (tặng 2 tháng)'),


-- ==========================
-- STANDARD - TRUCK
-- ==========================
('STANDARD', 'TRUCK', 1, 2500000, 'Thuê tháng xe tải Standard'),
('STANDARD', 'TRUCK', 3, 7200000, 'Gói quý xe tải (giảm 300k)'),
('STANDARD', 'TRUCK', 6, 13200000, 'Gói 6 tháng xe tải (giảm 1.2 triệu)'),
('STANDARD', 'TRUCK', 12, 25000000, 'Gói năm xe tải'),


-- ==========================
-- PREMIUM - CAR
-- ==========================
('PREMIUM', 'CAR', 1, 2000000, 'Thuê 1 tháng ô tô Premium (khu VIP)'),
('PREMIUM', 'CAR', 3, 5700000, 'Gói 3 tháng ô tô Premium (giảm 300k)'),
('PREMIUM', 'CAR', 6, 10800000, 'Gói 6 tháng ô tô Premium'),
('PREMIUM', 'CAR', 12, 21000000, 'Gói năm ô tô Premium (tặng 1.5 tháng)'),


-- ==========================
-- PREMIUM - MOTORBIKE
-- ==========================
('PREMIUM', 'MOTORBIKE', 1, 150000, 'Thuê tháng xe máy Premium'),
('PREMIUM', 'MOTORBIKE', 3, 420000, 'Gói 3 tháng xe máy Premium'),
('PREMIUM', 'MOTORBIKE', 6, 780000, 'Gói 6 tháng xe máy Premium'),
('PREMIUM', 'MOTORBIKE', 12, 1500000, 'Gói năm xe máy Premium'),


-- ==========================
-- PREMIUM - TRUCK
-- ==========================
('PREMIUM', 'TRUCK', 1, 3200000, 'Thuê tháng xe tải Premium'),
('PREMIUM', 'TRUCK', 3, 9000000, 'Gói 3 tháng xe tải Premium'),
('PREMIUM', 'TRUCK', 6, 17000000, 'Gói 6 tháng xe tải Premium'),
('PREMIUM', 'TRUCK', 12, 32000000, 'Gói năm xe tải Premium');
