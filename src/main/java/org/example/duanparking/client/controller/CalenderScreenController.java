package org.example.duanparking.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ResourceBundle;

public class CalenderScreenController implements Initializable {
    @FXML
    private Button nextBtn;
    @FXML
    private Button prevBtn;
    @FXML
    private GridPane calendarGrid;
    @FXML
    private Button monthBtn;
    @FXML
    private Label fromValue1;
    @FXML
    private Label toValue1;
    @FXML private Button mon1Btn;
    @FXML private Button mon3Btn;
    @FXML private Button year1Btn;
    @FXML private Button confirmBtn;
    @FXML private AnchorPane rootPane;
    private YearMonth currentMonth;
    private LocalDate startDate = null;
    private LocalDate endDate = null;
    private RentController rentController;

    public void setRentController(RentController controller) {
        this.rentController = controller;
    }

    public void setInitialDates(LocalDate start, LocalDate end) {
        this.startDate = start;
        this.endDate = end;

        Platform.runLater(() -> {
            fromValue1.setText(start != null
                    ? start.getDayOfMonth() + "/" + start.getMonthValue() + "/" + start.getYear()
                    : "--/--/----");

            toValue1.setText(end != null
                   ? end.getDayOfMonth() + "/" + end.getMonthValue() + "/" + end.getYear()
                    : "--/--/----");
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentMonth = YearMonth.now();
        Platform.runLater(this::updateCalendar);
        nextBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        prevBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });
        mon1Btn.setOnAction(e -> {handle1month(startDate);});
        year1Btn.setOnAction(e -> {handle1year(startDate);});
        mon3Btn.setOnAction(e -> {handle3month(startDate);});
        confirmBtn.setOnAction(e -> {
            if (rentController != null) {
                rentController.updateRentDates(startDate, endDate);
            }

            Stage stage = (Stage) confirmBtn.getScene().getWindow();
            stage.close();
        });
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();
        monthBtn.setText(currentMonth.getMonthValue() + "/" + currentMonth.getYear());
        renderHeader();

        LocalDate firstDay = currentMonth.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1 ;
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 1;
        int col = startCol;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);

            Label lbl = new Label(String.format("%02d", day));
            lbl.setPrefSize(55, 45);
            lbl.setAlignment(Pos.CENTER);

            String normalStyle = """
                -fx-font-size: 16px;
                -fx-text-fill: #3A3A3A;
            """;
            lbl.setStyle(normalStyle);

            LocalDate today = LocalDate.now();
            if (date.isBefore(today)) {
                lbl.setDisable(true);
                lbl.setStyle("""
                    -fx-opacity: 0.4;
                    -fx-font-size: 16px;
                    -fx-text-fill: #999999;
                """);

                lbl.setOnMouseEntered(null);
                lbl.setOnMouseExited(null);

                calendarGrid.add(lbl, col, row);
                col++;
                if (col > 6) {
                    col = 0;
                    row++;
                }
                continue;
            }

            if (startDate != null && endDate != null &&
                    !date.isBefore(startDate) && !date.isAfter(endDate)) {


                lbl.setStyle("""
                            -fx-background-color: #D2EEFF;
                            -fx-font-size: 16px;
                            -fx-text-fill: #1A558C;
                        """);
            }

            if (date.equals(startDate) || date.equals(endDate)) {
                lbl.setStyle("""
                            -fx-background-radius: 50;
                            -fx-background-color: #34C3FF;
                            -fx-text-fill: white;
                            -fx-font-size: 16px;
                        """);
            }


