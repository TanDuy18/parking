package org.example.duanparking.common.dto.rent;

import java.io.Serializable;
import java.time.LocalDate;

public class RentResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;          // thuê thành công hay thất bại
    private String message;           // thông báo cho client
    private int rentId;            // mã đơn thuê (server tạo)
    private double totalAmount;       // số tiền cuối cùng server tính
    private LocalDate fromDate;
    private LocalDate toDate;
    private String assignedSlot;      // số ô xe mà server cấp

    public RentResult() {}

    public RentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public RentResult(boolean success,  int rentId,String message ) {
        this.success = success;
        this.message = message;
        this.rentId = rentId;
    }

    public RentResult(boolean success, String message, int rentId,
                      double totalAmount, LocalDate fromDate,
                      LocalDate toDate, String assignedSlot) {
        this.success = success;
        this.message = message;
        this.rentId = rentId;
        this.totalAmount = totalAmount;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.assignedSlot = assignedSlot;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getRentId() { return rentId; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public String getAssignedSlot() { return assignedSlot; }
}
