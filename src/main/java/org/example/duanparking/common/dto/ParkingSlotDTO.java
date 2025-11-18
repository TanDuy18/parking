package org.example.duanparking.common.dto;

import java.io.Serializable;

public class ParkingSlotDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int transaction_id;
    private String spotId;
    private String status;
    private String plateNumber;
    private String owner;
    private String ownerName;
    private String entryTime;
    private String exitTime;
    private double fee;
    private String areaType;
    private String brand;
    private int row, col;
    private String vehicleType;

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
    public void setAreaType(String type){
        this.areaType = type;
    }
    public String getAreaType(){
        return areaType;
    }

    public int getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(int transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getExitTime() {
        return exitTime;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ParkingSlotDTO() {}

    // Constructor đầy đủ
    public ParkingSlotDTO(String spotId, String status, String plateNumber,
                          String ownerName, String entryTime, double fee,
                          int row, int col, String area_type) {
        this.spotId = spotId;
        this.status = status;
        this.plateNumber = plateNumber;
        this.ownerName = ownerName;
        this.entryTime = entryTime;
        this.fee = fee;
        this.row = row;
        this.col = col;
        this.areaType = area_type;
    }
    public ParkingSlotDTO(int transaction_id, String owner, String brand, String plateNumber,String spotId, String vehicleType, String entryTime, String exitTime, double fee) {
        this.transaction_id = transaction_id;
        this.owner = owner;
        this.brand = brand;
        this.plateNumber = plateNumber;
        this.spotId = spotId;
        this.vehicleType = vehicleType;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.fee = fee;
    }
}
