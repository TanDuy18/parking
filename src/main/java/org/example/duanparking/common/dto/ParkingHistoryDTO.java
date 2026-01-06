package org.example.duanparking.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ParkingHistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private int transactionId;
    private String spotId;
    private int vehicleId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double fee;

    public ParkingHistoryDTO(int transactionId, String spotId, int vehicleId, LocalDateTime entryTime, LocalDateTime exitTime, double fee) {
        this.transactionId = transactionId;
        this.spotId = spotId;
        this.vehicleId = vehicleId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.fee = fee;
    }
    public ParkingHistoryDTO() {}

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }
}
