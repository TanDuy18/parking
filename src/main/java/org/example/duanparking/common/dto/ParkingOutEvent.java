package org.example.duanparking.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ParkingOutEvent implements Serializable{
    private static final long serialVersionUID = 1L; 

    private final String spotId;
    private final String plateNumber; 
    private final int transactionId; 
    private final LocalDateTime exitTime; 
    private final double fee; 
    private final String sourceServer; 
    
    public ParkingOutEvent(String spotId, String plateNumber, int transactionId, LocalDateTime exitTime, double fee, String sourceServer) {
        this.spotId = spotId;
        this.plateNumber = plateNumber; 
        this.transactionId = transactionId; 
        this.exitTime = exitTime; 
        this.fee = fee; 
        this.sourceServer = sourceServer; 
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public double getFee() {
        return fee; 
    }
    public int getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }
    public String getSourceServer() {
        return sourceServer;
    }
    public String getSpotId() {
        return spotId;
    }
}
