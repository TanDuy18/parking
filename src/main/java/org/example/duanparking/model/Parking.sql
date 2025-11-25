-- ========================================
-- 1. PARKINGSLOT - Chỗ đỗ xe
-- ========================================
CREATE TABLE IF NOT EXISTS parkingslot (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL UNIQUE,
    area_type ENUM('STANDARD', 'PREMIUM') DEFAULT 'STANDARD',  -- ✅ NEW: Phân loại vị trí
    status ENUM('FREE', 'RESERVED', 'OCCUPIED', 'RENTED') DEFAULT 'FREE',
    row_index INT DEFAULT 0,
    col_index INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_type (status, area_type)
);

-- ========================================
-- 2. SLOTPRICING - Bảng giá theo spot
-- ========================================
CREATE TABLE IF NOT EXISTS SlotPricing (
    pricing_id INT PRIMARY KEY AUTO_INCREMENT,
    area_type ENUM('STANDARD', 'PREMIUM') NOT NULL,
    vehicle_type ENUM('CAR', 'MOTORBIKE', 'TRUCK') NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL,
    daily_rate DECIMAL(10,2) NOT NULL,
    effective_from DATETIME DEFAULT CURRENT_TIMESTAMP,
    effective_to DATETIME NULL,
    UNIQUE(area_type, vehicle_type, effective_from),
    INDEX idx_area_type (area_type, vehicle_type)
);

CREATE TABLE IF NOT EXISTS RentPricing (
    pricing_id INT PRIMARY KEY AUTO_INCREMENT,
    area_type ENUM('STANDARD', 'PREMIUM') NOT NULL,
    vehicle_type ENUM('CAR', 'MOTORBIKE', 'TRUCK') NOT NULL,
    duration_months INT NOT NULL DEFAULT 1, -- 1: 1 tháng, 3: 3 tháng (Quý), 12: 1 năm
    price DECIMAL(10,2) NOT NULL,           -- Giá trọn gói
    description VARCHAR(255),               -- Ví dụ: "Gói tháng tiêu chuẩn", "Gói VIP năm"
    is_active BOOLEAN DEFAULT TRUE,         -- Để ẩn hiện gói cước
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(area_type, vehicle_type, duration_months) -- Đảm bảo không trùng gói
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
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'ACTIVE',
    fee DECIMAL(10,2) DEFAULT 0,
    paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id),
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    INDEX idx_active_spot (status, spot_id),
    INDEX idx_vehicle_time (vehicle_id, entry_time),
    INDEX idx_dates (entry_time, exit_time)
);

-- ========================================
-- 5. RENTER - Hợp đồng thuê dài hạn
-- ========================================
CREATE TABLE IF NOT EXISTS renter (
    renter_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id INT NOT NULL,
    spot_id VARCHAR(25) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED') DEFAULT 'ACTIVE',  -- ✅ NEW
    monthly_rate DECIMAL(10,2) NOT NULL,                             -- ✅ NEW
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id),
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
    start_time TIME DEFAULT '00:00:00',
    end_time TIME DEFAULT '23:59:59',
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
    payment_method ENUM('CASH','CARD','TRANSFER') DEFAULT 'CASH',  -- ✅ NEW
    FOREIGN KEY (transaction_id) REFERENCES ParkingHistory(transaction_id),
    FOREIGN KEY (renter_id) REFERENCES renter(renter_id),
    CONSTRAINT chk_payment_source CHECK (
        (payment_type = 'VISITOR' AND transaction_id IS NOT NULL AND renter_id IS NULL) OR
        (payment_type = 'RENTAL' AND renter_id IS NOT NULL AND transaction_id IS NULL)
    ),
    INDEX idx_type_time (payment_type, payment_time),
    INDEX idx_transaction (transaction_id),
    INDEX idx_renter (renter_id)
);

-- ========================================
-- INSERT DỮ LIỆU MẪU
-- ========================================
INSERT INTO parkingslot (spot_id, area_type, status, row_index, col_index) VALUES
-- Tầng 1: Khu vực STANDARD (A)
('1A', 'STANDARD', 'FREE', 0, 0),
('2A', 'STANDARD', 'FREE', 0, 1),
('3A', 'STANDARD', 'FREE', 0, 2),
('4A', 'STANDARD', 'FREE', 0, 3),
('5A', 'STANDARD', 'FREE', 0, 4),
('6A', 'STANDARD', 'FREE', 1, 0),
('7A', 'STANDARD', 'FREE', 1, 1),
('8A', 'STANDARD', 'FREE', 1, 2),
('9A', 'STANDARD', 'FREE', 1, 3),
('10A', 'STANDARD','FREE', 1, 4),

-- Tầng 2: Khu vực PREMIUM (B)
('1B', 'PREMIUM', 'FREE', 3, 0),
('2B', 'PREMIUM', 'FREE', 3, 1),
('3B', 'PREMIUM', 'FREE', 3, 2),
('4B', 'PREMIUM', 'FREE', 3, 3),
('5B', 'PREMIUM', 'FREE', 3, 4),
('6B', 'PREMIUM', 'FREE', 4, 0),
('7B', 'PREMIUM', 'FREE', 4, 1),
('8B', 'PREMIUM', 'FREE', 4, 2),
('9B', 'PREMIUM', 'FREE', 4, 3),
('10B', 'PREMIUM', 'FREE', 4, 4);

INSERT INTO SlotPricing (area_type, vehicle_type, hourly_rate, daily_rate, effective_from) VALUES
-- STANDARD
('STANDARD', 'CAR', 15000, 120000, '2025-11-17 08:00:00'),
('STANDARD', 'MOTORBIKE', 5000, 40000, '2025-11-17 08:00:00'),
('STANDARD', 'TRUCK', 25000, 200000, '2025-11-17 08:00:00'),

-- PREMIUM
('PREMIUM', 'CAR', 20000, 150000, '2025-11-17 08:00:00'),
('PREMIUM', 'MOTORBIKE', 7000, 50000, '2025-11-17 08:00:00'),
('PREMIUM', 'TRUCK', 35000, 250000, '2025-11-17 08:00:00');


INSERT INTO RentPricing (area_type, vehicle_type, duration_months, price, description) VALUES
-- Gói 1 tháng
('STANDARD', 'CAR', 1, 1500000, 'Thuê tháng ô tô tiêu chuẩn'),
('STANDARD', 'MOTORBIKE', 1, 120000, 'Thuê tháng xe máy tiêu chuẩn'),
('PREMIUM', 'CAR', 1, 2000000, 'Thuê tháng ô tô VIP'),

-- Gói 3 tháng (Có thể rẻ hơn chút nếu tính ra từng tháng)
('STANDARD', 'CAR', 3, 4200000, 'Combo Quý ô tô (Tiết kiệm 300k)');
