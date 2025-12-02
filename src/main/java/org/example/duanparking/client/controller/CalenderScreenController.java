package org.example.duanparking.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ResourceBundle;

public class CalenderScreenController implements Initializable {

    @FXML
    private GridPane calendarGrid;
    @FXML private Button prevBtn, nextBtn, monthBtn;
    private YearMonth currentMonth;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentMonth = YearMonth.now();
        loadCalendar(currentMonth);

        prevBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadCalendar(currentMonth);
        });

        nextBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadCalendar(currentMonth);
        });
    }

    private void loadCalendar(YearMonth ym) {
        calendarGrid.getChildren().clear();
        monthBtn.setText(ym.getMonthValue() + "/" + ym.getYear());

        LocalDate firstDay = ym.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = ym.lengthOfMonth();

        int col = startCol, row = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            Button btn = createDayButton(day);
            calendarGrid.add(btn, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private Button createDayButton(int day) {
        Button btn = new Button(String.valueOf(day));
        btn.setPrefSize(42, 42);
        btn.setStyle("-fx-background-radius: 20; -fx-font-size: 13;");

        btn.setOnAction(e -> {
            btn.setStyle("-fx-background-color: #4CC3FF; -fx-background-radius: 20;");
        });

        return btn;
    }

}
