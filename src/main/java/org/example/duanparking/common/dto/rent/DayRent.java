package org.example.duanparking.common.dto.rent;

import java.io.Serializable;
import java.time.LocalTime;

public class DayRent implements Serializable {
    public String dayOfWeek; // MONDAY, TUESDAY...
    public LocalTime start;
    public LocalTime end;

    public DayRent(String dayOfWeek, LocalTime start, LocalTime end) {
        this.dayOfWeek = dayOfWeek;
        this.start = start;
        this.end = end;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStart() {
        return start;
    }

    public void setStart(LocalTime start) {
        this.start = start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public void setEnd(LocalTime end) {
        this.end = end;
    }
}