            lbl.setOnMouseClicked(e -> {
               if(startDate != null && date.isEqual(startDate)) { // bỏ chọn start date
                   if (endDate != null) {
                       startDate = null;

                       Platform.runLater(() -> {
                           fromValue1.setText("--/--/----");
                           toValue1.setText(endDate.getDayOfMonth()+"/"+endDate.getMonthValue()+"/"+endDate.getYear());
                       });

                   } else {
                       startDate = null;

                       Platform.runLater(() -> {
                           fromValue1.setText("--/--/----");
                           toValue1.setText("--/--/----");
                       });
                   }
                   updateCalendar();
                   return;
               }

                if (endDate != null && date.isEqual(endDate)) {
                    endDate = null;

                    Platform.runLater(() -> toValue1.setText("--/--/----"));

                    updateCalendar();
                    return;
                }

                if (endDate != null && !date.isEqual(endDate) && startDate != null) {
                    endDate = date;
                    Platform.runLater(() -> {
                       toValue1.setText(endDate.getDayOfMonth() + "/" + endDate.getMonthValue() + "/" + endDate.getYear());
                    });
                    updateCalendar();
                    return;
                }

                if (startDate == null) {
                    startDate = date;
                    Platform.runLater(() -> fromValue1.setText(date.getDayOfMonth()+"/"+date.getMonthValue()+"/"+date.getYear()));
                } else if (endDate == null) {
                    if (date.equals(startDate)) {
                        endDate = startDate;
                        Platform.runLater(() ->
                                toValue1.setText(startDate.getDayOfMonth() + "/" +
                                        startDate.getMonthValue() + "/" +
                                        startDate.getYear())
                        );
                    }else if (date.isBefore(startDate)) {
                        endDate = startDate;
                        startDate = date;
                        Platform.runLater(() -> {
                            fromValue1.setText(startDate.getDayOfMonth() + "/" +
                                    startDate.getMonthValue() + "/" +
                                    startDate.getYear());
                            toValue1.setText(endDate.getDayOfMonth() + "/" +
                                    endDate.getMonthValue() + "/" +
                                    endDate.getYear());
                        });
                    } else {
                        endDate = date;

                        Platform.runLater(() -> toValue1.setText(
                                endDate.getDayOfMonth() + "/" +
                                        endDate.getMonthValue() + "/" +
                                        endDate.getYear()
                        ));
                    }

                } else {
                    startDate = date;
                    endDate = null;

                    Platform.runLater(() -> {
                        fromValue1.setText(startDate.getDayOfMonth() + "/" +
                                startDate.getMonthValue() + "/" +
                                startDate.getYear());
                        toValue1.setText("--/--/----");
                    });
                }

                updateCalendar();
            });
            String appliedStyle = lbl.getStyle();

            lbl.setOnMouseEntered(e -> lbl.setStyle(appliedStyle + "-fx-background-color: #EAF7FF;"));
            lbl.setOnMouseExited(e -> lbl.setStyle(appliedStyle));


            calendarGrid.add(lbl, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private void renderHeader() {
        String[] vietnameseDays = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };

        for (int i = 0; i < vietnameseDays.length; i++) {
            Label lb = new Label(vietnameseDays[i]);
            lb.setFont(Font.font(16));
            lb.setAlignment(Pos.CENTER);
            lb.setStyle("-fx-font-weight: bold;");
            lb.setPrefSize(60, 40);
            calendarGrid.add(lb, i, 0);
        }
    }
    private void handle1month(LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
            LocalDate finalStartDate = startDate;
            Platform.runLater(() ->
                    fromValue1.setText(finalStartDate.getDayOfMonth() + "/" +
                            finalStartDate.getMonthValue() + "/" +
                            finalStartDate.getYear())
            );
        }

        endDate = startDate.plusMonths(1);

        Platform.runLater(() ->
                toValue1.setText(endDate.getDayOfMonth() + "/" +
                        endDate.getMonthValue() + "/" +
                        endDate.getYear())
        );

        updateCalendar();
    }

    private void handle3month(LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
            LocalDate finalStartDate = startDate;
            Platform.runLater(() ->
                    fromValue1.setText(finalStartDate.getDayOfMonth() + "/" +
                            finalStartDate.getMonthValue() + "/" +
                            finalStartDate.getYear())
            );
        }

        endDate = startDate.plusMonths(3);

        Platform.runLater(() ->
                toValue1.setText(endDate.getDayOfMonth() + "/" +
                        endDate.getMonthValue() + "/" +
                        endDate.getYear())
        );

        updateCalendar();
    }
    private void handle1year(LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
            LocalDate finalStartDate = startDate;
            Platform.runLater(() ->
                    fromValue1.setText(finalStartDate.getDayOfMonth() + "/" +
                            finalStartDate.getMonthValue() + "/" +
                            finalStartDate.getYear())
            );
        }

        endDate = startDate.plusYears(1);

        Platform.runLater(() ->
                toValue1.setText(endDate.getDayOfMonth() + "/" +
                        endDate.getMonthValue() + "/" +
                        endDate.getYear())
        );

        updateCalendar();
    }

}
