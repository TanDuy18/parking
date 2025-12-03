package org.example.duanparking.common.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String plate;
    private String owner;
    private String phone;
    private String brand;
    private String place;
    private String vehicleType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<DayRent> days;
    private double totalAmount;
    private String sourceServer;

    public RentEvent() {}

    // Constructor
    public RentEvent(String plate, String owner, String phone, String brand,
                     String place, String vehicleType, LocalDate fromDate,
                     LocalDate toDate, List<DayRent> days, double totalAmount,
                     String sourceServer) {
        this.plate = plate;
        this.owner = owner;
        this.phone = phone;
        this.brand = brand;
        this.place = place;
        this.vehicleType = vehicleType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.days = days;
        this.totalAmount = totalAmount;
        this.sourceServer = sourceServer;
    }

    // Getters and Setters
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public List<DayRent> getDays() { return days; }
    public void setDays(List<DayRent> days) { this.days = days; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getSourceServer() { return sourceServer; }
    public void setSourceServer(String sourceServer) { this.sourceServer = sourceServer; }

    @Override
    public String toString() {
        return "RentEvent{" +
                "plate='" + plate + '\'' +
                ", place='" + place + '\'' +
                ", fromDate=" + fromDate +
                ", toDate=" + toDate +
                ", totalAmount=" + totalAmount +
                ", sourceServer='" + sourceServer + '\'' +
                '}';
    }
}