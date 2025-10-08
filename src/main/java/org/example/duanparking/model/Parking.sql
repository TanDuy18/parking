CREATE TABLE users (
    username VARCHAR PRIMARY KEY,
    password_hash VARCHAR NOT NULL,
    full_name VARCHAR,
    phone VARCHAR,
    email VARCHAR
);

CREATE TABLE parking_lot (
    id VARCHAR PRIMARY KEY,
    name VARCHAR,
    location VARCHAR,
    capacity INT,
    available_spots INT
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    username VARCHAR REFERENCES users(username),
    lot_id VARCHAR REFERENCES parking_lot(id),
    reserved_at TIMESTAMP,
    expires_at TIMESTAMP,
    status VARCHAR CHECK (status IN ('ACTIVE','CANCELLED','USED','EXPIRED'))
);

CREATE TABLE bookings (history) (
  id UUID PRIMARY KEY,
  reservation_id UUID,
  event_type VARCHAR,
  payload JSONB,
  created_at TIMESTAMP
);
