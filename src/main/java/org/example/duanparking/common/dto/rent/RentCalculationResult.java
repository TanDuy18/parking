package org.example.duanparking.common.dto.rent;

import java.io.Serializable;

public class RentCalculationResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private double totalAmount;
    private String description;
    private String detail;
    private boolean success;
    private String errorMessage;

    public RentCalculationResult(double total, String desc, String detail) {
        this.totalAmount = total;
        this.description = desc;
        this.detail = detail;
        this.success = true;
    }
    public RentCalculationResult(String error) {
        this.success = false;
        this.errorMessage = error;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
