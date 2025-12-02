package org.example.duanparking.model;

import java.time.LocalTime;


public enum ShiftType {
    MORNING(LocalTime.of(7, 0), LocalTime.of(12, 0)),
    AFTERNOON(LocalTime.of(12, 1), LocalTime.of(17, 50)),
    EVENING(LocalTime.of(17, 51), LocalTime.of(23, 59)),
    ALL_DAY(LocalTime.of(0, 0), LocalTime.of(23, 59));

    public final LocalTime start;
    public final LocalTime end;

    ShiftType(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }
}
