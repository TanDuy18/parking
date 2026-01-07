package  org.example.duanparking.common; 
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.common.dto.rent.ScheduleDTO;
import org.example.duanparking.model.DisplayMode;
import  java.time.LocalTime ; 
import java.time.temporal.ChronoUnit;

public class SlotViewModel {
    private final StringProperty spotId = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty areaType = new SimpleStringProperty();
    private final ObjectProperty<LocalTime> startTime = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> endTime = new SimpleObjectProperty<>();
    private final BooleanProperty highlighted = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalTime> currentTime = new SimpleObjectProperty<>(LocalTime.now());

    private final ObservableList<ScheduleDTO> dailySchedules = FXCollections.observableArrayList();
    private final ObjectProperty<ScheduleDTO> activeSchedule = new SimpleObjectProperty<>();


    public SlotViewModel(ParkingSlotDTO dto) {
        this.spotId.set(dto.getSpotId());
        this.status.set(dto.getStatus());
        this.areaType.set(dto.getAreaType());
        if (dto.getSchedules() != null) {
            this.dailySchedules.setAll(dto.getSchedules());
        }
        currentTime.addListener((obs, oldT, newT) -> updateActiveSchedule(newT));
        updateActiveSchedule(LocalTime.now());
    }


    private void updateActiveSchedule(LocalTime now) {
        ScheduleDTO match = dailySchedules.stream()
                .filter(s -> now.isBefore(s.getEndTime()) || s.isOvernight())
                .findFirst()
                .orElse(null);
        activeSchedule.set(match);

        if (match != null) {
            this.startTime.set(match.getStartTime());
            this.endTime.set(match.getEndTime());
        } else {
            this.startTime.set(null);
            this.endTime.set(null);
        }
    }

    public StringBinding colorBinding(DisplayMode mode) {
        return Bindings.createStringBinding(() -> {
            String currentStatus = status.get() == null ? "" : status.get();
            String type = areaType.get() == null ? "STANDARD" : areaType.get();

            LocalTime start = startTime.get();
            LocalTime end = endTime.get();
            LocalTime now = currentTime.get();

            if (mode == DisplayMode.RENT_MANAGEMENT) {
                if ("RENTED".equalsIgnoreCase(type)) return "#aa44ff"; // tím
                if ("PREMIUM".equalsIgnoreCase(type)) return "#3388ff"; // Xanh
                return "#44aa44"; //
            }

            if ("OCCUPIED".equalsIgnoreCase(currentStatus)) {
                return "#ff4444"; // Đỏ
            }

            if (start != null && end != null && now != null) {
                boolean overnight = end.isBefore(start);
                    boolean inRentPeriod;

                    // 1. Kiểm tra xem có đang TRONG giờ thuê hay không
                    if (!overnight) {
                        inRentPeriod = (now.equals(start) || now.isAfter(start)) && (now.equals(end) || now.isBefore(end));
                    } else {
                        // Xuyên đêm: Đang ở cuối ngày cũ HOẶC đầu ngày mới
                        inRentPeriod = now.equals(start) || now.isAfter(start) || now.equals(end) || now.isBefore(end);
                    }

                    if (inRentPeriod) {
                        return "#FFA500"; // Đang trong giờ thuê -> Cam
                    }

                    // 2. Kiểm tra SẮP ĐẾN giờ thuê (Logic 30 phút)
                    long minutesUntil = ChronoUnit.MINUTES.between(now, start);
                    
                    // Xử lý khi now là 23:50 và start là 00:10 (xuyên ngày)
                    if (minutesUntil < 0) {
                        minutesUntil += 1440; // Cộng thêm 24 tiếng (1440 phút) để ra số phút thực tế
                    }

                    if (minutesUntil > 0 && minutesUntil <= 30) {
                        return "#FFA500"; // Sắp đến giờ -> Cam
                    } else {
                        return "#CCFF99";
                    }
            }

            // 4. HẾT LỊCH → màu theo khu vực
            return switch (type.toUpperCase()) {
                case "PREMIUM" -> "#3388ff";
                case "EV" -> "#44ff44";
                case "MOTOR" -> "#ffaa00";
                default -> "#44aa44";
            };

        }, status, areaType, startTime, endTime,currentTime);
    }


    public StringBinding textBinding() {
        return Bindings.createStringBinding(() -> {
            String id = spotId.get() == null ? "" : spotId.get();
            String st = status.get() == null ? "" : status.get();
            LocalTime start = startTime.get();
            LocalTime end = endTime.get();
            LocalTime now = currentTime.get();

            if ("OCCUPIED".equalsIgnoreCase(st)) {
                return id + "\n[ĐANG ĐỖ]";
            }

            if (start == null || end == null || now == null) {
                return id;
            }

            boolean overnight = end.isBefore(start) || end.equals(start); 

            if(overnight) {
                if (now.isBefore(start)) {
                    long minutesUntil = ChronoUnit.MINUTES.between(now, start);
                    if (minutesUntil <= 30) {
                        return id + "\n(-> " + start.toString().substring(0, 5) + ")";
                    }
                    return id;
                }
                return id + "\n(-> " + end.toString().substring(0, 5) + ")";
            }

            long minutesUntil = ChronoUnit.MINUTES.between(now, start);
            if (minutesUntil > 30) {
                return id + "\n(-> " + start.toString().substring(0, 5) + ")";
            } else if (minutesUntil > 0) {
                return id + "\n(-> " + end.toString().substring(0, 5) + ")";
            }

            if(!now.isBefore(start) && now.isBefore(end)) {
                return id + "\n(-> " + end.toString().substring(0, 5) + ")";
            }

            return id;
        }, spotId, startTime, endTime,status, currentTime);
    }


    // Getters để cập nhật giá trị
    public void setStatus(String status) { this.status.set(status); }
    public String getSpotId() { return spotId.get(); }

    public StringBinding borderBinding() {
        return Bindings.createStringBinding(() ->
                        highlighted.get() ? "-fx-border-color: yellow; -fx-border-width: 4;" : "-fx-border-color: black; -fx-border-width: 1;"
                , highlighted);
    }

    public void setHighlighted(boolean value) {
        this.highlighted.set(value);
    }

    public boolean isHighlighted() {
        return highlighted.get();
    }

    public void updateTime() {
        this.currentTime.set(LocalTime.now());   
    }
    public ObservableList<ScheduleDTO> getDailySchedules() {
        return dailySchedules;
    }
}
