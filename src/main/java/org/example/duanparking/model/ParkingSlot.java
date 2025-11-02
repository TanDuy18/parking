package org.example.duanparking.model;

import java.io.Serializable;

public class ParkingSlot implements Serializable {
    private static final long serialVersionUID = 1L; // Khuyến nghị

    private String spotId;
    private String status;
    private int rowIndex;
    private int columnIndex;

    public ParkingSlot(String spotId, String status, int rowIndex, int columnIndex) {
        this.spotId = spotId;
        this.status = status;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
    }

    // Getter / Setter
    public String getSpotId()   { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
    public int getColumnIndex() { return columnIndex; }
    public void setColumnIndex(int columnIndex) { this.columnIndex = columnIndex; }

    @Override
    public String toString() {
        return "ParkingSlot{spotId=" + spotId + ", status='" + status + "'}";
    }
}