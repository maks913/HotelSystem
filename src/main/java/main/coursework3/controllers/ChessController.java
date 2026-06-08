package main.coursework3.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import main.coursework3.io.Alerts;
import main.coursework3.io.Reports;
import main.coursework3.model.Bookings;
import main.coursework3.model.Rooms;
import main.coursework3.model.Settlements;
import main.coursework3.services.ChessService;
import main.coursework3.services.SettlementService;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static main.coursework3.io.Reports.convertMonthToNumber;

public class ChessController implements Initializable {
    @FXML
    private ComboBox<String> comboMonth;
    @FXML
    private ComboBox<Integer> comboYear;
    @FXML
    private Button btnPrint;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private GridPane chessGrid;
    @FXML
    private Button btnOpReport;
    @FXML
    private Button btnBusinessReport;

    private final ChessService chessService = new ChessService();
    private final SettlementService settlementService = new SettlementService();
    private final Alerts alerts = new Alerts();
    private final Reports reports = new Reports();

    private List<Bookings> currentBookings = new ArrayList<>();
    private List<Settlements> currentSettlements = new ArrayList<>();

    private final String[] months = {
            "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
            "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
    };

    /**
     * Ініціалізація контролера та налаштування обробників подій.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupComboBoxes();

        comboMonth.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> refreshDataAndBuildGrid());
        comboYear.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshDataAndBuildGrid());

        btnPrint.setOnAction(event -> handlePrintFinancialReport());
        btnOpReport.setOnAction(event -> handlePrintOperationalReport());
        btnBusinessReport.setOnAction(event -> handlePrintBusinessReport());
        refreshDataAndBuildGrid();
    }

    /**
     * Налаштування списків місяців та років у комбобоксах.
     */
    private void setupComboBoxes() {
        comboMonth.setItems(FXCollections.observableArrayList(months));
        int currentYear = LocalDate.now().getYear();
        comboYear.setItems(FXCollections.observableArrayList(currentYear - 1, currentYear, currentYear + 1));

        comboMonth.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
        comboYear.getSelectionModel().select(Integer.valueOf(currentYear));
    }

    /**
     * Оновлення даних та побудова шахматної сітки завантаженості номерів.
     */
    private void refreshDataAndBuildGrid() {
        try {
            settlementService.runFullHotelAutoUpdate();

            currentBookings = chessService.getAllBookings();
            currentSettlements = chessService.getAllSettlements();
            buildGrid();
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка бази даних", "Не вдалося оновити дані для графіку завантаженості:\n" + e.getMessage());
        }
    }

