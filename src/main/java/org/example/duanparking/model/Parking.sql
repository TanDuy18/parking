/*
CREATE TABLE IF NOT EXISTS ParkingLot (
    lot_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    capacity INT DEFAULT 0,
    location POINT, -- Dùng MySQL Spatial nếu cần
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS parkingslot (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL,
    parking_lot_id INT NOT NULL DEFAULT 1,
    status ENUM('FREE', 'RESERVED', 'OCCUPIED') DEFAULT 'FREE',
    row_index INT DEFAULT 0,
    col_index INT DEFAULT 0,
    UNIQUE KEY uniq_spot_lot (spot_id, parking_lot_id),
    FOREIGN KEY (parking_lot_id) REFERENCES ParkingLot(lot_id),
    INDEX idx_status_lot (status, parking_lot_id)
);

CREATE TABLE IF NOT EXISTS ParkingHistory (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL,
    parking_lot_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    entry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time DATETIME NULL,
    status ENUM('ACTIVE', 'COMPLETED') DEFAULT 'ACTIVE',
    fee DECIMAL(10,2) DEFAULT 0,
    FOREIGN KEY (spot_id, parking_lot_id)
    REFERENCES parkingslot(spot_id, parking_lot_id),
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    FOREIGN KEY (parking_lot_id) REFERENCES ParkingLot(lot_id),
    INDEX idx_active_spot (status, spot_id, parking_lot_id),
    INDEX idx_vehicle_time (vehicle_id, entry_time)
    );
*/

CREATE TABLE IF NOT EXISTS parkingslot (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL,
    status ENUM('FREE', 'RESERVED', 'OCCUPIED') DEFAULT 'FREE',
    row_index INT DEFAULT 0,
    col_index INT DEFAULT 0,
    UNIQUE KEY uniq_spot_lot (spot_id),
    INDEX idx_status_lot (status, spot_id)
);

CREATE TABLE IF NOT EXISTS SlotPricing (
    pricing_id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL,
    vehicle_type ENUM('CAR', 'MOTORBIKE', 'TRUCK') DEFAULT 'CAR',
    hourly_rate DECIMAL(10,2) NOT NULL, -- Giá/giờ
    daily_rate DECIMAL(10,2) NOT NULL, -- Giá trần/ ngày
    effective_from DATETIME DEFAULT CURRENT_TIMESTAMP,
    effective_to DATETIME NULL,
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id),
    UNIQUE(spot_id, vehicle_type, effective_from)
    );

CREATE TABLE IF NOT EXISTS Vehicle (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    owner_name VARCHAR(100),
    phone_number VARCHAR(20),
    vehicle_type ENUM('CAR', 'MOTORBIKE', 'TRUCK', 'BICYCLE') DEFAULT 'CAR',
    brand VARCHAR(50),
    color VARCHAR(30),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plate (plate_number)
    );



-- Thêm dữ liệu mẫu (2 hàng x 5 cột)
INSERT INTO parkingslot (spot_id, status, row_index, col_index) VALUES
    ('1A', 'Free', 0, 0),('2A', 'Free', 0, 1), ('3A', 'Free', 0, 2), ('4A', 'Free', 0, 3), ('5A', 'Free', 0, 4),
    ('6A', 'Free', 1, 0), ('7A', 'Free', 1, 1), ('8A', 'Free', 1, 2), ('9A', 'Free', 1, 3), ('10A', 'Free', 1, 4)

    ,('1B', 'Free', 3, 0), ('2B', 'Free', 3, 1), ('3B', 'Free', 3, 2), ('4B', 'Free', 3, 3), ('5B', 'Free', 3, 4),
    ('6B', 'Free', 4, 0), ('7B', 'Free', 4, 1), ('8B', 'Free', 4, 2), ('9B', 'Free', 4, 3), ('10B', 'Free', 4, 4)
;

CREATE TABLE IF NOT EXISTS ParkingHistory (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) NOT NULL,
    vehicle_id INT NOT NULL,
    entry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time DATETIME NULL,
    status ENUM('ACTIVE', 'COMPLETED') DEFAULT 'ACTIVE',
    fee DECIMAL(10,2) DEFAULT 0,
    FOREIGN KEY (spot_id, parking_lot_id)
    REFERENCES parkingslot(spot_id, parking_lot_id),
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    FOREIGN KEY (parking_lot_id) REFERENCES ParkingLot(lot_id),
    INDEX idx_active_spot (status, spot_id, parking_lot_id),
    INDEX idx_vehicle_time (vehicle_id, entry_time)
    );
-- Cần làm lượt ra vào ở bảng entryRecoard để xem thử tính tiền


CREATE TABLE IF NOT EXISTS renter (
    renter_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id INT NOT NULL,
    spot_id VARCHAR(25) NOT NULL,
    parking_lot_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES Vehicle(vehicle_id),
    FOREIGN KEY (spot_id, parking_lot_id)
    REFERENCES parkingslot(spot_id, parking_lot_id),
    FOREIGN KEY (parking_lot_id) REFERENCES ParkingLot(lot_id),
    UNIQUE KEY uniq_rent_period (vehicle_id, spot_id, parking_lot_id, start_date),
    INDEX idx_dates (start_date, end_date)
);

CREATE TABLE IF NOT EXISTS RenterSchedule (
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    renter_id INT NOT NULL,
    day_of_week ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
    start_time TIME DEFAULT '00:00:00',
    end_time TIME DEFAULT '23:59:59',
    FOREIGN KEY (renter_id) REFERENCES renter(renter_id),
    UNIQUE KEY uniq_day_time (renter_id, day_of_week)
    );

CREATE TABLE IF NOT EXISTS Payment (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    transaction_id INT NULL,
    renter_id INT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_type ENUM('VISITOR','RENTAL') NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES ParkingHistory(transaction_id),
    FOREIGN KEY (renter_id) REFERENCES renter(renter_id),
    CONSTRAINT chk_payment_source CHECK (
(payment_type = 'VISITOR' AND transaction_id IS NOT NULL AND renter_id IS NULL) OR
(payment_type = 'RENTAL' AND renter_id IS NOT NULL AND transaction_id IS NULL)
    ),
    INDEX idx_type_time (payment_type, payment_time)
    );
