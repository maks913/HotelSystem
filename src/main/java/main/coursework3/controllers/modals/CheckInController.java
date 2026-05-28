package main.coursework3.controllers.modals;

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
import javafx.util.StringConverter;
import main.coursework3.io.Alerts;
import main.coursework3.model.Settlements;
import main.coursework3.model.Clients;
import main.coursework3.model.Rooms;
import main.coursework3.dao.RoomDAO;
import main.coursework3.services.SettlementService;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class CheckInController implements Initializable {
    @FXML private ComboBox<Clients> cmbClient;
    @FXML private ComboBox<Rooms> cmbRoom;
    @FXML private DatePicker dpArrivalDate;
    @FXML private DatePicker dpDateLeaving;
    @FXML private ComboBox<String> cmbPaymentStatus;
    @FXML private Button btnCancel;
    @FXML private Button btnAddNewClient;
    @FXML private Button btnSave;

    private int currentSettlementId = 0;
    private final SettlementService settlementService = new SettlementService();
    private final Alerts alerts = new Alerts();

    private Rooms initialRoomRef = null;
    /** Ініціалізація контролера та налаштування форми поселення. */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupUI();
        setupListeners();
        setupActions();

        refreshClientList(false);
        updateAvailableRoomsList();
    }
    /** Заповнення форми даними існуючого поселення. */
    public void setInitialData(int idSettlement, int idClient, Rooms initialRoom, LocalDate arrivalDate, LocalDate departureDate, String paymentStatus) {
        this.currentSettlementId = idSettlement;
        this.initialRoomRef = initialRoom;

        if (arrivalDate != null) {
            dpArrivalDate.setValue(arrivalDate);
        }
        if (departureDate != null) {
            dpDateLeaving.setValue(departureDate);
        }
        if (paymentStatus != null && cmbPaymentStatus != null) {
            cmbPaymentStatus.setValue(paymentStatus);
        }

        updateAvailableRoomsList();

        if (initialRoom != null) {
            boolean alreadyExists = cmbRoom.getItems().stream()
                    .anyMatch(r -> r.getIdRoom() == initialRoom.getIdRoom());
            if (!alreadyExists) {
                cmbRoom.getItems().add(initialRoom);
            }
            cmbRoom.setValue(initialRoom);
        }

        if (idClient > 0) {
            for (Clients client : cmbClient.getItems()) {
                if (client.getIdClient() == idClient) {
                    cmbClient.setValue(client);
                    break;
                }
            }
        }
    }
    /** Налаштування початкових параметрів інтерфейсу. */
    private void setupUI() {
        cmbPaymentStatus.setItems(FXCollections.observableArrayList("Не оплачено", "Оплачено"));
        cmbPaymentStatus.setValue("Не оплачено");

        dpArrivalDate.setValue(LocalDate.now());
        dpDateLeaving.setValue(LocalDate.now().plusDays(1));

        setupConverters();
    }
    /** Налаштування слухачів зміни дат поселення. */
    private void setupListeners() {
        dpArrivalDate.valueProperty().addListener((obs, oldDate, newDate) -> updateAvailableRoomsList());
        dpDateLeaving.valueProperty().addListener((obs, oldDate, newDate) -> updateAvailableRoomsList());
    }
    /** Налаштування обробників кнопок форми. */
    private void setupActions() {
        btnCancel.setOnAction(event -> closeWindow(btnCancel));
        btnSave.setOnAction(event -> handleSave());
        btnAddNewClient.setOnAction(event -> openNewClientModal());
    }
    /** Налаштування відображення клієнтів та номерів у комбобоксах. */
    private void setupConverters() {
        cmbClient.setConverter(new StringConverter<>() {
            @Override
            public String toString(Clients client) {
                return client == null ? "" : client.getPib() + " (Паспорт: " + client.getPasportNumber() + ")";
            }
            @Override
            public Clients fromString(String string) { return null; }
        });

        cmbRoom.setConverter(new StringConverter<>() {
            @Override
            public String toString(Rooms room) {
                return room == null ? "" : "№" + room.getRoomNumber() + " - " + room.getRoomClass();
            }
            @Override
            public Rooms fromString(String string) { return null; }
        });
    }
    /** Оновлення списку доступних номерів на вибрані дати. */
    private void updateAvailableRoomsList() {
        LocalDate arrival = dpArrivalDate.getValue();
        LocalDate departure = dpDateLeaving.getValue();

        if (arrival == null || departure == null || departure.isBefore(arrival)) {
            cmbRoom.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            List<Rooms> freeRooms = settlementService.findAvailableRoomsByDates(arrival, departure);
            ObservableList<Rooms> roomOptions = FXCollections.observableArrayList(freeRooms);

            if (this.initialRoomRef != null) {
                boolean containsInitial = roomOptions.stream()
                        .anyMatch(r -> r.getIdRoom() == this.initialRoomRef.getIdRoom());
                if (!containsInitial) {
                    roomOptions.add(this.initialRoomRef);
                }
            }

            Rooms previouslySelected = cmbRoom.getValue();
            cmbRoom.setItems(roomOptions);

            if (previouslySelected != null) {
                roomOptions.stream()
                        .filter(r -> r.getIdRoom() == previouslySelected.getIdRoom())
                        .findFirst()
                        .ifPresent(cmbRoom::setValue);
            } else if (this.initialRoomRef != null) {
                roomOptions.stream()
                        .filter(r -> r.getIdRoom() == this.initialRoomRef.getIdRoom())
                        .findFirst()
                        .ifPresent(cmbRoom::setValue);
            }
        } catch (Exception e) {
            System.err.println("Помилка оновлення списку вільних кімнат: " + e.getMessage());
        }
    }
    /** Перевірка та збереження даних поселення. */
    private void handleSave() {
        Clients selectedClient = cmbClient.getValue();
        Rooms selectedRoom = cmbRoom.getValue();
        LocalDate arrivalDate = dpArrivalDate.getValue();
        LocalDate dateLeaving = dpDateLeaving.getValue();
        String paymentStatus = cmbPaymentStatus.getValue();

        if (arrivalDate == null) arrivalDate = LocalDate.now();
        if (arrivalDate.isAfter(LocalDate.now())) {
            alerts.showError("Помилка", "Пряме поселення можливе лише на сьогодні!");
            return;
        }


        if (arrivalDate == null || dateLeaving == null) {
            alerts.showError("Помилка заповнення", "Будь ласка, вкажіть точні дати заїзду та виїзду!");
            return;
        }
        if (selectedClient == null || selectedRoom == null || paymentStatus == null) {
            alerts.showError("Помилка заповнення", "Будь ласка, заповніть усі обов'язкові поля форми!");
            return;
        }

        if (!dateLeaving.isAfter(arrivalDate)) {
            alerts.showError("Помилка дат", "Дата виїзду має бути хоча б на 1 день пізніше дати заїзду!");
            return;
        }

        try {
            List<Rooms> freeRoomsOnTheseDates = settlementService.findAvailableRoomsByDates(arrivalDate, dateLeaving);
            boolean isRoomFree = freeRoomsOnTheseDates.stream()
                    .anyMatch(r -> r.getIdRoom() == selectedRoom.getIdRoom());

            if (currentSettlementId > 0 && this.initialRoomRef != null) {
                boolean isSameRoom = (selectedRoom.getIdRoom() == initialRoomRef.getIdRoom());
                if (!isRoomFree && !isSameRoom) {
                    alerts.showError("Номер зайнято", "На вибрані дати цей номер уже зайнятий іншим гостем!");
                    return;
                }
            } else {
                if (!isRoomFree) {
                    alerts.showError("Номер зайнято", "Неможливо оформити поселення. Цей номер зайнятий на вказані дати!");
                    return;
                }
            }

            int days = settlementService.calculateNights(arrivalDate, dateLeaving);
            double totalCost = days * selectedRoom.getCostPerDay();

            Settlements settlement = new Settlements(
                    currentSettlementId,
                    selectedClient.getIdClient(),
                    selectedRoom.getIdRoom(),
                    Date.valueOf(arrivalDate),
                    Date.valueOf(dateLeaving),
                    totalCost,
                    paymentStatus
            );

            boolean success;
            if (currentSettlementId == 0) {
                success = settlementService.insertSettlement(settlement);

                if (success) {
                    settlementService.updateRoomStatusById(selectedRoom.getIdRoom(), RoomDAO.STATUS_OCCUPIED);
                }
            } else {
                success = settlementService.updateSettlementFull(settlement);
            }

            if (success) {
                settlementService.runRoomAutoUpdate();

                alerts.showMessage("Успіх", "Дані проживання успішно збережено!");
                closeWindow(btnSave);
            } else {
                alerts.showError("Помилка бази даних", "СУБД відхилила запис проживання.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Системна помилка", "Помилка при обробці транзакції заселення:\n" + e.getMessage());
        }
    }
    /** Оновлення списку клієнтів у комбобоксі. */
    private void refreshClientList(boolean selectNewest) {
        try {
            List<Clients> clients = settlementService.getAllClients();
            cmbClient.setItems(FXCollections.observableArrayList(clients));

            if (selectNewest && !clients.isEmpty()) {
                Clients newestClient = clients.stream()
                        .max(Comparator.comparingInt(Clients::getIdClient))
                        .orElse(null);
                cmbClient.getSelectionModel().select(newestClient);
            }
        } catch (Exception e) {
            System.err.println("Не вдалося оновити список клієнтів: " + e.getMessage());
        }
    }
    /** Відкриття модального вікна для додавання нового клієнта. */
    private void openNewClientModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/client_details_modal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Додавання нового клієнта");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cmbClient.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            refreshClientList(true);

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка завантаження вікна", "Не вдалося відкрити форму швидкої реєстрації клієнта.");
        }
    }
    /** Закриття поточного модального вікна. */
    private void closeWindow(Button sourceButton) {
        if (sourceButton != null && sourceButton.getScene() != null) {
            Stage stage = (Stage) sourceButton.getScene().getWindow();
            stage.close();
        }
    }
}