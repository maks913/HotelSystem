package main.coursework3.controllers.modals;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.coursework3.io.Alerts;
import main.coursework3.model.Appliances;
import main.coursework3.model.Rooms;
import main.coursework3.dao.RoomDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDetailsController {
    @FXML
    private TextField txtRoomNumber, txtFloor, txtCost, txtCapacity;
    @FXML
    private ComboBox<String> comboClass, comboStatus;
    @FXML
    private TableView<Appliances> appliancesTable;
    @FXML
    private TableColumn<Appliances, String> colAppName, colAppCondition;
    @FXML
    private Button btnSave, btnCancel, btnAddAppliance, btnRemoveAppliance;

    private final RoomDAO roomDAO = new RoomDAO();
    private final Alerts alerts = new Alerts();
    private Rooms currentRoom;

    private final ObservableList<Appliances> localAppliancesList = FXCollections.observableArrayList();

    /**
     * Ініціалізація форми редагування кімнати та налаштування таблиці техніки.
     */
    @FXML
    public void initialize() {
        colAppName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colAppCondition.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTechnicalCondition()));

        appliancesTable.setItems(localAppliancesList);

        comboClass.setItems(FXCollections.observableArrayList("Економ", "Стандарт", "Напів-Люкс", "Люкс"));
        comboStatus.setItems(FXCollections.observableArrayList(RoomDAO.STATUS_FREE, RoomDAO.STATUS_OCCUPIED, RoomDAO.STATUS_BOOKED, "Ремонт"));

        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> handleCancel());
        btnAddAppliance.setOnAction(e -> handleAddAppliance());
        btnRemoveAppliance.setOnAction(e -> handleRemoveAppliance());
    }

    /**
     * Заповнення форми даними вибраної кімнати.
     */
    public void setRoomData(Rooms room) {
        this.currentRoom = room;

        if (room != null) {
            txtRoomNumber.setText(room.getRoomNumber());
            txtFloor.setText(String.valueOf(room.getFloor()));
            txtCost.setText(String.valueOf(room.getCostPerDay()));
            comboClass.setValue(room.getRoomClass());
            txtCapacity.setText(String.valueOf(room.getCapacity()));
            comboStatus.setValue(room.getStatus());

            localAppliancesList.setAll(roomDAO.getAppliancesForRoom(room.getIdRoom()));
        } else {
            comboStatus.setValue(RoomDAO.STATUS_FREE);
            comboClass.setValue("Стандарт");
            txtFloor.setText("1");
            txtCapacity.setText("2");
        }
    }

    /**
     * Перевірка та збереження даних кімнати і техніки.
     */
    @FXML
    private void handleSave() {
        if (!isInputValid()) return;

        try {
            if (currentRoom == null) {
                currentRoom = new Rooms();
            }

            currentRoom.setRoomNumber(txtRoomNumber.getText().trim());
            currentRoom.setFloor(Integer.parseInt(txtFloor.getText().trim()));
            currentRoom.setCostPerDay(Double.parseDouble(txtCost.getText().trim().replace(",", ".")));
            currentRoom.setCapacity(Integer.parseInt(txtCapacity.getText().trim()));
            currentRoom.setRoomClass(comboClass.getValue());
            currentRoom.setStatus(comboStatus.getValue());

            if (currentRoom.getIdRoom() == 0) {
                roomDAO.insertRoom(currentRoom);

                Rooms savedRoom = roomDAO.findAll().stream()
                        .filter(r -> r.getRoomNumber().equals(currentRoom.getRoomNumber()))
                        .findFirst()
                        .orElse(null);

                if (savedRoom != null) {
                    currentRoom.setIdRoom(savedRoom.getIdRoom());
                }
            } else {
                roomDAO.updateRoom(currentRoom);
            }

            List<Appliances> oldApps = roomDAO.getAppliancesForRoom(currentRoom.getIdRoom());
            for (Appliances oldApp : oldApps) {
                roomDAO.removeApplianceFromRoom(currentRoom.getIdRoom(), oldApp.getIdAppliance());
            }

            for (Appliances app : localAppliancesList) {
                roomDAO.addApplianceToRoom(currentRoom.getIdRoom(), app.getIdAppliance());
            }

            alerts.showMessage("Успіх", "Дані номера та комплектації техніки успішно збережено!");
            closeStage();

        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка БД", "Не вдалося зберегти параметри кімнати:\n" + e.getMessage());
        }
    }

    /**
     * Скасування редагування та закриття вікна.
     */
    @FXML
    private void handleCancel() {
        closeStage();
    }

    /**
     * Додавання нового приладу до комплектації кімнати.
     */
    @FXML
    private void handleAddAppliance() {
        try {
            List<Appliances> allAvailable = roomDAO.getAllAppliance();
            if (allAvailable.isEmpty()) {
                alerts.showError("Помилка", "Список доступного в готелі обладнання порожній.");
                return;
            }

            ChoiceDialog<Appliances> dialog = new ChoiceDialog<>(allAvailable.get(0), allAvailable);
            dialog.setTitle("Додати обладнання");
            dialog.setHeaderText("Виберіть прилад для додавання у кімнату");
            dialog.setContentText("Доступні прилади:");

            Optional<Appliances> result = dialog.showAndWait();
            result.ifPresent(appliance -> {
                boolean alreadyAdded = localAppliancesList.stream()
                        .anyMatch(a -> a.getIdAppliance() == appliance.getIdAppliance());

                if (alreadyAdded) {
                    alerts.showError("Увага", "Цей прилад вже додано до комплектації номера!");
                } else {
                    localAppliancesList.add(appliance);
                }
            });
        } catch (Exception e) {
            alerts.showError("Системна помилка", "Помилка роботи з діалогом технік.");
        }
    }

    /**
     * Видалення вибраного приладу зі списку кімнати.
     */
    @FXML
    private void handleRemoveAppliance() {
        Appliances selected = appliancesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alerts.showError("Вибір відсутній", "Будь ласка, виберіть прилад зі списку кімнати.");
            return;
        }

        localAppliancesList.remove(selected);
    }

    /**
     * Перевірка коректності введених даних форми.
     */
    private boolean isInputValid() {
        if (txtRoomNumber.getText() == null || txtRoomNumber.getText().trim().isEmpty() ||
                txtCost.getText() == null || txtCost.getText().trim().isEmpty() ||
                comboClass.getValue() == null ||
                txtCapacity.getText() == null || txtCapacity.getText().trim().isEmpty()) {

            alerts.showError("Помилка заповнення", "Поля 'Номер', 'Вартість', 'Місткість' та 'Клас' є обов'язковими!");
            return false;
        }
        return true;
    }

    /**
     * Закриття поточного модального вікна.
     */
    private void closeStage() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            stage.close();
        }
    }
}