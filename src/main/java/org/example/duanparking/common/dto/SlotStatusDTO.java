package org.example.duanparking.common.dto;

import java.io.Serializable;

public class SlotStatusDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String spotId;
    private String status;
    private String areaStyle;

    public SlotStatusDTO(String spotId, String status) {
        this.spotId = spotId;
        this.status = status;
    }

    public SlotStatusDTO(String spotId, String status, String areaStyle) {
        this.spotId = spotId;
        this.status = status;
        this.areaStyle = areaStyle;
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

    public String getAreaStyle() {
        return areaStyle;
    }

    public void setAreaStyle(String areaStyle) {
        this.areaStyle = areaStyle;
    }
}
