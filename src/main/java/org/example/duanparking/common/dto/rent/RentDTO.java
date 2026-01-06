package org.example.duanparking.common.dto.rent;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer rentID;
    private String spotId;
    private double monthlyRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<ScheduleDTO> schedules = new ArrayList<>();

    public List<ScheduleDTO> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ScheduleDTO> schedules) {
        this.schedules = schedules;
    }

    public void addSchedule(ScheduleDTO dto) {
        this.schedules.add(dto);
    }

    public Integer getRentID() {
        return rentID;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public void setRentID(Integer rentID) {
        this.rentID = rentID;
    }

    public double getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(double monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
