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
import main.coursework3.controllers.modals.RoomDetailsController;
import main.coursework3.dao.RoomDAO;
import main.coursework3.io.Alerts;
import main.coursework3.model.Rooms;
import main.coursework3.services.SettlementService;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RoomFundController implements Initializable {
    private static final String FILTER_ALL = "Всі";

    @FXML private TableView<Rooms> roomsTable;
    @FXML private TableColumn<Rooms, String> colRoomNumber;
    @FXML private TableColumn<Rooms, Integer> colFloor;
    @FXML private TableColumn<Rooms, String> colClass;
    @FXML private TableColumn<Rooms, Double> colPrice;
    @FXML private TableColumn<Rooms, Integer> colCapacity;
    @FXML private TableColumn<Rooms, String> colStatus;

    @FXML private ComboBox<String> filterClass;
    @FXML private ComboBox<String> filterStatus;

    @FXML private Button btnAddRoom;
    @FXML private Button btnEditRoom;
    @FXML private Button btnDeleteRoom;

    private final Alerts alerts = new Alerts();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SettlementService settlementService = new SettlementService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        setupEventHandlers();

        loadData();
    }

    /** Прив'язує стовпці таблиці до властивостей моделі. */
    private void setupTableColumns() {
        colRoomNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoomNumber()));
        colFloor.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getFloor()).asObject());
        colClass.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoomClass()));
        colPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getCostPerDay()).asObject());
        colCapacity.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacity()).asObject());
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
    }

    /** Ініціалізує ComboBox-фільтри та слухачі змін. */
    private void setupFilters() {
        filterClass.setItems(FXCollections.observableArrayList(FILTER_ALL, "Економ", "Стандарт", "Напів-Люкс", "Люкс"));
        filterStatus.setItems(FXCollections.observableArrayList(FILTER_ALL, RoomDAO.STATUS_FREE, RoomDAO.STATUS_OCCUPIED, RoomDAO.STATUS_BOOKED, "Ремонт"));
        filterClass.setValue(FILTER_ALL);
        filterStatus.setValue(FILTER_ALL);
        filterClass.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterStatus.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /** Реєструє обробники подій для кнопок та подвійного кліку. */
    private void setupEventHandlers() {
        btnAddRoom.setOnAction(event -> openRoomDetailsModal(null));

        btnEditRoom.setOnAction(event -> {
            Rooms selected = roomsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openRoomDetailsModal(selected);
            } else {
                alerts.showError("Вибір відсутній", "Будь ласка, виберіть номер для редагування.");
            }
        });

        btnDeleteRoom.setOnAction(event -> handleDeleteRoom());

        roomsTable.setRowFactory(tv -> {
            TableRow<Rooms> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openRoomDetailsModal(row.getItem());
                }
            });
            return row;
        });
    }

    /** Завантажує дані з БД та відображає у таблиці. */
    private void loadData() {
        try {
            settlementService.runRoomAutoUpdate();

            ObservableList<Rooms> rooms = FXCollections.observableArrayList(roomDAO.findAll());
            roomsTable.setItems(rooms);
        } catch (Exception e) {
            alerts.showError("Помилка БД", "Не вдалося завантажити список кімнат:\n" + e.getMessage());
        }
    }

    /** Застосовує вибрані фільтри та оновлює таблицю. */
    private void applyFilters() {
        String selectedClass = filterClass.getValue();
        String selectedStatus = filterStatus.getValue();

        boolean isClassFiltered = selectedClass != null && !selectedClass.equals(FILTER_ALL);
        boolean isStatusFiltered = selectedStatus != null && !selectedStatus.equals(FILTER_ALL);

        try {
            if (!isClassFiltered && !isStatusFiltered) {
                loadData();
            } else {
                ObservableList<Rooms> filteredRooms = FXCollections.observableArrayList(
                        roomDAO.findWithFilters(selectedClass, selectedStatus)
                );
                roomsTable.setItems(filteredRooms);
            }
        } catch (Exception e) {
            alerts.showError("Помилка фильтрації", "Не вдалося відфільтрувати номери:\n" + e.getMessage());
        }
    }

    /**
     * Відкриває модальне вікно для створення або редагування
     * кімнати. Після закриття оновлює список.
     */
    private void openRoomDetailsModal(Rooms room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/room_details_modal.fxml"));
            Parent root = loader.load();

            RoomDetailsController controller = loader.getController();
            if (room != null) {
                controller.setRoomData(room);
            }

            Stage stage = new Stage();
            stage.setTitle(room == null ? "Додавання нового номера" : "Редагування номера: " + room.getRoomNumber());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(roomsTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            loadData();

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка завантаження", "Не вдалося відкрити вікно деталей номера:\n" + e.getMessage());
        }
    }

    /**Видалення кімнати зі списку */
    private void handleDeleteRoom() {
        Rooms selectedRoom = roomsTable.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            alerts.showError("Вибір відсутній", "Будь ласка, виберіть кімнату для видалення.");
            return;
        }

        boolean isConfirmed = alerts.showConfirmation(
                "Підтвердження видалення",
                "Ви впевнені, що хочете видалити номер " + selectedRoom.getRoomNumber() + "?",
                "Ця дія незворотна. Якщо номер пов'язаний з активними бронюваннями, видалення буде заблоковано СУБД."
        );

        if (isConfirmed) {
            try {
                roomDAO.deleteRoom(selectedRoom.getIdRoom());
                alerts.showMessage("Успіх", "Номер успішно видалено з номерного фонду.");
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
                alerts.showError("Помилка видалення", "Неможливо видалити цей номер!\n" +
                        "Він пов'язаний з історією заселень або активними бронями клиентов.");
            }
        }
    }
}