package org.example.duanparking.common.dto;

import org.example.duanparking.common.dto.rent.ScheduleDTO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingSlotDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String spotId;
    private String status;
    private int row, col;
    private String areaType;
    private int version;
    private VehicleDTO vehicle;
    private ParkingHistoryDTO parkingHistory;
    private List<ScheduleDTO> schedules = new ArrayList();
    private Integer rentID;
    private LocalTime startTime;
    private LocalTime endTime; 

    public ParkingSlotDTO() {}

    public List<ScheduleDTO> getSchedules() {
        return schedules; 
    }

    public void setSchedules(List<ScheduleDTO> schedules) {
        this.schedules = schedules; 
    }

    public void addSchedule(ScheduleDTO dto) {
        this.schedules.add(dto);
    }

    public LocalTime getStartTime() {
        return startTime; 
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime; 
    }
    public LocalTime getEndTime() {
        return endTime; 
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime; 
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public VehicleDTO getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleDTO vehicle) {
        this.vehicle = vehicle;
    }

    public ParkingHistoryDTO getParkingHistory() {
        return parkingHistory;
    }

    public void setParkingHistory(ParkingHistoryDTO parkingHistory) {
        this.parkingHistory = parkingHistory;
    }

    public Integer getRentID() {
        return rentID;
    }

    public void setRentID(Integer rentID) {
        this.rentID = rentID;
    }
}
