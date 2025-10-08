package org.example.duanparking.common;

import java.io.Serializable;

public class ParkingResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status;  // "Approved", "Denied", "Full"
    private int slotId;  // ID slot nếu approved (-1 nếu không)
    private String message;  // Chi tiết lỗi hoặc info

    public ParkingResponse(String status) {
        this(status, -1, "");
    }

    public ParkingResponse(String status, int slotId) {
        this(status, slotId, "");
    }

    public ParkingResponse(String status, int slotId, String message) {
        this.status = status;
        this.slotId = slotId;
        this.message = message;
    }

    // Getters
    public String getStatus() { return status; }
    public int getSlotId() { return slotId; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "ParkingResponse{status='" + status + "', slotId=" + slotId + ", message='" + message + "'}";
    }
}
