package org.example.duanparking.common.dto;

import java.io.Serializable;

public class ParkingSlotDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String spotId;
    private String status;
    private String plateNumber;
    private String ownerName;
    private String entryTime;
    private double fee;
    private int row, col;

    public String getSpotId() {return spotId;}
    public void setSpotId(String spotId) {this.spotId = spotId;}

    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}

    public String getPlateNumber() {return plateNumber;}
    public void setPlateNumber(String plateNumber) {this.plateNumber = plateNumber;}

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
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

    public ParkingSlotDTO() {}

    // Constructor đầy đủ
    public ParkingSlotDTO(String spotId, String status, String plateNumber,
                          String ownerName, String entryTime, double fee,
                          int row, int col, String parkingLotId) {
        this.spotId = spotId;
        this.status = status;
        this.plateNumber = plateNumber;
        this.ownerName = ownerName;
        this.entryTime = entryTime;
        this.fee = fee;
        this.row = row;
        this.col = col;
    }
}
