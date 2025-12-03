package org.example.duanparking.model;

import java.time.LocalTime;

public enum ShiftType {
    MORNING(LocalTime.of(7, 0), LocalTime.of(11, 59)),
    AFTERNOON(LocalTime.of(12, 0), LocalTime.of(17, 59)),
    EVENING(LocalTime.of(18, 0), LocalTime.of(6, 59)), // Kéo dài qua đêm
    FULL_DAY(LocalTime.of(0, 0), LocalTime.of(23, 59));

    public final LocalTime start;
    public final LocalTime end;

    ShiftType(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }
}