



CREATE TABLE IF NOT EXISTS parkingslot (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) UNIQUE,
    status ENUM('FREE', 'RESERVED','OCCUPIED') DEFAULT 'FREE',
    row_index INT DEFAULT 0,
    col_index INT DEFAULT 0,
    );



-- Thêm dữ liệu mẫu (2 hàng x 5 cột)
INSERT INTO parkingslot (spot_id, status, row_index, col_index) VALUES
    ('1A', 'Free', 0, 0)/*,*/('2A', 'Free', 0, 1), ('3A', 'Free', 0, 2), ('4A', 'Free', 0, 3), ('5A', 'Free', 0, 4),
    ('6A', 'Free', 1, 0), ('7A', 'Free', 1, 1), ('8A', 'Free', 1, 2), ('9A', 'Free', 1, 3), ('10A', 'Free', 1, 4)

    /*,*/('1B', 'Free', 3, 0), ('2B', 'Free', 3, 1), ('3B', 'Free', 3, 2), ('4B', 'Free', 3, 3), ('5B', 'Free', 3, 4),
    ('6B', 'Free', 4, 0), ('7B', 'Free', 4, 1), ('8B', 'Free', 4, 2), ('9B', 'Free', 4, 3), ('10B', 'Free', 4, 4)
;

CREATE TABLE IF NOT EXISTS ParkingTransaction(
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    spot_id VARCHAR(25) UNIQUE,
    plate_number VARCHAR(20) NOT NULL,  -- Biển số xe
    owner_name VARCHAR(100) NULL, -- Chủ xe có thể null
    entry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Thời gian vào
    exit_time DATETIME,
    status ENUM('ACTIVE','COMPLETED') DEFAULT 'ACTIVE',
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id)
)
-- Cần làm lượt ra vào ở bảng entryRecoard để xem thử tính tiền 


CREATE TABLE IF NOT EXISTS renter(
    renter_id int primary key AUTO_INCREMENT,
    plate_number varchar(20) NOT NULL UNIQUE,
    owner_name varchar(20) NOT NULL,
    phone_number varchar(20),
    spot_id varchar(25) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    FOREIGN KEY (spot_id) REFERENCES parkingslot(spot_id)
)

CREATE TABLE IF NOT EXISTS RenterSchedule(
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    renter_id INT NOT NULL,
    day_of_week ENUM('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
    start_time TIME DEFAULT '00:00:00',
    end_time TIME DEFAULT '23:59:59',
    FOREIGN KEY (renter_id) REFERENCES Renter(renter_id)
)

CREATE TABLE IF NOT EXISTS Payment (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    transaction_id INT,
    amount DECIMAL(10,2) NOT NULL,
    payment_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    payment_type ENUM('VISITOR','RENTAL') DEFAULT 'VISITOR',
    FOREIGN KEY (transaction_id) REFERENCES ParkingTransaction(transaction_id)
);
