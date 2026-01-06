package org.example.duanparking.common.dto.rent; 

import java.io.Serializable;
import java.time.LocalTime;

public class ScheduleDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String renterName;
    private LocalTime startTime;
    private LocalTime endTime;

    public ScheduleDTO() {}

    public ScheduleDTO(String renterName, LocalTime startTime, LocalTime endTime) {
        this.renterName = renterName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters và Setters
    public String getRenterName() { return renterName; }
    public void setRenterName(String renterName) { this.renterName = renterName; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public boolean isOvernight() {
        return endTime != null && startTime != null && (endTime.isBefore(startTime) || endTime.equals(startTime));
    }
}
