package org.example.duanparking.common;

import java.io.Serializable;
import java.time.Instant;

public class VehicleData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String plate;  // Biển số
    private long timestamp;  // Thời gian vào (ms)
    private String owner;  // "CAR", "MOTORBIKE", etc.

    public VehicleData(String plate, long timestamp, String vehicleType) {
        this.plate = plate;
        this.timestamp = timestamp;
        this.owner = vehicleType;
    }

    // Getters
    public String getPlate() { return plate; }
    public long getTimestamp() { return timestamp; }
    public String getOwner() { return owner; }

    @Override
    public String toString() {
        return "VehicleData{plate='" + plate + "', timestamp=" + Instant.ofEpochMilli(timestamp) + ", type='" + owner + "'}";
    }
}
