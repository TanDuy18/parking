package org.example.duanparking.common.dto;

import java.io.Serializable;

public class VehicleDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private int vehicleId;
    private String plateNumber;
    private String ownerName;
    private String phoneNumber; 
    private String brand;
    private String vehicleType;

    public VehicleDTO() {}

    public VehicleDTO(int vehicleId, String plateNumber, String ownerName, String brand, String vehicleType) {
        this.vehicleId = vehicleId;
        this.plateNumber = plateNumber;
        this.ownerName = ownerName;
        this.brand = brand;
        this.vehicleType = vehicleType;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getOwner() {
        return ownerName;
    }

    public void setOwner(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPhone() {
        return phoneNumber;
    }

    public void setPhone(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}
