package main.coursework3.controllers;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.coursework3.controllers.modals.BookingDetailsController;
import main.coursework3.controllers.modals.CheckInController;
import main.coursework3.io.Alerts;
import main.coursework3.model.Rooms;
import main.coursework3.services.RoomSearchService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class RoomSearchController implements Initializable {

    @FXML private DatePicker dpDateFrom;
    @FXML private DatePicker dpDateTo;
    @FXML private ComboBox<String> cmbRoomClass;
    @FXML private TextField txtMaxPrice;
    @FXML private Button handleSearch;
    @FXML private Button btnClear;
    @FXML private Button btnCreateBooking;
    @FXML private Button btnCheckIn;

    @FXML private TableView<Rooms> roomsTable;
    @FXML private TableColumn<Rooms, String> colRoomNumber;
    @FXML private TableColumn<Rooms, Integer> colFloor;
    @FXML private TableColumn<Rooms, String> colClass;
    @FXML private TableColumn<Rooms, Integer> colCapacity;
    @FXML private TableColumn<Rooms, Double> colPrice;
    @FXML private TableColumn<Rooms, String> colStatus;

    private final RoomSearchService searchService = new RoomSearchService();
    private final Alerts alerts = new Alerts();
    private final ObservableList<Rooms> observableRoomsList = FXCollections.observableArrayList();

    /** Ініціалізує таблицю, фільтри та обробники подій пошуку номерів. */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupFilters();
        setDefaultDates();

        handleSearch.setOnAction(event -> performSearch());
        btnClear.setOnAction(event -> clearFilters());
        btnCreateBooking.setOnAction(event -> handleBookingAction());
        btnCheckIn.setOnAction(event -> handleCheckInAction());

        performSearch();
    }
    /** Прив'язує стовпці таблиці до властивостей моделі кімнати. */
    private void setupTable() {
        colRoomNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoomNumber()));
        colFloor.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getFloor()).asObject());
        colClass.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoomClass()));
        colCapacity.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacity()).asObject());
        colPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getCostPerDay()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        roomsTable.setItems(observableRoomsList);
    }
    /** Ініціалізує фільтри пошуку та перевірку введення ціни. */
    private void setupFilters() {
        cmbRoomClass.setItems(FXCollections.observableArrayList("Всі","Економ", "Стандарт", "Напів-Люкс","Люкс"));
        cmbRoomClass.setValue("Всі");

        txtMaxPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtMaxPrice.setText(newValue.replaceAll("[^\\d.]", ""));
            }
        });
    }
    /** Встановлює стандартні дати пошуку номерів. */
    private void setDefaultDates() {
        if (dpDateFrom != null && dpDateFrom.getValue() == null) dpDateFrom.setValue(LocalDate.now());
        if (dpDateTo != null && dpDateTo.getValue() == null) dpDateTo.setValue(LocalDate.now().plusDays(1));
    }
    /** Виконує пошук вільних номерів за заданими параметрами. */
    private void performSearch() {
        LocalDate dateFrom = dpDateFrom.getValue();
        LocalDate dateTo = dpDateTo.getValue();
        String roomClass = cmbRoomClass.getValue();
        String maxPriceText = txtMaxPrice.getText();

        if (dateFrom == null || dateTo == null) {
            alerts.showError("Помилка вводу", "Будь ласка, вкажіть обидві дати!");
            return;
        }
        if (dateFrom.isAfter(dateTo)) {
            alerts.showError("Помилка дат", "Дата виїзду не може бути раніше дати заїзду!");
            return;
        }
        if (dateFrom.isBefore(LocalDate.now())) {
            alerts.showError("Помилка дат", "Неможливо знайти номери на минулі дати!");
            return;
        }

        try {
            List<Rooms> freeRooms = searchService.searchFreeRooms(dateFrom, dateTo, roomClass, maxPriceText);
            observableRoomsList.setAll(freeRooms);
        } catch (Exception e) {
            alerts.showError("Помилка СУБД", "Не вдалося отримати список номерів:\n" + e.getMessage());
        }
    }
    /** Очищує параметри пошуку та оновлює список номерів. */
    private void clearFilters() {
        txtMaxPrice.clear();
        cmbRoomClass.setValue("Всі");
        setDefaultDates();
        performSearch();
    }
    /** Відкриває форму створення нового бронювання для вибраного номера. */
    private void handleBookingAction() {
        Rooms selectedRoom = roomsTable.getSelectionModel().getSelectedItem();
        LocalDate dateFrom = dpDateFrom.getValue();
        LocalDate dateTo = dpDateTo.getValue();

        if (selectedRoom == null || dateFrom == null || dateTo == null) {
            alerts.showError("Помилка", "Оберіть номер та заповніть дати!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/bookings_details_modal.fxml"));
            Parent root = loader.load();

            BookingDetailsController controller = loader.getController();
            controller.setInitialData(0, 0, selectedRoom, dateFrom, dateTo, 0.0);

            openModalStage("Оформлення бронювання", root);
            performSearch();
        } catch (IOException e) {
            alerts.showError("Помилка", "Не вдалося відкрити вікно бронювання:\n" + e.getMessage());
        }
    }
    /** Відкриває форму прямого поселення гостя у вибраний номер. */
    private void handleCheckInAction() {
        Rooms selectedRoom = roomsTable.getSelectionModel().getSelectedItem();
        LocalDate dateFrom = dpDateFrom.getValue();
        LocalDate dateTo = dpDateTo.getValue();

        if (selectedRoom == null || dateTo == null) {
            alerts.showError("Помилка", "Оберіть номер та вкажіть дату виїзду!");
            return;
        }
        if (dateFrom == null) dateFrom = LocalDate.now();
        if (dateFrom.isAfter(LocalDate.now())) {
            alerts.showError("Помилка", "Пряме поселення можливе лише на сьогодні!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/check_in_modal.fxml"));
            Parent root = loader.load();

            CheckInController controller = loader.getController();
            controller.setInitialData(0, 0, selectedRoom, dateFrom, dateTo, "Не оплачено");

            openModalStage("Оформлення поселення — Номер " + selectedRoom.getRoomNumber(), root);
            performSearch();
        } catch (IOException e) {
            alerts.showError("Помилка", "Не вдалося відкрити вікно поселения:\n" + e.getMessage());
        }
    }
    /** Відкриває модальне вікно з переданим інтерфейсом. */
    private void openModalStage(String title, Parent root) {
        Stage modalStage = new Stage();
        modalStage.setTitle(title);
        modalStage.initModality(Modality.WINDOW_MODAL);
        modalStage.initOwner(roomsTable.getScene().getWindow());
        modalStage.setScene(new Scene(root));
        modalStage.setResizable(false);
        modalStage.showAndWait();
    }
}