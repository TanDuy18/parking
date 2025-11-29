package org.example.duanparking.common.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ParkingSlotDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String spotId;
    private String status;
    private int row, col;
    private String areaType;
    private VehicleDTO vehicle;
    private ParkingHistoryDTO history;

    public ParkingSlotDTO() {
        vehicle = new VehicleDTO();
        history = new ParkingHistoryDTO();
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public String getAreaType() {
        return areaType;
    }

    public void setAreaType(String areaType) {
        this.areaType = areaType;
    }

    public VehicleDTO getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleDTO vehicle) {
        this.vehicle = vehicle;
    }

    public ParkingHistoryDTO getHistory() {
        return history;
    }

    public void setHistory(ParkingHistoryDTO history) {
        this.history = history;
    }
}