    /**
     * Побудова таблиці завантаженості номерного фонду по днях.
     */
    private void buildGrid() {
        chessGrid.getChildren().clear();
        chessGrid.getRowConstraints().clear();
        chessGrid.getColumnConstraints().clear();

        int monthNum = comboMonth.getSelectionModel().getSelectedIndex() + 1;
        Integer year = comboYear.getSelectionModel().getSelectedItem();
        if (year == null) return;

        YearMonth yearMonth = YearMonth.of(year, monthNum);
        int daysInMonth = yearMonth.lengthOfMonth();

        ColumnConstraints cNum = new ColumnConstraints();
        cNum.setHgrow(Priority.ALWAYS);
        chessGrid.getColumnConstraints().add(cNum);

        for (int day = 1; day <= daysInMonth; day++) {
            ColumnConstraints cDay = new ColumnConstraints();
            cDay.setHgrow(Priority.ALWAYS);
            cDay.setFillWidth(true);
            chessGrid.getColumnConstraints().add(cDay);
        }

        RowConstraints headerConstraints = new RowConstraints();
        headerConstraints.setPrefHeight(35);
        headerConstraints.setMinHeight(35);
        headerConstraints.setMaxHeight(35);
        headerConstraints.setVgrow(Priority.NEVER);
        chessGrid.getRowConstraints().add(headerConstraints);

        Label cornerLabel = new Label("Кімната / День");
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5px;");
        cornerLabel.setPrefWidth(120);
        cornerLabel.setAlignment(Pos.BOTTOM_LEFT);
        chessGrid.add(cornerLabel, 0, 0);

        for (int day = 1; day <= daysInMonth; day++) {
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 0.5px; -fx-padding: 5px;");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setMaxHeight(35);
            GridPane.setHgrow(dayLabel, Priority.ALWAYS);
            GridPane.setVgrow(dayLabel, Priority.NEVER);
            chessGrid.add(dayLabel, day, 0);
        }

        try {
            List<Rooms> rooms = chessService.getAllRooms();

            for (int rowIndex = 0; rowIndex < rooms.size(); rowIndex++) {
                Rooms room = rooms.get(rowIndex);
                int actualRow = rowIndex + 1;

                RowConstraints roomRowConstraints = new RowConstraints();
                roomRowConstraints.setPrefHeight(40);
                roomRowConstraints.setMinHeight(35);
                roomRowConstraints.setMaxHeight(60);
                roomRowConstraints.setVgrow(Priority.NEVER);
                chessGrid.getRowConstraints().add(roomRowConstraints);

                Label roomLabel = new Label("Кімн. " + room.getRoomNumber());
                roomLabel.setStyle("-fx-padding: 5px; -fx-font-weight: bold;");
                roomLabel.setAlignment(Pos.CENTER_LEFT);
                chessGrid.add(roomLabel, 0, actualRow);

                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate currentDate = LocalDate.of(year, monthNum, day);
                    Pane cell = createCell(room, currentDate);
                    chessGrid.add(cell, day, actualRow);
                }
            }
        } catch (Exception e) {
            alerts.showError("Помилка рендеру", "Не вдалося побудувати сітку номерного фонду:\n" + e.getMessage());
        }
    }

    /**
     * Створення окремої клітинки графіку для кімнати та дати.
     */
    private Pane createCell(Rooms room, LocalDate date) {
        Pane pane = new Pane();
        pane.setPrefSize(30, 30);
        pane.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0.5px;");

        String status = chessService.getRoomStatusOnDate(room, date, currentSettlements, currentBookings);

        Color bgColor = Reports.forStatus(status);

        pane.setBackground(new Background(new BackgroundFill(bgColor, CornerRadii.EMPTY, Insets.EMPTY)));

        Tooltip tooltip = new Tooltip(date.toString() + "\nКімната: " + room.getRoomNumber() + "\nСтатус: " + status);
        Tooltip.install(pane, tooltip);
        pane.setMinSize(25, 30);
        pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return pane;
    }

    /**
     * Формування та відображення фінансового звіту готелю.
     */
    private void handlePrintFinancialReport() {
        String selectedMonth = comboMonth.getValue();
        String selectedYear = String.valueOf(comboYear.getValue());

        if (selectedMonth == null || selectedYear.equals("null")) {
            alerts.showError("Помилка формування", "Будь ласка, оберіть місяць та рік у верхній панелі!");
            return;
        }

        int monthNum = convertMonthToNumber(selectedMonth);
        int yearNum = Integer.parseInt(selectedYear);

        try {
            double[][] financialData = chessService.getFinancialReportData(monthNum, yearNum);
            String reportText = Reports.buildFinancialReport(selectedMonth, yearNum, financialData[0], financialData[1]);
            alerts.showReportWindow(reportText, selectedMonth + " " + yearNum, "Офіційний фінансовий звіт");
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка БД", "Не вдалося згенерувати звіт:\n" + e.getMessage());
        }
    }

    /**
     * Формування та відображення операційного звіту готелю.
     */
    private void handlePrintOperationalReport() {
        String selectedMonth = comboMonth.getValue();
        String selectedYear = String.valueOf(comboYear.getValue());

        if (selectedMonth == null || selectedYear.equals("null")) {
            alerts.showError("Помилка формування", "Будь ласка, оберіть місяць та рік!");
            return;
        }

        int monthNum = convertMonthToNumber(selectedMonth);
        int yearNum = Integer.parseInt(selectedYear);

        try {
            Object[] opStats = chessService.getOperationalReportData(monthNum, yearNum);
            String reportText = Reports.buildOperationalReport(selectedMonth, yearNum, opStats);
            alerts.showReportWindow(reportText, "Операційний_" + selectedMonth + "_" + yearNum, "Операційний звіт готелю");
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка БД", "Не вдалося згенерувати звіт:\n" + e.getMessage());
        }
    }

    /**
     * Формування та відображення бізнес-аналітичного звіту готелю.
     */
    private void handlePrintBusinessReport() {
        String selectedMonth = comboMonth.getValue();
        String selectedYear = String.valueOf(comboYear.getValue());

        if (selectedMonth == null || selectedYear.equals("null")) {
            alerts.showError("Помилка формування", "Будь ласка, оберіть місяць та рік!");
            return;
        }

        int monthNum = Reports.convertMonthToNumber(selectedMonth);
        int yearNum = Integer.parseInt(selectedYear);

        try {
            Map<String, Object> analysisData = settlementService.getRoomPerformanceStats(monthNum, yearNum);
            String reportText = Reports.buildAdvancedBusinessAnalysis(analysisData);
            alerts.showReportWindow(reportText, "Аналітика_" + selectedMonth + "_" + yearNum, "Розгорнутий бізнес-аналіз готелю");
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка БД", "Не вдалося згенерувати звіт:\n" + e.getMessage());
        }
    }
}