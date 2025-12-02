package org.example.duanparking.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ParkingInEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String spotId;  // Dang immutable ko thay đổi được
    private final String plateNumber;
    private final String vehicleType;
    private final LocalDateTime entryTime;
    private final String ownerName;
    private final String brand;
    private final int version;
    private final String sourceServer;

    public ParkingInEvent(String spotId, String plateNumber, String vehicleType,
                          LocalDateTime entryTime, String ownerName, String brand,
                          int version, String sourceServer) {
        this.spotId = spotId;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.entryTime = entryTime;
        this.ownerName = ownerName;
        this.brand = brand;
        this.version = version;
        this.sourceServer = sourceServer;
    }

    public String getSpotId() {
        return spotId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getBrand() {
        return brand;
    }

    public int getVersion() {
        return version;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    @Override
    public String toString() {
        return "ParkingInEvent{" +
                "spotId='" + spotId + '\'' +
                ", plateNumber='" + plateNumber + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", entryTime=" + entryTime +
                ", ownerName='" + ownerName + '\'' +
                ", brand='" + brand + '\'' +
                ", version=" + version +
                ", sourceServer='" + sourceServer + '\'' +
                '}';
    }
}
